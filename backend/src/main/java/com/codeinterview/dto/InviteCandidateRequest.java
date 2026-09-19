package com.codeinterview.dto;

public class InviteCandidateRequest {
    private String roomId;
    private String candidateName;
    private String candidateEmail;
    private Integer expiresInHours;
    private Integer maxUses;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public Integer getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(Integer expiresInHours) { this.expiresInHours = expiresInHours; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
}
