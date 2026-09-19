package com.codeinterview.dto;

public class InviteCandidateRequest {
    private String roomId;
    private String candidateName;
    private String candidateEmail;
    /** 有效期小时数；为空或 <= 0 表示长期有效 */
    private Integer validityHours;
    /** 可加入次数；为空时默认 1 */
    private Integer maxJoinCount;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public Integer getValidityHours() { return validityHours; }
    public void setValidityHours(Integer validityHours) { this.validityHours = validityHours; }
    public Integer getMaxJoinCount() { return maxJoinCount; }
    public void setMaxJoinCount(Integer maxJoinCount) { this.maxJoinCount = maxJoinCount; }
}
