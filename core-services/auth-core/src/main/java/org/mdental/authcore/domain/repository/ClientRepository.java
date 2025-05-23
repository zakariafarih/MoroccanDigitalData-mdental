package org.mdental.authcore.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.mdental.authcore.domain.model.Client;

/**
 * Repository port for client operations.
 */
public interface ClientRepository {
    /**
     * Find a client by its client ID.
     *
     * @param clientId the client ID
     * @return the client if found
     */
    Optional<Client> findByClientId(String clientId);

    /**
     * Save a client.
     *
     * @param client the client to save
     * @return the saved client
     */
    Client save(Client client);

    /**
     * Find a client by its ID.
     *
     * @param id the client ID
     * @return the client if found
     */
    Optional<Client> findById(UUID id);
}