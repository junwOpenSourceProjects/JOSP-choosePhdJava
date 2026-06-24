package com.choosephd.importer;

import com.choosephd.entity.RankingEntry;
import com.choosephd.entity.RankingSource;
import com.choosephd.entity.Subject;
import com.choosephd.entity.University;
import com.choosephd.repository.RankingEntryMapper;
import com.choosephd.repository.UniversityMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImportBatchService {

    private final UniversityMapper universityMapper;
    private final RankingEntryMapper rankingEntryMapper;

    public ImportBatchService(UniversityMapper universityMapper, RankingEntryMapper rankingEntryMapper) {
        this.universityMapper = universityMapper;
        this.rankingEntryMapper = rankingEntryMapper;
    }

    @Transactional
    public void saveBatch(List<RawRankingRecord> records, RankingSource source, Subject subject, int year) {
        for (RawRankingRecord record : records) {
            saveRecord(record, source, subject, year);
        }
    }

    private void saveRecord(RawRankingRecord record, RankingSource source, Subject subject, int year) {
        University university = upsertUniversity(record);

        Integer sourceId = source.getId();
        Integer subjectId = subject != null ? subject.getId() : null;

        rankingEntryMapper.deleteByNaturalKey(university.getUrlId(), sourceId, subjectId, year);

        RankingEntry entry = new RankingEntry();
        entry.setUniversityId(university.getUrlId());
        entry.setSourceId(sourceId);
        entry.setSubjectId(subjectId);
        entry.setYear(year);
        entry.setRankDisplay(record.getRankDisplay());
        entry.setRankValue(record.getRankValue());
        entry.setRankDelta(record.getRankDelta());
        entry.setDirection(record.getDirection());
        rankingEntryMapper.insert(entry);
    }

    private University upsertUniversity(RawRankingRecord record) {
        String urlId = record.getUrlId();
        University existing = universityMapper.selectById(urlId);

        if (existing != null) {
            boolean needUpdate = false;
            if (StringUtils.isEmpty(existing.getCountry()) && StringUtils.isNotEmpty(record.getCountry())) {
                existing.setCountry(record.getCountry());
                needUpdate = true;
            }
            if (StringUtils.isEmpty(existing.getRegion()) && StringUtils.isNotEmpty(record.getRegion())) {
                existing.setRegion(record.getRegion());
                needUpdate = true;
            }
            if (StringUtils.isEmpty(existing.getNameZhTw()) && StringUtils.isNotEmpty(record.getNameZhTw())) {
                existing.setNameZhTw(record.getNameZhTw());
                needUpdate = true;
            }
            if (needUpdate) {
                universityMapper.updateById(existing);
            }
            return existing;
        }

        University university = new University();
        university.setUrlId(urlId);
        university.setNameZh(StringUtils.defaultString(record.getNameZh(), record.getNameEn()));
        university.setNameEn(StringUtils.defaultString(record.getNameEn(), record.getNameZh()));
        university.setNameZhTw(record.getNameZhTw());
        university.setCountry(StringUtils.defaultString(record.getCountry(), "unknown"));
        university.setRegion(StringUtils.defaultString(record.getRegion(), "unknown"));
        university.setBadgeUrl(record.getBadgeUrl());
        universityMapper.insert(university);
        return university;
    }
}
