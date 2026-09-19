import { request } from './api';
import type { InviteCandidateRequest, CandidateInvitation, InvitationAccessView } from '../types';

export function createInvitation(data: InviteCandidateRequest): Promise<CandidateInvitation> {
  return request<CandidateInvitation>('/invitations', {
    method: 'POST',
    body: data,
  });
}

export function getInvitationsByRoom(roomId: string): Promise<CandidateInvitation[]> {
  return request<CandidateInvitation[]>(`/invitations/room/${roomId}`);
}

/** 候选人凭邀请链接访问：返回是否可访问及不可访问的原因 */
export function getInvitationByToken(token: string): Promise<InvitationAccessView> {
  return request<InvitationAccessView>(`/invitations/token/${token}`);
}

export function updateInvitationStatus(invitationId: string, status: string): Promise<CandidateInvitation> {
  return request<CandidateInvitation>(`/invitations/${invitationId}/status`, {
    method: 'PUT',
    body: { status },
  });
}

/** 面试官提前失效邀请（撤销，记录保留） */
export function revokeInvitation(invitationId: string): Promise<CandidateInvitation> {
  return request<CandidateInvitation>(`/invitations/${invitationId}/revoke`, {
    method: 'POST',
  });
}

export function deleteInvitation(invitationId: string): Promise<CandidateInvitation> {
  return revokeInvitation(invitationId);
}
