package com.codeinterview.exception;

/**
 * 邀请访问被拒绝（过期、已撤销、次数用尽、已被新邀请替换等）。
 */
public class InvitationAccessException extends RuntimeException {
    private final String reasonCode;

    public InvitationAccessException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
