package com.codeinterview.service;

import com.codeinterview.dto.InvitationAccessView;
import com.codeinterview.dto.InviteCandidateRequest;
import com.codeinterview.exception.InvitationAccessException;
import com.codeinterview.model.CandidateInvitation;
import com.codeinterview.model.InterviewRoom;
import com.codeinterview.repository.CandidateInvitationRepository;
import com.codeinterview.repository.InterviewRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationService {

    @Autowired
    private CandidateInvitationRepository invitationRepository;

    @Autowired
    private InterviewRoomRepository roomRepository;

    /**
     * 发送邀请。同一房间内同一候选人邮箱若仍有可访问的旧邀请，
     * 旧邀请会被标记为 SUPERSEDED（不会因此重新生效），候选人信息随新邀请保留。
     */
    @Transactional
    public CandidateInvitation createInvitation(InviteCandidateRequest request) {
        if (request.getRoomId() == null || request.getRoomId().isBlank()) {
            throw new IllegalArgumentException("房间ID不能为空");
        }
        if (request.getCandidateName() == null || request.getCandidateName().isBlank()) {
            throw new IllegalArgumentException("候选人姓名不能为空");
        }
        if (request.getCandidateEmail() == null || request.getCandidateEmail().isBlank()) {
            throw new IllegalArgumentException("候选人邮箱不能为空");
        }

        int maxJoinCount = request.getMaxJoinCount() == null ? 1 : request.getMaxJoinCount();
        if (maxJoinCount < 1) {
            throw new IllegalArgumentException("可加入次数至少为 1");
        }
        if (request.getValidityHours() != null && request.getValidityHours() != 0 && request.getValidityHours() < 1) {
            throw new IllegalArgumentException("有效期必须为正数小时");
        }

        InterviewRoom room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("房间不存在"));

        if (!"WAITING".equals(room.getStatus())) {
            throw new IllegalArgumentException("房间状态不是 WAITING，无法发送邀请");
        }

        // 补发邀请：作废旧邀请（不删除，保留记录与候选人信息）
        List<CandidateInvitation> existing =
                invitationRepository.findByRoomIdAndCandidateEmailIgnoreCaseOrderByCreatedAtDesc(
                        request.getRoomId(), request.getCandidateEmail().trim());
        LocalDateTime now = LocalDateTime.now();

        CandidateInvitation newInvitation = new CandidateInvitation();
        newInvitation.setRoomId(request.getRoomId());
        newInvitation.setCandidateName(request.getCandidateName().trim());
        newInvitation.setCandidateEmail(request.getCandidateEmail().trim());
        newInvitation.setInviteToken(UUID.randomUUID().toString());
        newInvitation.setStatus("PENDING");
        newInvitation.setMaxJoinCount(maxJoinCount);
        newInvitation.setUsedJoinCount(0);
        if (request.getValidityHours() != null && request.getValidityHours() > 0) {
            newInvitation.setExpiresAt(now.plusHours(request.getValidityHours()));
        }
        newInvitation.setCreatedAt(now);
        newInvitation = invitationRepository.save(newInvitation);

        for (CandidateInvitation old : existing) {
            if (old.isAccessible()) {
                old.setStatus("SUPERSEDED");
                old.setSupersededByInvitationId(newInvitation.getId());
                invitationRepository.save(old);
            }
        }

        return newInvitation;
    }

    /** 候选人凭邀请链接查看邀请（不消耗次数）。 */
    @Transactional(readOnly = true)
    public InvitationAccessView getAccessView(String inviteToken) {
        CandidateInvitation invitation = invitationRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new InvitationAccessException("INVALID_TOKEN", "邀请链接无效或不存在"));

        Optional<String> blockedReason = checkBlockedReason(invitation);
        if (blockedReason.isPresent()) {
            return InvitationAccessView.blocked(invitation, blockedReason.get());
        }

        InvitationAccessView view = new InvitationAccessView();
        view.setInviteToken(invitation.getInviteToken());
        view.setAccessible(true);
        view.setEffectiveStatus(invitation.getEffectiveStatus());
        view.setCandidateName(invitation.getCandidateName());
        view.setCandidateEmail(invitation.getCandidateEmail());
        view.setRoomId(invitation.getRoomId());
        InterviewRoom room = roomRepository.findById(invitation.getRoomId()).orElse(null);
        if (room == null) {
            return InvitationAccessView.blocked(invitation, "面试房间不存在或已被删除");
        }
        if ("COMPLETED".equals(room.getStatus()) || "CANCELLED".equals(room.getStatus())) {
            return InvitationAccessView.blocked(invitation,
                    "面试房间已" + ("COMPLETED".equals(room.getStatus()) ? "结束" : "取消") + "，无法加入");
        }
        view.setRoomTitle(room.getTitle());
        view.setExpiresAt(invitation.getExpiresAt());
        view.setMaxJoinCount(invitation.getMaxJoinCount());
        view.setUsedJoinCount(invitation.getUsedJoinCount());
        view.setCreatedAt(invitation.getCreatedAt());
        return view;
    }

    /**
     * 校验邀请是否可用于加入房间，通过后立即记录一次使用（原子操作，避免并发突破次数上限）。
     * 过期、已撤销、次数用尽、被替换时抛出带明确原因的 InvitationAccessException。
     */
    @Transactional
    public synchronized CandidateInvitation consumeForJoin(String roomId, String inviteToken, String candidateEmail) {
        CandidateInvitation invitation = validateForJoin(roomId, inviteToken, candidateEmail);

        // 双重检查：在锁内重新确认次数未被并发请求耗尽
        Optional<String> blockedReason = checkBlockedReason(invitation);
        if (blockedReason.isPresent()) {
            throw new InvitationAccessException(invitation.getEffectiveStatus(), blockedReason.get());
        }

        recordJoin(invitation);
        return invitation;
    }

    /**
     * 校验邀请是否可用于加入房间，返回邀请实体（不消耗次数）。
     */
    private CandidateInvitation validateForJoin(String roomId, String inviteToken, String candidateEmail) {
        if (inviteToken == null || inviteToken.isBlank()) {
            throw new InvitationAccessException("INVALID_TOKEN", "缺少邀请凭证，请使用邀请链接加入");
        }

        CandidateInvitation invitation = invitationRepository.findByInviteToken(inviteToken.trim())
                .orElseThrow(() -> new InvitationAccessException("INVALID_TOKEN", "邀请链接无效或不存在"));

        if (!invitation.getRoomId().equals(roomId)) {
            throw new InvitationAccessException("ROOM_MISMATCH", "邀请链接与面试房间不匹配");
        }

        Optional<String> blockedReason = checkBlockedReason(invitation);
        if (blockedReason.isPresent()) {
            throw new InvitationAccessException(invitation.getEffectiveStatus(), blockedReason.get());
        }

        if (candidateEmail != null && !candidateEmail.isBlank()
                && !candidateEmail.trim().equalsIgnoreCase(invitation.getCandidateEmail())) {
            throw new InvitationAccessException("CANDIDATE_MISMATCH",
                    "该邀请仅对候选人 " + invitation.getCandidateEmail() + " 有效，请使用对应邀请链接");
        }

        return invitation;
    }

    /** 候选人成功加入后记录一次使用。 */
    private void recordJoin(CandidateInvitation invitation) {
        LocalDateTime now = LocalDateTime.now();
        invitation.setUsedJoinCount((invitation.getUsedJoinCount() == null ? 0 : invitation.getUsedJoinCount()) + 1);
        invitation.setLastUsedAt(now);
        if (invitation.getJoinedAt() == null) {
            invitation.setJoinedAt(now);
        }
        if (!"JOINED".equals(invitation.getStatus())) {
            invitation.setStatus("JOINED");
        }
        invitationRepository.save(invitation);
    }

    /** 面试官提前失效邀请（撤销，记录保留）。 */
    @Transactional
    public CandidateInvitation revoke(String invitationId) {
        CandidateInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("邀请不存在"));

        if ("REVOKED".equals(invitation.getStatus())) {
            return invitation;
        }
        if ("SUPERSEDED".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("该邀请已被新邀请替换，无需撤销");
        }

        invitation.setStatus("REVOKED");
        invitation.setRevokedAt(LocalDateTime.now());
        return invitationRepository.save(invitation);
    }

    /**
     * 返回邀请当前不可访问的原因；可访问时返回 empty。
     */
    private Optional<String> checkBlockedReason(CandidateInvitation invitation) {
        String stored = invitation.getStatus();
        if ("REVOKED".equals(stored)) {
            return Optional.of("邀请已被面试官撤销，无法访问，请联系面试官重新发送邀请");
        }
        if ("SUPERSEDED".equals(stored)) {
            return Optional.of("该邀请已失效，面试官已补发新邀请，请使用最新的邀请链接");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.of("邀请已过期（有效期至 " + invitation.getExpiresAt() + "），请联系面试官重新发送邀请");
        }
        int used = invitation.getUsedJoinCount() == null ? 0 : invitation.getUsedJoinCount();
        int max = invitation.getMaxJoinCount() == null ? 1 : invitation.getMaxJoinCount();
        if (used >= max) {
            return Optional.of("邀请的可加入次数已用完（" + used + "/" + max + "），请联系面试官重新发送邀请");
        }
        return Optional.empty();
    }
}
