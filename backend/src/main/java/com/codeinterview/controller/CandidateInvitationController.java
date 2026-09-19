package com.codeinterview.controller;

import com.codeinterview.dto.InvitationAccessView;
import com.codeinterview.dto.InviteCandidateRequest;
import com.codeinterview.model.CandidateInvitation;
import com.codeinterview.repository.CandidateInvitationRepository;
import com.codeinterview.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class CandidateInvitationController {

    @Autowired
    private CandidateInvitationRepository invitationRepository;

    @Autowired
    private InvitationService invitationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createInvitation(@RequestBody InviteCandidateRequest request) {
        CandidateInvitation invitation = invitationService.createInvitation(request);
        return toResponse(invitation);
    }

    /** 面试官查看房间的邀请记录（含候选人信息与使用情况） */
    @GetMapping("/room/{roomId}")
    public List<CandidateInvitation> getInvitationsByRoomId(@PathVariable String roomId) {
        return invitationRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    @GetMapping("/{invitationId}")
    public CandidateInvitation getInvitationById(@PathVariable String invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));
    }

    /** 面试官提前失效邀请（软撤销，记录保留可查） */
    @PostMapping("/{invitationId}/revoke")
    public CandidateInvitation revokeInvitation(@PathVariable String invitationId) {
        return invitationService.revoke(invitationId);
    }

    /** 兼容旧前端：DELETE 同样执行软撤销而非删除记录 */
    @DeleteMapping("/{invitationId}")
    public CandidateInvitation deleteInvitation(@PathVariable String invitationId) {
        return invitationService.revoke(invitationId);
    }

    /**
     * 候选人凭邀请链接访问。邀请不可用时仍返回 200，
     * 但 accessible=false 且附带具体原因，前端据此展示阻止页面。
     */
    @GetMapping("/token/{inviteToken}")
    public InvitationAccessView getInvitationByToken(@PathVariable String inviteToken) {
        return invitationService.getAccessView(inviteToken);
    }

    private Map<String, Object> toResponse(CandidateInvitation invitation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", invitation.getId());
        response.put("roomId", invitation.getRoomId());
        response.put("candidateName", invitation.getCandidateName());
        response.put("candidateEmail", invitation.getCandidateEmail());
        response.put("inviteToken", invitation.getInviteToken());
        response.put("status", invitation.getStatus());
        response.put("expiresAt", invitation.getExpiresAt());
        response.put("maxJoinCount", invitation.getMaxJoinCount());
        response.put("usedJoinCount", invitation.getUsedJoinCount());
        response.put("createdAt", invitation.getCreatedAt());
        response.put("inviteLink", "/join?token=" + invitation.getInviteToken());
        return response;
    }
}
