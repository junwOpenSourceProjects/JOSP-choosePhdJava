package josp.choosphd.domain.auth;

import java.time.LocalDateTime;

public class UserPO {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String role;
    private Integer enabled;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer v) { this.enabled = v; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v) { this.lastLoginAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
}
