package josp.choosphd.api.university.dto;

import java.util.List;
import java.util.Map;

public class UniversityDetailDTO {
    private String urlId;
    private String name;
    private String cnName;
    private String region;
    private String country;
    private String logo;
    private Map<String, List<RankingEntryPoint>> rankings;
    private Map<String, List<TrendPoint>> trends;
    private List<String> subjects;

    public String getUrlId() { return urlId; }
    public void setUrlId(String urlId) { this.urlId = urlId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCnName() { return cnName; }
    public void setCnName(String cnName) { this.cnName = cnName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public Map<String, List<RankingEntryPoint>> getRankings() { return rankings; }
    public void setRankings(Map<String, List<RankingEntryPoint>> rankings) { this.rankings = rankings; }
    public Map<String, List<TrendPoint>> getTrends() { return trends; }
    public void setTrends(Map<String, List<TrendPoint>> trends) { this.trends = trends; }
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    public static class RankingEntryPoint {
        private Integer year;
        private Integer rank;
        private Double score;
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }

    public static class TrendPoint {
        private Integer year;
        private Integer rank;
        private String direction;
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }
}
