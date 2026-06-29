package com.choosephd.dto;

public class UserInfo {

    private Long id;
    private String username;
    private String role;
    private String membership;

    public UserInfo() {
    }

    public UserInfo(Long id, String username, String role, String membership) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.membership = membership;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMembership() {
        return membership;
    }

    public void setMembership(String membership) {
        this.membership = membership;
    }
}
