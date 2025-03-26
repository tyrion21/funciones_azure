package com.userrolemgmt.model;

import java.sql.Timestamp;

public class UserRole {
    private Long userId;
    private Long roleId;
    private Timestamp assignedAt;

    // Constructores
    public UserRole() {
    }

    public UserRole(Long userId, Long roleId, Timestamp assignedAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedAt = assignedAt;
    }

    // Getters y Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "userId=" + userId +
                ", roleId=" + roleId +
                ", assignedAt=" + assignedAt +
                '}';
    }
}