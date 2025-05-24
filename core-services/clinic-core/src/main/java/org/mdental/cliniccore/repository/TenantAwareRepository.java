package org.mdental.cliniccore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findAllByClinicRealm(String realm);

}