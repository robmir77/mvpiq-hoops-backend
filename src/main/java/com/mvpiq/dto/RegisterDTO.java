package com.mvpiq.dto;

import com.mvpiq.enums.UserRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDTO {
    private String username;
    private String email;
    private String password;
    private UserRole role; // opzionale, default a PLAYER
    private String displayName;
}