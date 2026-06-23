package josp.choosphd.importer;

import josp.choosphd.domain.auth.ImportJobPO;
import josp.choosphd.domain.ranking.RankingEntryPO;
import josp.choosphd.domain.trend.RankingTrendPO;
import josp.choosphd.domain.university.UniversityPO;
import josp.choosphd.mapper.DictMapper;
import josp.choosphd.mapper.ImportJobMapper;
import josp.choosphd.mapper.RankingEntryMapper;
import josp.choosphd.mapper.RankingTrendMapper;
import josp.choosphd.mapper.UniversityMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private static final Pattern RANK_HTML = Pattern.compile(".*?(?:<i[^>]*>)?\\s*(\\d+)\\s*(?:</i>)?.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_CLEAN = Pattern.compile("[^0-9.\\-]");

    private final UniversityMapper universityMapper;
    private final RankingEntryMapper entryMapper;
    private final RankingTrendMapper trendMapper;
    private final ImportJobMapper jobMapper;
    private final DictMapper dictMapper;

    public DataImportService(UniversityMapper universityMapper,
                             RankingEntryMapper entryMapper,
                             RankingTrendMapper trendMapper,
                             ImportJobMapper jobMapper,
                             DictMapper dictMapper) {
        this.universityMapper = universityMapper;
        this.entryMapper = entryMapper;
        this.trendMapper = trendMapper;
        this.jobMapper = jobMapper;
        this.dictMapper = dictMapper;
    }

    @Transactional
    public ImportJobPO importOne(RawDataScanner.RawFile rf) {
        Integer sourceId = dictMapper.findSourceIdByCode(rf.source().toUpperCase(Locale.ROOT));
        if (sourceId == null) {
            log.warn("unknown source code: {}", rf.source());
            return null;
        }

        ImportJobPO job = new ImportJobPO();
        job.setJobKey(rf.source().toUpperCase(Locale.ROOT) + ":" + rf.year() + ":" +
                (rf.trendDirection() == null ? "entry" : "trend") + ":" + rf.fileName());
        job.setSourceId(sourceId);
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setInsertedRows(0);
        job.setUpdatedRows(0);
        job.setSkippedRows(0);
        job.setCreatedAt(LocalDateTime.now());

        // 幂等:已存在则更新(让 job_key 唯一但可重入)
        Long existingId = jobMapper.findIdByJobKey(job.getJobKey());
        if (existingId != null) {
            job.setId(existingId);
            jobMapper.update(job);
        } else {
            jobMapper.insert(job);
        }

        try {
            int read = 0, inserted = 0, updated = 0, skipped = 0;
            try (Reader r = Files.newBufferedReader(Path.of(rf.path()), StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setIgnoreEmptyLines(true)
                         .setTrim(true)
                         .build()
                         .parse(r)) {

                for (CSVRecord row : parser) {
                    read++;
                    try {
                        if (rf.trendDirection() == null) {
                            int[] r2 = importEntryRow(sourceId, rf, row);
                            inserted += r2[0]; updated += r2[1]; skipped += r2[2];
                        } else {
                            int[] r2 = importTrendRow(sourceId, rf, row);
                            inserted += r2[0]; updated += r2[1]; skipped += r2[2];
                        }
                    } catch (Exception ex) {
                        skipped++;
                        log.warn("row skipped in {}: {}", rf.fileName(), ex.getMessage());
                    }
                }
            }

            job.setStatus("SUCCESS");
            job.setTotalRows(read);
            job.setProcessedRows(read);
            job.setInsertedRows(inserted);
            job.setUpdatedRows(updated);
            job.setSkippedRows(skipped);
            job.setFinishedAt(LocalDateTime.now());
            jobMapper.update(job);
            return job;
        } catch (Exception e) {
            log.error("import failed: {}", rf.path(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            jobMapper.update(job);
            return job;
        }
    }

    private int[] importEntryRow(Integer sourceId, RawDataScanner.RawFile rf, CSVRecord row) {
        String urlId = pickField(row, "url_id", "url", "id", "slug", "university_id");
        String name = pickField(row, "name", "university", "eng_name", "name_en", "institution", "school");
        if (urlId == null || name == null) return new int[]{0, 0, 1};
        urlId = urlId.trim().toLowerCase();
        name = name.trim();

        String cnName = pickField(row, "cn_name", "chinese_name", "cn", "name_zh", "name_cn");
        String country = pickField(row, "country", "country_code", "location", "nation");
        String regionCode = pickField(row, "region", "region_code");
        String logo = pickField(row, "logo", "logo_url", "image");
        String subjectCode = pickField(row, "subject", "subject_code", "discipline");

        Integer rank = parseIntOrNull(pickField(row, "rank", "ranking", "world_rank", "position"));
        Double scoreD = parseDoubleOrNull(pickField(row, "score", "total_score", "overall_score"));
        BigDecimal score = scoreD == null ? null : BigDecimal.valueOf(scoreD);

        Integer regionId = (regionCode == null || regionCode.isBlank()) ? null :
                dictMapper.findRegionIdByCode(regionCode);
        Integer subjectId = (subjectCode == null || subjectCode.isBlank()) ? null :
                dictMapper.findSubjectIdByCode(subjectCode);

        LocalDateTime now = LocalDateTime.now();
        Long uniId = universityMapper.findIdByUrlId(urlId);
        if (uniId == null) {
            UniversityPO u = new UniversityPO();
            u.setUrlId(urlId);
            u.setNameEn(name);
            u.setNameZh(cnName);
            u.setLogoUrl(logo);
            u.setRegionId(regionId);
            u.setCountryCode(country);
            u.setDeleted(0);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            universityMapper.insert(u);
            uniId = u.getId();
        } else {
            UniversityPO u = new UniversityPO();
            u.setId(uniId);
            u.setNameEn(name);
            u.setNameZh(cnName);
            u.setLogoUrl(logo);
            u.setRegionId(regionId);
            u.setCountryCode(country);
            u.setUpdatedAt(now);
            universityMapper.update(u);
        }

        // 幂等:删同 (university_id, source_id, year) 再插
        entryMapper.deleteByNaturalKey(uniId, sourceId, (short) rf.year(), subjectId);
        RankingEntryPO e = new RankingEntryPO();
        e.setUniversityId(uniId);
        e.setSourceId(sourceId);
        e.setSubjectId(subjectId);
        e.setYear((short) rf.year());
        e.setRankDisplay(rank);
        e.setScore(score);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        entryMapper.insert(e);
        return new int[]{1, 0, 0};
    }

    private int[] importTrendRow(Integer sourceId, RawDataScanner.RawFile rf, CSVRecord row) {
        String urlId = pickField(row, "url_id", "url", "id", "slug");
        String name = pickField(row, "name", "university", "eng_name", "name_en");
        if (urlId == null && name == null) return new int[]{0, 0, 1};

        Long uniId;
        if (urlId != null) {
            uniId = universityMapper.findIdByUrlId(urlId.trim().toLowerCase());
        } else {
            String foundUrl = universityMapper.findUrlIdByName(name.trim());
            uniId = foundUrl == null ? null : universityMapper.findIdByUrlId(foundUrl);
        }
        if (uniId == null) return new int[]{0, 0, 1};

        Integer rank = parseRankHtmlOrNull(pickField(row, "rank", "ranking", "position", "rank_to", "to_rank"));
        Integer rankPrev = parseRankHtmlOrNull(pickField(row, "rank_prev", "previous_rank", "prev", "rank_from", "from_rank"));
        Integer delta = parseIntOrNull(pickField(row, "delta", "change", "rank_change"));
        if (delta == null && rank != null && rankPrev != null) {
            delta = rankPrev - rank;  // 正=上升
        }

        // trend_type: UP -> GROWING, DOWN -> DECLINING
        String trendType = "STABLE";
        if (rf.trendDirection() != null) {
            trendType = "UP".equalsIgnoreCase(rf.trendDirection()) ? "GROWING" : "DECLINING";
        }

        // base_year: file year - 1 (默认相对上一年)
        short baseYear = (short) (rf.year() - 1);
        short targetYear = (short) rf.year();

        trendMapper.deleteByNaturalKey(uniId, sourceId, baseYear, targetYear, trendType);
        RankingTrendPO t = new RankingTrendPO();
        t.setUniversityId(uniId);
        t.setSourceId(sourceId);
        t.setTrendType(trendType);
        t.setBaseYear(baseYear);
        t.setTargetYear(targetYear);
        t.setRankChange(delta);
        t.setRankFrom(rankPrev);
        t.setRankTo(rank);
        t.setCreatedAt(LocalDateTime.now());
        trendMapper.insert(t);
        return new int[]{1, 0, 0};
    }

    private static String pickField(CSVRecord row, String... names) {
        for (String n : names) {
            if (row.isMapped(n)) {
                String v = row.get(n);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try {
            return (int) Math.round(Double.parseDouble(s.replaceAll("[^0-9.\\-]", "")));
        } catch (Exception e) { return null; }
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s.replaceAll("[^0-9.\\-]", "")); }
        catch (Exception e) { return null; }
    }

    private static Integer parseRankHtmlOrNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return null;
        try { return Integer.parseInt(trimmed); } catch (Exception ignore) {}
        Matcher m = RANK_HTML.matcher(trimmed);
        if (m.matches()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
        }
        String cleaned = NUMBER_CLEAN.matcher(trimmed).replaceAll(" ").trim();
        String[] parts = cleaned.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            try { return Integer.parseInt(parts[i]); } catch (Exception ignore) {}
        }
        return null;
    }
}
