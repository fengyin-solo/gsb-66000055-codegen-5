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
    /** Stored lifecycle status: PENDING / JOINED / REVOKED / SUPERSEDED. */
    private String status;
    /** null 表示长期有效 */
    private LocalDateTime expiresAt;
    /** 凭此邀请最多可加入的次数，默认 1 */
    private Integer maxJoinCount = 1;
    /** 已经使用该邀请加入的次数 */
    private Integer usedJoinCount = 0;
    private LocalDateTime joinedAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    /** 被新邀请替换时，指向最新邀请 */
    private String supersededByInvitationId;
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 对应当前时刻的实际状态（过期、次数用完为动态计算）。
     * 取值：PENDING / JOINED / EXPIRED / EXHAUSTED / REVOKED / SUPERSEDED。
     */
    @Transient
    public String getEffectiveStatus() {
        if ("REVOKED".equals(status) || "SUPERSEDED".equals(status)) {
            return status;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        if (maxJoinCount != null && usedJoinCount != null && usedJoinCount >= maxJoinCount) {
            return "EXHAUSTED";
        }
        return status;
    }

    @Transient
    public boolean isAccessible() {
        return "PENDING".equals(getEffectiveStatus()) || "JOINED".equals(getEffectiveStatus());
    }

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
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxJoinCount() { return maxJoinCount; }
    public void setMaxJoinCount(Integer maxJoinCount) { this.maxJoinCount = maxJoinCount; }
    public Integer getUsedJoinCount() { return usedJoinCount; }
    public void setUsedJoinCount(Integer usedJoinCount) { this.usedJoinCount = usedJoinCount; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getSupersededByInvitationId() { return supersededByInvitationId; }
    public void setSupersededByInvitationId(String supersededByInvitationId) { this.supersededByInvitationId = supersededByInvitationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
