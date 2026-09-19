package com.codeinterview.dto;

import com.codeinterview.model.CandidateInvitation;

import java.time.LocalDateTime;

/**
 * 候选人凭邀请链接访问邀请时返回的视图。
 * 邀请不可访问时不返回 roomId/roomTitle，避免泄露房间信息。
 */
public class InvitationAccessView {
    private String inviteToken;
    private boolean accessible;
    private String effectiveStatus;
    private String reason;
    private String candidateName;
    private String candidateEmail;
    private String roomId;
    private String roomTitle;
    private LocalDateTime expiresAt;
    private Integer maxJoinCount;
    private Integer usedJoinCount;
    private LocalDateTime createdAt;

    public static InvitationAccessView blocked(CandidateInvitation invitation, String reason) {
        InvitationAccessView view = new InvitationAccessView();
        view.setInviteToken(invitation.getInviteToken());
        view.setAccessible(false);
        view.setEffectiveStatus(invitation.getEffectiveStatus());
        view.setReason(reason);
        view.setCandidateName(invitation.getCandidateName());
        view.setCandidateEmail(invitation.getCandidateEmail());
        view.setExpiresAt(invitation.getExpiresAt());
        view.setMaxJoinCount(invitation.getMaxJoinCount());
        view.setUsedJoinCount(invitation.getUsedJoinCount());
        view.setCreatedAt(invitation.getCreatedAt());
        return view;
    }

    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }
    public String getEffectiveStatus() { return effectiveStatus; }
    public void setEffectiveStatus(String effectiveStatus) { this.effectiveStatus = effectiveStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxJoinCount() { return maxJoinCount; }
    public void setMaxJoinCount(Integer maxJoinCount) { this.maxJoinCount = maxJoinCount; }
    public Integer getUsedJoinCount() { return usedJoinCount; }
    public void setUsedJoinCount(Integer usedJoinCount) { this.usedJoinCount = usedJoinCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
