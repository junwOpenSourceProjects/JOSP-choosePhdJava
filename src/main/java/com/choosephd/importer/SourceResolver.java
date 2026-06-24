package com.choosephd.importer;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class SourceResolver {

    public static final Set<String> SUBJECT_PARENTS = Set.of(
            "qs-university-subject-rankings",
            "arwu-university-subject-rankings",
            "the-university-subject-rankings",
            "usnews-university-subject-rankings",
            "rur-university-subject-rankings"
    );

    public static class SourceContext {
        private String sourceSlug;
        private Integer kind;
        private String ownerOrg;
        private String subjectSlug;
        private Integer year;

        public String getSourceSlug() {
            return sourceSlug;
        }

        public void setSourceSlug(String sourceSlug) {
            this.sourceSlug = sourceSlug;
        }

        public Integer getKind() {
            return kind;
        }

        public void setKind(Integer kind) {
            this.kind = kind;
        }

        public String getOwnerOrg() {
            return ownerOrg;
        }

        public void setOwnerOrg(String ownerOrg) {
            this.ownerOrg = ownerOrg;
        }

        public String getSubjectSlug() {
            return subjectSlug;
        }

        public void setSubjectSlug(String subjectSlug) {
            this.subjectSlug = subjectSlug;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }
    }

    public static SourceContext resolve(Path file, String sourceSlug, String subjectSlug) {
        SourceContext ctx = new SourceContext();
        ctx.setSourceSlug(sourceSlug);
        ctx.setSubjectSlug(subjectSlug);
        ctx.setKind(detectKind(sourceSlug, subjectSlug));
        ctx.setOwnerOrg(extractOwnerOrg(sourceSlug));
        ctx.setYear(extractYear(file.getFileName().toString()));
        return ctx;
    }

    public static boolean isSubjectParent(String dirName) {
        return SUBJECT_PARENTS.contains(dirName);
    }

    public static boolean isFlatSubjectDir(String dirName) {
        for (String parent : SUBJECT_PARENTS) {
            if (dirName.startsWith(parent + "-")) {
                return true;
            }
        }
        return false;
    }

    public static String extractFlatSubjectSlug(String dirName) {
        for (String parent : SUBJECT_PARENTS) {
            if (dirName.startsWith(parent + "-")) {
                return dirName.substring(parent.length() + 1);
            }
        }
        return null;
    }

    private static Integer detectKind(String sourceSlug, String subjectSlug) {
        if (subjectSlug != null) {
            return 3;
        }
        String lower = sourceSlug.toLowerCase(Locale.ROOT);
        if (lower.startsWith("growth-trend-") || lower.startsWith("declining-trend-")) {
            return 4;
        }
        if (lower.contains("-asia-") || lower.contains("-europe-") || lower.contains("-north-america-")
                || lower.contains("-oceania-") || lower.contains("-africa-") || lower.contains("-latin-america-")
                || lower.contains("-japan-") || lower.contains("-national-")
                || lower.endsWith("-asia") || lower.endsWith("-europe")
                || lower.endsWith("-north-america") || lower.endsWith("-oceania")
                || lower.endsWith("-africa") || lower.endsWith("-latin-america")
                || lower.endsWith("-japan") || lower.endsWith("-national")) {
            return 2;
        }
        return 1;
    }

    private static String extractOwnerOrg(String sourceSlug) {
        String lower = sourceSlug.toLowerCase(Locale.ROOT);
        if (lower.startsWith("qs")) return "QS";
        if (lower.startsWith("the")) return "THE";
        if (lower.startsWith("arwu")) return "ARWU";
        if (lower.startsWith("usnews")) return "USN";
        if (lower.startsWith("cwur")) return "CWUR";
        if (lower.startsWith("rur")) return "RUR";
        if (lower.startsWith("edurank")) return "EduRank";
        if (lower.startsWith("mosiur")) return "Mosiur";
        if (lower.startsWith("menggy")) return "Menggy";
        if (lower.startsWith("growth-trend-") || lower.startsWith("declining-trend-")) {
            return extractOwnerOrg(sourceSlug.substring(sourceSlug.indexOf("-") + 1));
        }
        return "Other";
    }

    private static Integer extractYear(String fileName) {
        String base = StringUtils.substringBeforeLast(fileName, ".");
        try {
            return Integer.parseInt(base);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
