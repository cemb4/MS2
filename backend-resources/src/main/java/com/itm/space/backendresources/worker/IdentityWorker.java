package com.itm.space.backendresources.worker;

import com.itm.space.backendresources.exception.BackendResourcesException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityWorker {
    private final Keycloak keycloakClient;
    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(UserRepresentation user) {
        try {
            Response response = keycloakClient.realm(realm).users().create(user);
            return CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException ex) {
            log.error("Exception on \"createUser\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.resolve(ex.getResponse().getStatus()));
        }
    }

    public UserRepresentation getUserById(UUID id) {
        try {
            return keycloakClient.realm(realm).users().get(String.valueOf(id)).toRepresentation();
        } catch (RuntimeException ex) {
            log.error("Exception on \"getUserById\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<RoleRepresentation> getUserRoles(UUID id) {
        try {
            return keycloakClient.realm(realm).users().get(String.valueOf(id)).roles().getAll().getRealmMappings();
        } catch (RuntimeException ex) {
            log.error("Exception on \"getUserRoles\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<GroupRepresentation> getUserGroups(UUID id) {
        try {
            return keycloakClient.realm(realm).users().get(String.valueOf(id)).groups();
        } catch (RuntimeException ex) {
            log.error("Exception on \"getUserGroups\": ", ex);
            throw new BackendResourcesException(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}