package josp.choosphd.domain.university;

import java.time.LocalDateTime;

/**
 * 院校主表(V1 schema: id PK, url_id 唯一, region_id FK, country_code 字符)
 */
public class UniversityPO {
    private Long id;
    private String urlId;
    private String nameEn;
    private String nameZh;
    private String logoUrl;
    private Integer regionId;
    private String countryCode;
    private String city;
    private String website;
    private Short foundedYear;
    private String type;
    private String aliases;          // json,存为 raw string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrlId() { return urlId; }
    public void setUrlId(String urlId) { this.urlId = urlId; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Integer getRegionId() { return regionId; }
    public void setRegionId(Integer regionId) { this.regionId = regionId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public Short getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Short foundedYear) { this.foundedYear = foundedYear; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
