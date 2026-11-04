package com.mingle.match_service.unlock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Placeholder until a payment provider is chosen. Grants every unlock unless
 * {@code chat.unlock.stub.grant=false}, which is handy to exercise the "payment required" path.
 * A real provider adds its own bean and sets {@code chat.unlock.provider} to something else.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "chat.unlock.provider", havingValue = "stub", matchIfMissing = true)
public class StubChatUnlockService implements ChatUnlockService {

    private final boolean grant;

    public StubChatUnlockService(@Value("${chat.unlock.stub.grant:true}") boolean grant) {
        this.grant = grant;
    }

    @Override
    public UnlockResult unlock(UnlockRequest request) {
        log.warn("STUB unlock for conversation {} by user {}: no payment was taken", request.conversationId(), request.userId());
        return grant ? UnlockResult.granted("stub") : UnlockResult.paymentRequired(null);
    }
}
