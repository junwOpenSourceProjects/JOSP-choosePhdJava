package com.choosephd.importer;

import com.choosephd.config.ChoosePhdProperties;
import com.choosephd.entity.RankingSource;
import com.choosephd.entity.Subject;
import com.choosephd.repository.RankingSourceMapper;
import com.choosephd.repository.SubjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private final ChoosePhdProperties properties;
    private final RankingSourceMapper rankingSourceMapper;
    private final SubjectMapper subjectMapper;
    private final ImportBatchService importBatchService;
    private final StringRedisTemplate redisTemplate;

    private final ImportProgress progress = new ImportProgress();
    private final Map<String, RankingSource> sourceCache = new ConcurrentHashMap<>();
    private final Map<String, Subject> subjectCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int BATCH_SIZE = 500;
    private static final String REDIS_KEY = "choosephd:import:progress";

    public DataImportService(ChoosePhdProperties properties,
                             RankingSourceMapper rankingSourceMapper,
                             SubjectMapper subjectMapper,
                             ImportBatchService importBatchService,
                             StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.rankingSourceMapper = rankingSourceMapper;
        this.subjectMapper = subjectMapper;
        this.importBatchService = importBatchService;
        this.redisTemplate = redisTemplate;
    }

    public ImportProgress getProgress() {
        if ("IDLE".equals(progress.getStatus())) {
            ImportProgress fromRedis = readRedisProgress();
            if (fromRedis != null) {
                return fromRedis;
            }
        }
        return progress;
    }

    public void runImport() {
        if ("RUNNING".equals(progress.getStatus())) {
            throw new IllegalStateException("Import already running");
        }

        resetProgress();
        progress.setStatus("RUNNING");
        progress.setStartedAt(Instant.now());

        try {
            Path rawDir = Paths.get(properties.getData().getRawDir());
            List<RawDataScanner.RankingFile> files = RawDataScanner.scan(rawDir);
            progress.getTotalFiles().set(files.size());

            for (RawDataScanner.RankingFile file : files) {
                processFile(file);
                progress.getProcessedFiles().incrementAndGet();
                writeRedisProgress();
            }

            progress.setStatus("SUCCESS");
            progress.setMessage("Imported " + progress.getSuccessRecords().get() + " records");
        } catch (Exception e) {
            log.error("Import failed", e);
            progress.setStatus("FAILED");
            progress.setMessage(e.getMessage());
        } finally {
            progress.setFinishedAt(Instant.now());
            writeRedisProgress();
        }
    }

    private void resetProgress() {
        progress.setStatus("IDLE");
        progress.setStartedAt(null);
        progress.setFinishedAt(null);
        progress.getTotalFiles().set(0);
        progress.getProcessedFiles().set(0);
        progress.getTotalRecords().set(0);
        progress.getSuccessRecords().set(0);
        progress.getFailedRecords().set(0);
        progress.setCurrentFile(null);
        progress.setMessage(null);
        sourceCache.clear();
        subjectCache.clear();
    }

    private void processFile(RawDataScanner.RankingFile file) throws IOException {
        SourceResolver.SourceContext ctx = file.context();
        progress.setCurrentFile(file.file().toString());

        if (ctx.getYear() == null || ctx.getYear() < 1900 || ctx.getYear() > 2100) {
            log.warn("Skip invalid year file: {}", file.file());
            return;
        }

        RankingSource source = getOrCreateSource(ctx);
        Subject subject = ctx.getSubjectSlug() != null ? getOrCreateSubject(ctx) : null;

        List<RawRankingRecord> records = JsonRankingParser.parse(file.file());
        progress.getTotalRecords().addAndGet(records.size());

        List<RawRankingRecord> batch = new ArrayList<>(BATCH_SIZE);
        int successInFile = 0;

        for (RawRankingRecord record : records) {
            batch.add(record);
            if (batch.size() >= BATCH_SIZE) {
                try {
                    importBatchService.saveBatch(batch, source, subject, ctx.getYear());
                    successInFile += batch.size();
                } catch (Exception e) {
                    log.warn("Batch save failed for {}: {}", file.file(), e.getMessage());
                    progress.getFailedRecords().addAndGet(batch.size());
                }
                batch.clear();
                writeRedisProgress();
            }
        }

        if (!batch.isEmpty()) {
            try {
                importBatchService.saveBatch(batch, source, subject, ctx.getYear());
                successInFile += batch.size();
            } catch (Exception e) {
                log.warn("Final batch save failed for {}: {}", file.file(), e.getMessage());
                progress.getFailedRecords().addAndGet(batch.size());
            }
        }

        progress.getSuccessRecords().addAndGet(successInFile);
    }

    private RankingSource getOrCreateSource(SourceResolver.SourceContext ctx) {
        return sourceCache.computeIfAbsent(ctx.getSourceSlug(), slug -> {
            RankingSource source = rankingSourceMapper.selectBySlug(slug);
            if (source != null) {
                return source;
            }

            RankingSource newSource = new RankingSource();
            newSource.setSlug(slug);
            newSource.setNameZh(formatSourceName(slug));
            newSource.setKind(ctx.getKind());
            newSource.setOwnerOrg(ctx.getOwnerOrg());
            newSource.setActive(1);
            rankingSourceMapper.insert(newSource);
            return newSource;
        });
    }

    private Subject getOrCreateSubject(SourceResolver.SourceContext ctx) {
        String key = ctx.getSourceSlug() + ":" + ctx.getSubjectSlug();
        return subjectCache.computeIfAbsent(key, k -> {
            Subject subject = subjectMapper.selectBySlug(ctx.getSubjectSlug());
            if (subject != null) {
                return subject;
            }

            Subject newSubject = new Subject();
            newSubject.setSlug(ctx.getSubjectSlug());
            newSubject.setNameZh(formatSubjectName(ctx.getSubjectSlug()));
            newSubject.setNameEn(ctx.getSubjectSlug().replace("-", " "));
            newSubject.setOwnerOrg(ctx.getOwnerOrg());
            newSubject.setActive(1);
            subjectMapper.insert(newSubject);
            return newSubject;
        });
    }

    private String formatSourceName(String slug) {
        return slug.replace("-", " ").toUpperCase();
    }

    private String formatSubjectName(String slug) {
        return slug.replace("-", " ");
    }

    private ImportProgress readRedisProgress() {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_KEY);
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            Map<String, Object> map = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            ImportProgress p = new ImportProgress();
            p.setStatus(StringUtils.defaultString((String) map.get("status"), "IDLE"));
            if ("IDLE".equals(p.getStatus())) {
                return null;
            }
            p.setStartedAt(parseInstant(map.get("startedAt")));
            p.setFinishedAt(parseInstant(map.get("finishedAt")));
            p.getTotalFiles().set(toInt(map.get("totalFiles")));
            p.getProcessedFiles().set(toInt(map.get("processedFiles")));
            p.getTotalRecords().set(toLong(map.get("totalRecords")));
            p.getSuccessRecords().set(toLong(map.get("successRecords")));
            p.getFailedRecords().set(toLong(map.get("failedRecords")));
            p.setCurrentFile(StringUtils.defaultString((String) map.get("currentFile"), null));
            p.setMessage(StringUtils.defaultString((String) map.get("message"), null));
            return p;
        } catch (Exception e) {
            log.warn("Failed to read redis progress", e);
            return null;
        }
    }

    private Instant parseInstant(Object value) {
        if (value == null) return null;
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private void writeRedisProgress() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("status", progress.getStatus());
            map.put("totalFiles", progress.getTotalFiles().get());
            map.put("processedFiles", progress.getProcessedFiles().get());
            map.put("totalRecords", progress.getTotalRecords().get());
            map.put("successRecords", progress.getSuccessRecords().get());
            map.put("failedRecords", progress.getFailedRecords().get());
            map.put("currentFile", StringUtils.defaultString(progress.getCurrentFile(), ""));
            map.put("message", StringUtils.defaultString(progress.getMessage(), ""));
            redisTemplate.opsForValue().set(REDIS_KEY, objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("Failed to write redis progress", e);
        }
    }
}
