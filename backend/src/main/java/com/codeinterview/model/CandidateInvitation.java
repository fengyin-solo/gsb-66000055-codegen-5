package com.codeinterview.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_invitations")
public class CandidateInvitation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String roomId;
    private String candidateName;
    private String candidateEmail;
    private String inviteToken;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private int usedCount = 0;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime supersededAt;
    private String supersededById;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public LocalDateTime getSupersededAt() { return supersededAt; }
    public void setSupersededAt(LocalDateTime supersededAt) { this.supersededAt = supersededAt; }
    public String getSupersededById() { return supersededById; }
    public void setSupersededById(String supersededById) { this.supersededById = supersededById; }

    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Transient
    public boolean isExhausted() {
        return maxUses != null && usedCount >= maxUses;
    }

    @Transient
    public boolean isUsable() {
        return revokedAt == null && supersededById == null && !isExpired() && !isExhausted();
    }

    @Transient
    public String getEffectiveStatus() {
        if (revokedAt != null) return "REVOKED";
        if (supersededById != null) return "SUPERSEDED";
        if (isExpired()) return "EXPIRED";
        if (isExhausted()) return "EXHAUSTED";
        return status;
    }

    @Transient
    public String getBlockReason() {
        if (revokedAt != null) return "邀请已被面试官撤销";
        if (supersededById != null) return "该邀请已被新的邀请取代，请使用最新的邀请链接";
        if (isExpired()) return "邀请已过期";
        if (isExhausted()) return "邀请已达到可加入次数上限";
        return null;
    }

    @Transient
    public Integer getRemainingUses() {
        if (maxUses == null) return null;
        return Math.max(0, maxUses - usedCount);
    }

    @Transient
    public String getInviteLink() {
        return "/join?token=" + inviteToken;
    }
}
