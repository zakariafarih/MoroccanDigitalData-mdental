package org.mdental.authcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String sub;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String realm;
    private Set<String> roles;
}