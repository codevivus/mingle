package com.mingle.ticket_service.client.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventIdsRequest {
    private Long eventId;
}
