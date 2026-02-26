package com.argus.api.dto.response;

/**
 * Typed response for the unread alert count endpoint.
 * Serializes to: {"unreadCount": N}
 */
public record UnreadCountResponse(long unreadCount) {
    public static UnreadCountResponse of(long count) {
        return new UnreadCountResponse(count);
    }
}
