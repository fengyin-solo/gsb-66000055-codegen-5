package com.codeinterview.controller;

import com.codeinterview.dto.InviteCandidateRequest;
import com.codeinterview.model.CandidateInvitation;
import com.codeinterview.model.InterviewRoom;
import com.codeinterview.repository.CandidateInvitationRepository;
import com.codeinterview.repository.InterviewRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class CandidateInvitationController {

    private static final int DEFAULT_EXPIRES_IN_HOURS = 72;
    private static final int DEFAULT_MAX_USES = 1;

    @Autowired
    private CandidateInvitationRepository invitationRepository;

    @Autowired
    private InterviewRoomRepository roomRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> createInvitation(@RequestBody InviteCandidateRequest request) {
        InterviewRoom room = roomRepository.findById(request.getRoomId())
                .orElse(null);
        if (room == null) {
            return error(HttpStatus.NOT_FOUND, "房间不存在");
        }

        if (!"WAITING".equals(room.getStatus())) {
            return error(HttpStatus.BAD_REQUEST, "房间状态不是 WAITING，无法发送邀请");
        }

        if (request.getCandidateName() == null || request.getCandidateName().trim().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "候选人姓名不能为空");
        }
        if (request.getCandidateEmail() == null || request.getCandidateEmail().trim().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "候选人邮箱不能为空");
        }

        int expiresInHours = request.getExpiresInHours() != null
                ? request.getExpiresInHours() : DEFAULT_EXPIRES_IN_HOURS;
        if (expiresInHours <= 0) {
            return error(HttpStatus.BAD_REQUEST, "有效期必须大于 0 小时");
        }

        int maxUses = request.getMaxUses() != null ? request.getMaxUses() : DEFAULT_MAX_USES;
        if (maxUses <= 0) {
            return error(HttpStatus.BAD_REQUEST, "可加入次数必须大于 0");
        }

        LocalDateTime now = LocalDateTime.now();

        CandidateInvitation invitation = new CandidateInvitation();
        invitation.setRoomId(request.getRoomId());
        invitation.setCandidateName(request.getCandidateName().trim());
        invitation.setCandidateEmail(request.getCandidateEmail().trim());
        invitation.setInviteToken(UUID.randomUUID().toString());
        invitation.setStatus("PENDING");
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(now.plusHours(expiresInHours));
        invitation.setMaxUses(maxUses);
        invitation.setUsedCount(0);

        invitation = invitationRepository.save(invitation);

        // 补发新邀请时，将同一房间同一候选人仍可用的旧邀请标记为“已被取代”，
        // 旧邀请从此失效且无法恢复，候选人信息关联到最新邀请。
        List<CandidateInvitation> existing = invitationRepository
                .findByRoomIdOrderByCreatedAtDesc(request.getRoomId());
        for (CandidateInvitation old : existing) {
            if (old.getId().equals(invitation.getId())) {
                continue;
            }
            boolean sameEmail = old.getCandidateEmail() != null
                    && old.getCandidateEmail().equalsIgnoreCase(invitation.getCandidateEmail());
            if (sameEmail && old.isUsable()) {
                old.setSupersededAt(now);
                old.setSupersededById(invitation.getId());
                invitationRepository.save(old);
            }
        }

        return new ResponseEntity<>(invitation, HttpStatus.CREATED);
    }

    @GetMapping("/room/{roomId}")
    public List<CandidateInvitation> getInvitationsByRoomId(@PathVariable String roomId) {
        return invitationRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    @GetMapping("/{invitationId}")
    public ResponseEntity<?> getInvitationById(@PathVariable String invitationId) {
        return invitationRepository.findById(invitationId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "邀请不存在"));
    }

    @PutMapping("/{invitationId}/status")
    public ResponseEntity<?> updateInvitationStatus(
            @PathVariable String invitationId,
            @RequestParam String status) {

        CandidateInvitation invitation = invitationRepository.findById(invitationId)
                .orElse(null);
        if (invitation == null) {
            return error(HttpStatus.NOT_FOUND, "邀请不存在");
        }

        if (!List.of("PENDING", "ACCEPTED", "DECLINED", "JOINED", "LEFT").contains(status)) {
            return error(HttpStatus.BAD_REQUEST, "无效的状态值");
        }

        if (invitation.getRevokedAt() != null || invitation.getSupersededById() != null) {
            return error(HttpStatus.CONFLICT, "邀请已失效（" + invitation.getEffectiveStatus() + "），无法更新状态");
        }

        invitation.setStatus(status);

        if ("JOINED".equals(status)) {
            invitation.setJoinedAt(LocalDateTime.now());
        }

        return ResponseEntity.ok(invitationRepository.save(invitation));
    }

    @GetMapping("/token/{inviteToken}")
    public ResponseEntity<?> getInvitationByToken(@PathVariable String inviteToken) {
        // 无论邀请是否仍可用都返回邀请信息，由调用方根据 effectiveStatus/blockReason 展示阻止原因
        return invitationRepository.findByInviteToken(inviteToken)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "邀请链接无效"));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<?> revokeInvitation(@PathVariable String invitationId) {
        CandidateInvitation invitation = invitationRepository.findById(invitationId)
                .orElse(null);
        if (invitation == null) {
            return error(HttpStatus.NOT_FOUND, "邀请不存在");
        }

        if (invitation.getRevokedAt() != null) {
            return error(HttpStatus.CONFLICT, "邀请已被撤销");
        }

        // 软撤销：保留邀请记录，提前失效
        invitation.setRevokedAt(LocalDateTime.now());
        return ResponseEntity.ok(invitationRepository.save(invitation));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "message", message,
                "status", status.value()
        ));
    }
}
