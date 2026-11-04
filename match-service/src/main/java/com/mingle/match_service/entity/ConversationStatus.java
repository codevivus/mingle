package com.mingle.match_service.entity;

public enum ConversationStatus {
    /** Sending and reading both work. */
    ACTIVE,
    /** Sending is blocked, history is still readable. */
    READ_ONLY,
    /** Sending and reading are both blocked until the chat is unlocked. */
    LOCKED
}
