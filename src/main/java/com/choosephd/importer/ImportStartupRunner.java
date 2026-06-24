package com.choosephd.importer;

import com.choosephd.repository.RankingEntryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ImportStartupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportStartupRunner.class);

    private final DataImportService dataImportService;
    private final RankingEntryMapper rankingEntryMapper;

    public ImportStartupRunner(DataImportService dataImportService, RankingEntryMapper rankingEntryMapper) {
        this.dataImportService = dataImportService;
        this.rankingEntryMapper = rankingEntryMapper;
    }

    @Override
    public void run(String... args) {
        long count = rankingEntryMapper.selectCount(null);
        if (count > 0) {
            log.info("Ranking entries already exist: {}, skip startup import", count);
            return;
        }
        log.info("No ranking entries found, starting data import...");
        new Thread(() -> dataImportService.runImport()).start();
    }
}
