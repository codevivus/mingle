package com.mingle.match_service.dto;

import com.mingle.match_service.entity.SwipeAction;
import jakarta.validation.constraints.NotNull;

public record SwipeRequest(@NotNull Long targetUserId, @NotNull SwipeAction action) {
}
