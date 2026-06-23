package josp.choosphd.domain.dict;

public class DictSourcePO {
    private Integer id;
    private String code;
    private String nameEn;
    private String nameZh;
    private String organization;
    private String methodologyUrl;
    private String scale;
    private Integer hasTrend;
    private Integer hasScore;
    private Integer active;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getMethodologyUrl() { return methodologyUrl; }
    public void setMethodologyUrl(String v) { this.methodologyUrl = v; }
    public String getScale() { return scale; }
    public void setScale(String scale) { this.scale = scale; }
    public Integer getHasTrend() { return hasTrend; }
    public void setHasTrend(Integer hasTrend) { this.hasTrend = hasTrend; }
    public Integer getHasScore() { return hasScore; }
    public void setHasScore(Integer hasScore) { this.hasScore = hasScore; }
    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
}
