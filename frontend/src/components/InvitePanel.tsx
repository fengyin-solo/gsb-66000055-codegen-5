import React, { useState, useEffect } from 'react';
import { createInvitation, getInvitationsByRoom, revokeInvitation } from '../services/invitationService';
import { InviteCandidateRequest, CandidateInvitation, InvitationEffectiveStatus } from '../types';
import { useInterviewStore } from '../store/interview';

interface InvitePanelProps {
  roomId: string;
  roomCode: string;
}

const statusMeta: Record<InvitationEffectiveStatus, { label: string; color: string }> = {
  PENDING: { label: '待加入', color: '#ff9800' },
  ACCEPTED: { label: '已接受', color: '#4caf50' },
  DECLINED: { label: '已拒绝', color: '#f44336' },
  JOINED: { label: '已加入', color: '#2196f3' },
  LEFT: { label: '已离开', color: '#9e9e9e' },
  EXPIRED: { label: '已过期', color: '#9e9e9e' },
  REVOKED: { label: '已撤销', color: '#f44336' },
  EXHAUSTED: { label: '次数已用完', color: '#9c27b0' },
  SUPERSEDED: { label: '已被新邀请取代', color: '#795548' },
};

const TERMINAL_STATUSES: InvitationEffectiveStatus[] = ['EXPIRED', 'REVOKED', 'EXHAUSTED', 'SUPERSEDED'];

const getEffectiveStatus = (invitation: CandidateInvitation): InvitationEffectiveStatus =>
  invitation.effectiveStatus || invitation.status;

const formatDateTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN') : '-');

