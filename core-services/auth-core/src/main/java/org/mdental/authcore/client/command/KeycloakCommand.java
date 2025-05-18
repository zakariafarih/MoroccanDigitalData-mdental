package org.mdental.authcore.client.command;

public interface KeycloakCommand<T> {
    T execute();
}