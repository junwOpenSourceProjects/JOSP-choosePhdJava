package com.choosephd.importer;

public class RawRankingRecord {

    private String urlId;
    private String nameZh;
    private String nameEn;
    private String nameZhTw;
    private String country;
    private String region;
    private String badgeUrl;
    private String rankDisplay;
    private Integer rankValue;
    private Integer rankDelta;
    private Integer direction;

    public String getUrlId() {
        return urlId;
    }

    public void setUrlId(String urlId) {
        this.urlId = urlId;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }

    public void setNameZhTw(String nameZhTw) {
        this.nameZhTw = nameZhTw;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBadgeUrl() {
        return badgeUrl;
    }

    public void setBadgeUrl(String badgeUrl) {
        this.badgeUrl = badgeUrl;
    }

    public String getRankDisplay() {
        return rankDisplay;
    }

    public void setRankDisplay(String rankDisplay) {
        this.rankDisplay = rankDisplay;
    }

    public Integer getRankValue() {
        return rankValue;
    }

    public void setRankValue(Integer rankValue) {
        this.rankValue = rankValue;
    }

    public Integer getRankDelta() {
        return rankDelta;
    }

    public void setRankDelta(Integer rankDelta) {
        this.rankDelta = rankDelta;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }
}
