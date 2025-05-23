package org.mdental.authcore.infrastructure.jpa;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.Client;
import org.mdental.authcore.domain.repository.ClientRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepositoryImpl extends ClientRepository, JpaRepository<Client, UUID> {
    @Override
    Optional<Client> findByClientId(String clientId);
}