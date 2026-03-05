package com.mvpiq.dto;

import com.mvpiq.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private UserRole role;
    private Boolean verified;
    private Boolean isCreator;
    private Boolean isTrainer;
}