export const InvitePanel: React.FC<InvitePanelProps> = ({ roomId, roomCode }) => {
  const { invitations, setInvitations } = useInterviewStore();
  const [candidateName, setCandidateName] = useState('');
  const [candidateEmail, setCandidateEmail] = useState('');
  const [expiresInHours, setExpiresInHours] = useState('72');
  const [maxUses, setMaxUses] = useState('1');
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [error, setError] = useState('');

  const fetchInvitations = async () => {
    try {
      const data = await getInvitationsByRoom(roomId);
      setInvitations(data);
    } catch (error) {
      console.error('Failed to fetch invitations:', error);
    }
  };

  useEffect(() => {
    fetchInvitations();
  }, [roomId]);

  const handleCopy = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleSendInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!candidateName.trim() || !candidateEmail.trim()) return;

    const hours = parseInt(expiresInHours, 10);
    const uses = parseInt(maxUses, 10);
    if (!Number.isFinite(hours) || hours <= 0) {
      setError('有效期必须是大于 0 的小时数');
      return;
    }
    if (!Number.isFinite(uses) || uses <= 0) {
      setError('可加入次数必须是大于 0 的整数');
      return;
    }

    const request: InviteCandidateRequest = {
      roomId,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
      expiresInHours: hours,
      maxUses: uses,
    };

    try {
      setError('');
      await createInvitation(request);
      setCandidateName('');
      setCandidateEmail('');
      fetchInvitations();
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送邀请失败');
    }
  };

  const handleRevoke = async (invitationId: string) => {
    try {
      setError('');
      await revokeInvitation(invitationId);
      fetchInvitations();
    } catch (err) {
      setError(err instanceof Error ? err.message : '撤销邀请失败');
    }
  };

  const inviteLink = `${window.location.origin}/join/${roomCode}`;

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '10px 12px',
    background: '#2a2a2a',
    border: '1px solid #444',
    borderRadius: '4px',
    color: '#fff',
    fontSize: '14px',
    boxSizing: 'border-box',
  };

  const labelStyle: React.CSSProperties = {
    display: 'block',
    fontSize: '12px',
    color: '#888',
    marginBottom: '4px',
  };

  return (
    <div style={{
      width: '420px',
      padding: '24px',
      background: '#1e1e1e',
      color: '#e0e0e0',
      overflowY: 'auto',
      borderRight: '1px solid #333',
      display: 'flex',
      flexDirection: 'column',
      gap: '24px',
    }}>
      <h2 style={{ margin: 0, fontSize: '20px', color: '#fff' }}>邀请候选人</h2>

      <div style={{ background: '#2a2a2a', padding: '16px', borderRadius: '8px' }}>
        <div style={{ marginBottom: '12px' }}>
          <div style={{ fontSize: '12px', color: '#888', marginBottom: '4px' }}>房间邀请码</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '18px', fontWeight: 'bold', color: '#fff', letterSpacing: '2px' }}>{roomCode}</span>
            <button
              onClick={() => handleCopy(roomCode, 'code')}
              style={{
                padding: '4px 12px',
                background: copiedField === 'code' ? '#4caf50' : '#3a3a3a',
                color: '#fff',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '12px',
              }}
            >
              {copiedField === 'code' ? '已复制' : '复制'}
            </button>
          </div>
        </div>
        <div>
          <div style={{ fontSize: '12px', color: '#888', marginBottom: '4px' }}>邀请链接</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#bbb', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{inviteLink}</span>
            <button
              onClick={() => handleCopy(inviteLink, 'link')}
              style={{
                padding: '4px 12px',
                background: copiedField === 'link' ? '#4caf50' : '#3a3a3a',
                color: '#fff',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '12px',
              }}
            >
              {copiedField === 'link' ? '已复制' : '复制'}
            </button>
          </div>
        </div>
      </div>

      <form onSubmit={handleSendInvite} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div>
          <label style={labelStyle}>候选人姓名</label>
          <input
            type="text"
            value={candidateName}
            onChange={(e) => setCandidateName(e.target.value)}
            placeholder="请输入候选人姓名"
            style={inputStyle}
          />
        </div>
        <div>
          <label style={labelStyle}>候选人邮箱</label>
          <input
            type="email"
            value={candidateEmail}
            onChange={(e) => setCandidateEmail(e.target.value)}
            placeholder="请输入候选人邮箱"
            style={inputStyle}
          />
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <div style={{ flex: 1 }}>
            <label style={labelStyle}>有效期（小时）</label>
            <input
              type="number"
              min={1}
              value={expiresInHours}
              onChange={(e) => setExpiresInHours(e.target.value)}
              placeholder="如 72"
              style={inputStyle}
            />
          </div>
          <div style={{ flex: 1 }}>
            <label style={labelStyle}>可加入次数</label>
            <input
              type="number"
              min={1}
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              placeholder="如 1"
              style={inputStyle}
            />
          </div>
        </div>
        {error && (
          <div style={{
            color: '#f44336',
            fontSize: '12px',
            padding: '8px 10px',
            background: 'rgba(244,67,54,0.1)',
            borderRadius: '4px',
            border: '1px solid rgba(244,67,54,0.3)',
          }}>
            {error}
          </div>
        )}
        <button
          type="submit"
          style={{
            padding: '10px 16px',
            background: '#2196f3',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: '500',
          }}
        >
          发送邀请
        </button>
        <div style={{ fontSize: '11px', color: '#666', lineHeight: 1.5 }}>
          向同一候选人补发新邀请后，旧邀请将立即失效，且无法恢复。
        </div>
      </form>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <h3 style={{ margin: '0 0 12px', fontSize: '14px', color: '#fff' }}>邀请记录</h3>
        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {invitations.length === 0 ? (
            <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px' }}>
              暂无邀请记录
            </div>
          ) : (
            invitations.map((invitation) => {
              const effectiveStatus = getEffectiveStatus(invitation);
              const meta = statusMeta[effectiveStatus] || { label: effectiveStatus, color: '#666' };
              const revocable = !TERMINAL_STATUSES.includes(effectiveStatus);
              const personalLink = `${window.location.origin}/join?token=${invitation.inviteToken}`;
              return (
                <div key={invitation.id} style={{
                  background: '#2a2a2a',
                  padding: '12px',
                  borderRadius: '6px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '500', color: '#fff' }}>{invitation.candidateName}</span>
                    <span style={{
                      padding: '2px 10px',
                      borderRadius: '12px',
                      fontSize: '11px',
                      color: '#fff',
                      background: meta.color,
                    }}>
                      {meta.label}
                    </span>
                  </div>
                  <div style={{ fontSize: '12px', color: '#888' }}>{invitation.candidateEmail}</div>
                  <div style={{ fontSize: '11px', color: '#777', display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span>
                      使用情况：{invitation.usedCount ?? 0}
                      {invitation.maxUses != null ? ` / ${invitation.maxUses}` : ''} 次
                    </span>
                    <span>有效期至：{formatDateTime(invitation.expiresAt)}</span>
                    {invitation.lastUsedAt && <span>最近使用：{formatDateTime(invitation.lastUsedAt)}</span>}
                    {effectiveStatus === 'REVOKED' && <span>撤销时间：{formatDateTime(invitation.revokedAt)}</span>}
                    {effectiveStatus === 'SUPERSEDED' && <span>已被更新的邀请取代</span>}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '11px', color: '#666' }}>
                      {formatDateTime(invitation.createdAt)}
                    </span>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      {revocable && (
                        <>
                          <button
                            onClick={() => handleCopy(personalLink, `invite-${invitation.id}`)}
                            style={{
                              padding: '4px 10px',
                              background: copiedField === `invite-${invitation.id}` ? '#4caf50' : 'transparent',
                              color: copiedField === `invite-${invitation.id}` ? '#fff' : '#2196f3',
                              border: '1px solid #2196f3',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '11px',
                            }}
                          >
                            {copiedField === `invite-${invitation.id}` ? '已复制' : '复制链接'}
                          </button>
                          <button
                            onClick={() => handleRevoke(invitation.id)}
                            style={{
                              padding: '4px 10px',
                              background: 'transparent',
                              color: '#f44336',
                              border: '1px solid #f44336',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontSize: '11px',
                            }}
                          >
                            撤销
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
