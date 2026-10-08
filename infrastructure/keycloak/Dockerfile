# Keycloak with two extensions. The public-id mapper is ours and gives every
# user a publicId of its own. The email-code extension supplies the emailed
# second factor, which Keycloak lacks. Its version names the Keycloak release
# it was built for, so bump the two together.
FROM maven:3.10.0-eclipse-temurin-21 AS public-id-mapper
WORKDIR /build
COPY public-id-mapper/pom.xml ./
COPY public-id-mapper/src ./src
# CI runs the tests, Checkstyle and PMD. Here the jar is all that is needed.
RUN mvn -B -q package -DskipTests

FROM alpine:3.22.6 AS providers
ARG EMAIL_OTP_VERSION=26.7.1-KC26.8.0
ARG EMAIL_OTP_SHA256=68fba0008a04ff6a63dd8585563ba1d12878f8d9f50711ebc575c4075e99f8d9
WORKDIR /providers
RUN wget -q -O email-otp.jar \
        "https://repo1.maven.org/maven2/io/github/mesutpiskin/keycloak-2fa-email-authenticator/${EMAIL_OTP_VERSION}/keycloak-2fa-email-authenticator-${EMAIL_OTP_VERSION}.jar" \
    && echo "${EMAIL_OTP_SHA256}  email-otp.jar" > email-otp.sha256 \
    && sha256sum -c email-otp.sha256

FROM quay.io/keycloak/keycloak:26.8.0
COPY --from=providers --chown=keycloak:keycloak /providers/email-otp.jar /opt/keycloak/providers/
COPY --from=public-id-mapper --chown=keycloak:keycloak /build/target/public-id-mapper.jar /opt/keycloak/providers/
ENV KC_HEALTH_ENABLED=true
# 1000 is the image's keycloak user. A number lets a runtime check that it is not root.
USER 1000
# The management port answers on 9000. The image has no curl, so bash opens the socket.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/9000 && printf 'GET /health/ready HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
