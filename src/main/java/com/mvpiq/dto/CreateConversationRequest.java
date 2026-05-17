package com.mvpiq.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateConversationRequest {
    private String title;
    private List<String> participantIds; // stringhe UUID dal FE, convertiamo nel resource
}
