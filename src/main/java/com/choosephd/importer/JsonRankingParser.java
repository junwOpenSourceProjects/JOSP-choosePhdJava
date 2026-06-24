package com.choosephd.importer;

import com.choosephd.util.RankParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonRankingParser {

    private static final Logger log = LoggerFactory.getLogger(JsonRankingParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<RawRankingRecord> parse(Path jsonFile) throws IOException {
        List<RawRankingRecord> records = new ArrayList<>();

        JsonNode root = MAPPER.readTree(jsonFile.toFile());
        JsonNode entities = findEntities(root);

        if (entities == null || !entities.isArray()) {
            log.warn("No entities array found in {}", jsonFile);
            return records;
        }

        for (JsonNode node : entities) {
            RawRankingRecord record = toRecord(node);
            if (StringUtils.isNotEmpty(record.getUrlId())) {
                records.add(record);
            }
        }

        return records;
    }

    private static JsonNode findEntities(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (root.has("entities")) {
            return root.get("entities");
        }
        if (root.has("data") && root.get("data").has("entities")) {
            return root.get("data").get("entities");
        }
        Iterator<String> fields = root.fieldNames();
        while (fields.hasNext()) {
            JsonNode child = root.get(fields.next());
            if (child.isArray() && child.size() > 0 && child.get(0).has("url_id")) {
                return child;
            }
        }
        return null;
    }

    private static RawRankingRecord toRecord(JsonNode node) {
        RawRankingRecord record = new RawRankingRecord();

        record.setUrlId(normalizeUrlId(getText(node, "url_id", "urlId", "id")));
        record.setNameZh(getText(node, "name"));
        record.setNameEn(getText(node, "eng_name", "engName"));
        record.setNameZhTw(getText(node, "name_fanti", "nameFanti"));
        record.setBadgeUrl(getText(node, "badge"));

        JsonNode tags = node.get("tags");
        if (tags != null && tags.isArray()) {
            for (JsonNode tag : tags) {
                String tagType = detectTagType(tag);
                if ("country".equals(tagType)) {
                    record.setCountry(getText(tag, "name"));
                } else if ("region".equals(tagType)) {
                    record.setRegion(getText(tag, "name"));
                }
            }
        }

        String rank = getText(node, "rank");
        String rankAlias = getText(node, "rank_alias", "rankAlias");
        RankParser.RankResult parsed = RankParser.parse(rank, rankAlias);

        record.setRankDisplay(parsed.getDisplay());
        record.setRankValue(parsed.getValue());
        record.setRankDelta(parsed.getDelta());
        record.setDirection(parsed.getDirection());

        return record;
    }

    private static String detectTagType(JsonNode tag) {
        if (tag.has("url")) {
            String url = getText(tag, "url");
            if (url != null) {
                if (url.contains("-country-") || url.contains("/country/")) {
                    return "country";
                }
                if (url.contains("-region-") || url.contains("/region/")) {
                    return "region";
                }
            }
        }
        String parentIds = getText(tag, "parent_id_list", "parentIdList");
        if (StringUtils.isNotEmpty(parentIds)) {
            return "country";
        }
        return "unknown";
    }

    private static String getText(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        }
        return null;
    }

    private static String normalizeUrlId(String urlId) {
        if (StringUtils.isEmpty(urlId)) {
            return null;
        }
        return urlId.toLowerCase().trim();
    }
}
