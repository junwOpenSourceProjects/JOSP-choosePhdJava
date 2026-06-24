package com.choosephd.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RankParser {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(\\d+)\\s*-\s*(\\d+)$");

    public static class RankResult {
        private String display;
        private Integer value;
        private Integer delta;
        private Integer direction;

        public String getDisplay() {
            return display;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Integer getDelta() {
            return delta;
        }

        public void setDelta(Integer delta) {
            this.delta = delta;
        }

        public Integer getDirection() {
            return direction;
        }

        public void setDirection(Integer direction) {
            this.direction = direction;
        }
    }

    public static RankResult parse(String rank, String rankAlias) {
        RankResult result = new RankResult();

        String cleanRank = stripHtml(StringUtils.trimToEmpty(rank));
        String cleanAlias = stripHtml(StringUtils.trimToEmpty(rankAlias));

        result.setDisplay(StringUtils.isEmpty(cleanRank) ? "-" : cleanRank);
        result.setValue(parseValue(cleanRank, cleanAlias));
        result.setDelta(parseDelta(cleanAlias));
        result.setDirection(parseDirection(cleanAlias));

        return result;
    }

    private static String stripHtml(String input) {
        if (StringUtils.isEmpty(input)) {
            return "";
        }
        String cleaned = HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
        cleaned = cleaned.replaceAll("&nbsp;", " ").trim();
        return cleaned;
    }

    private static Integer parseValue(String rank, String alias) {
        String text = StringUtils.isEmpty(rank) ? alias : rank;
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        text = stripHtml(text).replace("#", "").replace("=", "").trim();

        if (text.equalsIgnoreCase("Reporter")) {
            return 9999;
        }

        if (text.equals("-") || text.isEmpty()) {
            return null;
        }

        Matcher rangeMatcher = RANGE_PATTERN.matcher(text);
        if (rangeMatcher.matches()) {
            int start = Integer.parseInt(rangeMatcher.group(1));
            int end = Integer.parseInt(rangeMatcher.group(2));
            return (start + end) / 2;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseDelta(String alias) {
        if (StringUtils.isEmpty(alias)) {
            return null;
        }
        String text = alias.replace("+", "").replace("-", "").trim();
        try {
            int value = Integer.parseInt(text);
            return alias.contains("-") ? -value : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseDirection(String alias) {
        if (StringUtils.isEmpty(alias)) {
            return null;
        }
        if (alias.contains("+")) {
            return 1;
        }
        if (alias.contains("-")) {
            return -1;
        }
        return 0;
    }
}
