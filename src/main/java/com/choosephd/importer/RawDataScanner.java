package com.choosephd.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class RawDataScanner {

    private static final Logger log = LoggerFactory.getLogger(RawDataScanner.class);

    public static List<RankingFile> scan(Path rawDir) throws IOException {
        if (!Files.exists(rawDir) || !Files.isDirectory(rawDir)) {
            throw new IOException("Raw data directory does not exist: " + rawDir);
        }

        List<Path> topLevelDirs = new ArrayList<>();
        try (Stream<Path> stream = Files.list(rawDir)) {
            stream.filter(Files::isDirectory).forEach(topLevelDirs::add);
        }

        Set<String> topLevelFlatSubjectDirs = new HashSet<>();
        for (Path dir : topLevelDirs) {
            String dirName = dir.getFileName().toString();
            if (SourceResolver.isFlatSubjectDir(dirName)) {
                topLevelFlatSubjectDirs.add(dirName);
            }
        }

        // key = sourceSlug/year.json, keeps first encountered file (top-level wins over nested)
        Map<String, RankingFile> seen = new LinkedHashMap<>();

        // Pass 1: top-level non-parent dirs (flat subject dirs and all other rankings)
        for (Path dir : topLevelDirs) {
            String dirName = dir.getFileName().toString();
            if (SourceResolver.isSubjectParent(dirName)) {
                continue;
            }
            String subjectSlug = SourceResolver.isFlatSubjectDir(dirName)
                    ? SourceResolver.extractFlatSubjectSlug(dirName)
                    : null;
            processYearFiles(dir, rawDir, dirName, subjectSlug, seen);
        }

        // Pass 2: subject parents, skip children already represented at top level
        for (Path dir : topLevelDirs) {
            String dirName = dir.getFileName().toString();
            if (SourceResolver.isSubjectParent(dirName)) {
                processSubjectParent(dir, rawDir, topLevelFlatSubjectDirs, seen);
            }
        }

        log.info("Scanned {} ranking files from {}", seen.size(), rawDir);
        return new ArrayList<>(seen.values());
    }

    private static void processSubjectParent(Path parentDir, Path root,
                                             Set<String> topLevelFlatSubjectDirs,
                                             Map<String, RankingFile> seen) {
        String parentName = parentDir.getFileName().toString();
        try (Stream<Path> stream = Files.list(parentDir)) {
            stream.filter(Files::isDirectory).forEach(child -> {
                String childName = child.getFileName().toString();
                if (childName.startsWith(parentName + "-")) {
                    // nested flat subject dir (common for THE/RUR)
                    if (topLevelFlatSubjectDirs.contains(childName)) {
                        return; // top-level copy takes precedence
                    }
                    String subjectSlug = SourceResolver.extractFlatSubjectSlug(childName);
                    processYearFiles(child, root, childName, subjectSlug, seen);
                } else {
                    // standard bare subject dir (common for QS/ARWU/USNews)
                    String sourceSlug = parentName + "-" + childName;
                    processYearFiles(child, root, sourceSlug, childName, seen);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to list subject parent dir: {}", parentDir, e);
        }
    }

    private static void processYearFiles(Path dir, Path root, String sourceSlug,
                                         String subjectSlug, Map<String, RankingFile> seen) {
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        String key = sourceSlug + "/" + p.getFileName().toString();
                        seen.putIfAbsent(key, new RankingFile(root, p, sourceSlug, subjectSlug));
                    });
        } catch (IOException e) {
            log.warn("Failed to list year files: {}", dir, e);
        }
    }

    public record RankingFile(Path root, Path file, String sourceSlug, String subjectSlug) {

        public SourceResolver.SourceContext context() {
            return SourceResolver.resolve(file, sourceSlug, subjectSlug);
        }
    }
}
