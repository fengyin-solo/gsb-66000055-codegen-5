import React, { useState, useEffect } from 'react';
import { createInvitation, getInvitationsByRoom, revokeInvitation } from '../services/invitationService';
import { InviteCandidateRequest, getInvitationStatusConfig } from '../types';
import { useInterviewStore } from '../store/interview';
import { useToastStore } from '../store/toast';

interface InvitePanelProps {
  roomId: string;
  roomCode: string;
}

const VALIDITY_PRESETS: { label: string; hours: number }[] = [
  { label: '2小时', hours: 2 },
  { label: '24小时', hours: 24 },
  { label: '3天', hours: 72 },
  { label: '7天', hours: 168 },
  { label: '长期有效', hours: 0 },
];

const formatTime = (value?: string | null): string =>
  value ? new Date(value).toLocaleString() : '—';

export const InvitePanel: React.FC<InvitePanelProps> = ({ roomId, roomCode }) => {
  const { invitations, setInvitations } = useInterviewStore();
  const toast = useToastStore();
  const [candidateName, setCandidateName] = useState('');
  const [candidateEmail, setCandidateEmail] = useState('');
  const [validityHours, setValidityHours] = useState(24);
  const [maxJoinCount, setMaxJoinCount] = useState(1);
  const [sending, setSending] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

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

    const request: InviteCandidateRequest = {
      roomId,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
      validityHours,
      maxJoinCount,
    };

    setSending(true);
    try {
      const created = await createInvitation(request);
      toast.success('邀请已发送，旧邀请（如有）已自动失效');
      setCandidateName('');
      setCandidateEmail('');
      await fetchInvitations();
      handleCopy(`${window.location.origin}/join?token=${created.inviteToken}`, `link-${created.id}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : '发送邀请失败';
      toast.error(message);
      console.error('Failed to send invitation:', error);
    } finally {
      setSending(false);
    }
  };

  const handleRevoke = async (invitationId: string) => {
    try {
      await revokeInvitation(invitationId);
      toast.success('邀请已提前失效');
      fetchInvitations();
    } catch (error) {
      const message = error instanceof Error ? error.message : '撤销邀请失败';
      toast.error(message);
      console.error('Failed to revoke invitation:', error);
    }
  };

  const roomInviteLink = `${window.location.origin}/join?code=${roomCode}`;

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
          <div style={{ fontSize: '12px', color: '#888', marginBottom: '4px' }}>房间码链接（非受控）</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#bbb', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{roomInviteLink}</span>
            <button
              onClick={() => handleCopy(roomInviteLink, 'link')}
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
          <label style={{ display: 'block', fontSize: '12px', color: '#888', marginBottom: '4px' }}>候选人姓名</label>
          <input
            type="text"
            value={candidateName}
            onChange={(e) => setCandidateName(e.target.value)}
            placeholder="请输入候选人姓名"
            style={inputStyle}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '12px', color: '#888', marginBottom: '4px' }}>候选人邮箱</label>
          <input
            type="email"
            value={candidateEmail}
            onChange={(e) => setCandidateEmail(e.target.value)}
            placeholder="请输入候选人邮箱（凭此匹配补发邀请）"
            style={inputStyle}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '12px', color: '#888', marginBottom: '6px' }}>有效期</label>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {VALIDITY_PRESETS.map((preset) => (
              <button
                key={preset.hours}
                type="button"
                onClick={() => setValidityHours(preset.hours)}
                style={{
                  padding: '6px 10px',
                  fontSize: '12px',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  border: validityHours === preset.hours ? '1px solid #2196f3' : '1px solid #444',
                  background: validityHours === preset.hours ? 'rgba(33,150,243,0.2)' : '#2a2a2a',
                  color: validityHours === preset.hours ? '#90caf9' : '#bbb',
                }}
              >
                {preset.label}
              </button>
            ))}
          </div>
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '12px', color: '#888', marginBottom: '4px' }}>
            可加入次数
          </label>
          <input
            type="number"
            min={1}
            max={99}
            value={maxJoinCount}
            onChange={(e) => setMaxJoinCount(Math.max(1, Number(e.target.value) || 1))}
            style={inputStyle}
          />
          <div style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>
            候选人断线重连时可适当调大（如 3）；达到次数后链接立即失效
          </div>
        </div>
        <button
          type="submit"
          disabled={sending}
          style={{
            padding: '10px 16px',
            background: '#2196f3',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: sending ? 'not-allowed' : 'pointer',
            fontSize: '14px',
            fontWeight: '500',
            opacity: sending ? 0.6 : 1,
          }}
        >
          {sending ? '发送中...' : '发送邀请'}
        </button>
        <div style={{ fontSize: '11px', color: '#666' }}>
          同一候选人补发新邀请后，旧邀请立即失效且不会恢复，候选人信息自动关联到最新邀请。
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
              const config = getInvitationStatusConfig(invitation.effectiveStatus || invitation.status);
              const used = invitation.usedJoinCount ?? 0;
              const max = invitation.maxJoinCount ?? 1;
              const canRevoke = config.accessible;
              const link = `${window.location.origin}/join?token=${invitation.inviteToken}`;
              return (
                <div key={invitation.id} style={{
                  background: '#2a2a2a',
                  padding: '12px',
                  borderRadius: '6px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px',
                  opacity: config.accessible ? 1 : 0.75,
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: '500', color: '#fff' }}>{invitation.candidateName}</span>
                    <span style={{
                      padding: '2px 10px',
                      borderRadius: '12px',
                      fontSize: '11px',
                      color: '#fff',
                      background: config.color,
                      whiteSpace: 'nowrap',
                    }}>
                      {config.label}
                    </span>
                  </div>
                  <div style={{ fontSize: '12px', color: '#888' }}>{invitation.candidateEmail}</div>
                  <div style={{ fontSize: '11px', color: '#777', display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span>使用次数：{used}/{max}
                      {invitation.lastUsedAt ? `（最近 ${formatTime(invitation.lastUsedAt)}）` : '（尚未使用）'}
                    </span>
                    <span>有效期至：{invitation.expiresAt ? formatTime(invitation.expiresAt) : '长期有效'}</span>
                    <span>发送时间：{formatTime(invitation.createdAt)}</span>
                    {invitation.revokedAt && <span style={{ color: '#f44336' }}>撤销时间：{formatTime(invitation.revokedAt)}</span>}
                    {invitation.status === 'SUPERSEDED' && <span style={{ color: '#9e9e9e' }}>已被补发的新邀请替换</span>}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                    <button
                      onClick={() => handleCopy(link, `inv-link-${invitation.id}`)}
                      style={{
                        padding: '4px 10px',
                        background: 'transparent',
                        color: '#2196f3',
                        border: '1px solid #2196f3',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '11px',
                      }}
                    >
                      {copiedField === `inv-link-${invitation.id}` ? '已复制链接' : '复制邀请链接'}
                    </button>
                    {canRevoke && (
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
                        提前失效
                      </button>
                    )}
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
