ARG KEYCLOAK_VERSION
ARG KEYCLOAK_IMAGE_DIGEST
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}@${KEYCLOAK_IMAGE_DIGEST}

COPY --chown=1000:0 target/forwardmeasure-keycloak-theme-1.0.0.jar /opt/keycloak/providers/forwardmeasure-keycloak-theme.jar

USER 1000
