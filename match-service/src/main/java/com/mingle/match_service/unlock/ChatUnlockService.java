package com.mingle.match_service.unlock;

/**
 * Unlocks a locked chat, normally after a payment. This is the only seam to the payment provider:
 * the provider is not chosen yet, so callers depend on this interface and never on a provider SDK.
 */
public interface ChatUnlockService {

    UnlockResult unlock(UnlockRequest request);

    record UnlockRequest(Long conversationId, Long eventId, Long userId) {
    }

    /**
     * @param granted     true when the chat may be unlocked now
     * @param reference   provider-side payment reference, if any
     * @param redirectUrl where the user must complete payment when not yet granted, if applicable
     */
    record UnlockResult(boolean granted, String reference, String redirectUrl) {

        public static UnlockResult granted(String reference) {
            return new UnlockResult(true, reference, null);
        }

        public static UnlockResult paymentRequired(String redirectUrl) {
            return new UnlockResult(false, null, redirectUrl);
        }
    }
}
