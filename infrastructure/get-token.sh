#!/bin/bash
# Interactive script to get Zitadel JWT Token using PKCE Authorization Code flow

# 1. Read Client ID
ID_FILE=".client-id"
if [ -f "$ID_FILE" ]; then
    CLIENT_ID=$(cat "$ID_FILE")
    # Clean any whitespace or newlines
    CLIENT_ID=$(echo "$CLIENT_ID" | tr -d '[:space:]')
else
    echo -n "Enter your Zitadel Client ID: "
    read -r CLIENT_ID
    # Save it for next time
    echo "$CLIENT_ID" > "$ID_FILE"
fi

if [ -z "$CLIENT_ID" ]; then
    echo "ERROR: Client ID cannot be empty."
    exit 1
fi

echo ">> Configured Client ID: $CLIENT_ID"
echo ""

# 2. Generate PKCE values securely
echo "1. Generating PKCE Verifier and Challenge..."
CODE_VERIFIER=$(python3 -c "import secrets; print(secrets.token_urlsafe(64))")
CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary | base64 -w0 | tr '+/' '-_' | tr -d '=')

AUTHORIZE_URL="http://localhost:8080/oauth/v2/authorize?client_id=${CLIENT_ID}&redirect_uri=http://localhost:3000/callback&response_type=code&scope=openid+profile+email&code_challenge_method=S256&code_challenge=${CODE_CHALLENGE}"

# 3. Request User Interaction
echo ""
echo "=========================================================="
echo "2. Open the following URL in your web browser to log in:"
echo "$AUTHORIZE_URL"
echo "=========================================================="
echo ""
echo "(If your OS supports it, I am attempting to auto-open it behind the scenes...)"
echo ""
# Attempt to auto-open the browser 
xdg-open "$AUTHORIZE_URL" 2>/dev/null || open "$AUTHORIZE_URL" 2>/dev/null || true

echo "After a successful login, you'll be redirected to a localhost:3000 page."
echo "If your browser says 'Unable to connect', that is perfectly fine!"
echo ""
echo "Look at the URL bar. It will look something like this:"
echo "http://localhost:3000/callback?code=euyf3sdt_A1b...&state="
echo ""
echo -n "3. Copy the 'code' value from the URL and paste it here: "
read -r AUTH_CODE

# Basic validation to handle accidentally pasting the entire URL
if [[ "$AUTH_CODE" == *"code="* ]]; then
    AUTH_CODE=$(echo "$AUTH_CODE" | sed -n 's/.*code=\([^&]*\).*/\1/p')
    echo "Extracted code from URL."
fi

if [ -z "$AUTH_CODE" ]; then
    echo "ERROR: Auth code cannot be empty."
    exit 1
fi

echo ""
echo "4. Exchanging code for JWT token..."
RESPONSE=$(curl -s -X POST http://localhost:8080/oauth/v2/token \
  -d "grant_type=authorization_code" \
  -d "code=${AUTH_CODE}" \
  -d "client_id=${CLIENT_ID}" \
  -d "redirect_uri=http://localhost:3000/callback" \
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
    echo "Failed to get token! Here is the response from Zitadel:"
    echo "$RESPONSE" | python3 -m json.tool
    
    echo ""
    echo "HINT: Did you remember to go to your Zitadel Application settings,"
    echo "find 'Auth Token Type', and change it from 'Bearer' to 'JWT'?"
fi
