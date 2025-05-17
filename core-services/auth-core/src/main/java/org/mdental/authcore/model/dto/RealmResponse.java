package org.mdental.authcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealmResponse {
    private String issuer;
    private String realmName;
    private String kcRealmAdminUser;
    private String tmpPassword;
}
