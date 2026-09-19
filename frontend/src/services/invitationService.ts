import { request } from './api';
import type { InviteCandidateRequest, CandidateInvitation } from '../types';

export function createInvitation(data: InviteCandidateRequest): Promise<CandidateInvitation> {
  return request<CandidateInvitation>('/invitations', {
    method: 'POST',
    body: data,
  });
}

export function getInvitationsByRoom(roomId: string): Promise<CandidateInvitation[]> {
  return request<CandidateInvitation[]>(`/invitations/room/${roomId}`);
}

export function getInvitationByToken(token: string): Promise<CandidateInvitation> {
  return request<CandidateInvitation>(`/invitations/token/${token}`);
}

export function updateInvitationStatus(invitationId: string, status: string): Promise<CandidateInvitation> {
  return request<CandidateInvitation>(`/invitations/${invitationId}/status`, {
    method: 'PUT',
    body: { status },
  });
}

export function revokeInvitation(invitationId: string): Promise<CandidateInvitation> {
  return request<CandidateInvitation>(`/invitations/${invitationId}`, {
    method: 'DELETE',
  });
}
