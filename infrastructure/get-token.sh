#!/bin/bash
# Interactive script to get a Keycloak JWT using the PKCE authorization code flow.
# It signs in as the portal does, so the token carries roles and public_id.
#
# Usage: ./get-token.sh            (reads KEYCLOAK_PUBLIC_URL and PORTAL_REDIRECT_URI from .env)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

env_value() {
    sed -n "s/^$1=//p" "$ENV_FILE" 2>/dev/null | tail -n 1
}

KEYCLOAK_URL="$(env_value KEYCLOAK_PUBLIC_URL)"
REDIRECT_URI="$(env_value PORTAL_REDIRECT_URI)"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REDIRECT_URI="${REDIRECT_URI:-http://localhost:4200/callback}"
OIDC="${KEYCLOAK_URL}/realms/tombtale/protocol/openid-connect"
CLIENT_ID="tombtale-portal"

echo ">> Client ${CLIENT_ID} at ${KEYCLOAK_URL}"
echo ""

# 1. Generate PKCE values securely
echo "1. Generating PKCE Verifier and Challenge..."
CODE_VERIFIER=$(python3 -c "import secrets; print(secrets.token_urlsafe(64))")
CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary | base64 -w0 | tr '+/' '-_' | tr -d '=')
STATE=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
ENCODED_REDIRECT=$(python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=''))" "$REDIRECT_URI")

AUTHORIZE_URL="${OIDC}/auth?client_id=${CLIENT_ID}&redirect_uri=${ENCODED_REDIRECT}&response_type=code&scope=openid+profile+email&code_challenge_method=S256&code_challenge=${CODE_CHALLENGE}&state=${STATE}"

# 2. Request User Interaction
echo ""
echo "=========================================================="
echo "2. Open the following URL in your web browser to log in:"
echo "$AUTHORIZE_URL"
echo "=========================================================="
echo ""
xdg-open "$AUTHORIZE_URL" 2>/dev/null || open "$AUTHORIZE_URL" 2>/dev/null || true

echo "After you log in, the browser goes to ${REDIRECT_URI}."
echo "If the portal is not running, the page fails to load. That is fine:"
echo "copy the whole URL from the address bar."
echo ""
echo -n "3. Paste the URL (or only the 'code' value) here: "
read -r AUTH_CODE

# Smart parsing: handle both raw code and full callback URL
if [[ "$AUTH_CODE" == *"code="* ]]; then
    # Validate state parameter to prevent code substitution attacks
    RETURNED_STATE=$(echo "$AUTH_CODE" | sed -n 's/.*state=\([^&]*\).*/\1/p')
    if [ "$RETURNED_STATE" != "$STATE" ]; then
        echo "ERROR: OAuth state mismatch! Expected '${STATE}' but got '${RETURNED_STATE}'."
        echo "This could indicate a CSRF or code substitution attack. Aborting."
        exit 1
    fi
    AUTH_CODE=$(echo "$AUTH_CODE" | sed -n 's/.*code=\([^&]*\).*/\1/p')
    echo "Extracted and validated code from URL."
fi

if [ -z "$AUTH_CODE" ]; then
    echo "ERROR: Auth code cannot be empty."
    exit 1
fi

echo ""
echo "4. Exchanging code for JWT token..."
RESPONSE=$(curl -s -X POST "${OIDC}/token" \
  -d "grant_type=authorization_code" \
  -d "code=${AUTH_CODE}" \
  -d "client_id=${CLIENT_ID}" \
  -d "redirect_uri=${REDIRECT_URI}" \
  -d "code_verifier=${CODE_VERIFIER}")

# 5. Extract and print the final token
ACCESS_TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; data=json.load(sys.stdin); print(data.get('access_token', 'null'))" 2>/dev/null || echo "null")

if [ "$ACCESS_TOKEN" != "null" ]; then
    echo ""
    echo "🎉 SUCCESS! Here is your JWT Access Token:"
    echo "===================================================================="
    echo "$ACCESS_TOKEN"
    echo "===================================================================="
else
    echo "Failed to get token! Here is the response from Keycloak:"
    echo "$RESPONSE" | python3 -m json.tool
    exit 1
fi
