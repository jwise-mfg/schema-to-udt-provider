#!/bin/bash
#
# Generate a self-signed code signing certificate for Ignition module signing.
# Creates: keystore.jks, codesigning.pem, codesigning.p7b, ../gradle.properties
#
# This script can be run from any directory - files are always created in
# the code-signing folder where this script resides.
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
GRADLE_PROPS="$PROJECT_DIR/gradle.properties"

# Change to script directory so files are created there
cd "$SCRIPT_DIR"

echo "========================================"
echo "Ignition Module Code Signing Generator"
echo "========================================"
echo
echo "Output directory: $SCRIPT_DIR"
echo

# --- Pre-condition checks ---

echo "Checking prerequisites..."

# Check for keytool
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}Error: 'keytool' not found.${NC}"
    echo "Please ensure Java JDK is installed and keytool is in your PATH."
    exit 1
fi
echo -e "${GREEN}✓${NC} keytool found"

# Check for openssl
if ! command -v openssl &> /dev/null; then
    echo -e "${RED}Error: 'openssl' not found.${NC}"
    echo "Please install OpenSSL and ensure it's in your PATH."
    exit 1
fi
echo -e "${GREEN}✓${NC} openssl found"

# Check if signing files already exist
if [[ -f "$SCRIPT_DIR/keystore.jks" || -f "$SCRIPT_DIR/codesigning.pem" || -f "$SCRIPT_DIR/codesigning.p7b" ]]; then
    echo
    echo -e "${YELLOW}Warning: Code signing files already exist in $SCRIPT_DIR${NC}"
    read -p "Overwrite existing files? (y/N): " overwrite
    if [[ ! "$overwrite" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
    rm -f "$SCRIPT_DIR/keystore.jks" "$SCRIPT_DIR/codesigning.pem" "$SCRIPT_DIR/codesigning.p7b"
fi

# Check if gradle.properties already exists
if [[ -f "$GRADLE_PROPS" ]]; then
    echo
    echo -e "${YELLOW}Warning: gradle.properties already exists at $GRADLE_PROPS${NC}"
    read -p "Overwrite existing gradle.properties? (y/N): " overwrite_gradle
    if [[ ! "$overwrite_gradle" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

echo
echo "--- Certificate Details ---"
echo "Enter the following information for your code signing certificate:"
echo

# --- Gather user input ---

read -p "Your Name (CN): " cn
while [[ -z "$cn" ]]; do
    echo -e "${RED}Name is required.${NC}"
    read -p "Your Name (CN): " cn
done

read -p "Organization (O): " org
while [[ -z "$org" ]]; do
    echo -e "${RED}Organization is required.${NC}"
    read -p "Organization (O): " org
done

read -p "City/Locality (L): " city
while [[ -z "$city" ]]; do
    echo -e "${RED}City is required.${NC}"
    read -p "City/Locality (L): " city
done

read -p "State/Province (ST): " state
while [[ -z "$state" ]]; do
    echo -e "${RED}State is required.${NC}"
    read -p "State/Province (ST): " state
done

read -p "Country Code (C) [US]: " country
country=${country:-US}

read -p "Validity in days [3650]: " validity
validity=${validity:-3650}

echo
echo "--- Keystore Passwords ---"
echo "(Passwords must be at least 6 characters)"
echo

while true; do
    read -s -p "Keystore password: " storepass
    echo
    if [[ ${#storepass} -lt 6 ]]; then
        echo -e "${RED}Password must be at least 6 characters.${NC}"
        continue
    fi
    read -s -p "Confirm keystore password: " storepass_confirm
    echo
    if [[ "$storepass" != "$storepass_confirm" ]]; then
        echo -e "${RED}Passwords do not match.${NC}"
        continue
    fi
    break
done

read -p "Use same password for key? (Y/n): " same_pass
if [[ "$same_pass" =~ ^[Nn]$ ]]; then
    while true; do
        read -s -p "Key password: " keypass
        echo
        if [[ ${#keypass} -lt 6 ]]; then
            echo -e "${RED}Password must be at least 6 characters.${NC}"
            continue
        fi
        read -s -p "Confirm key password: " keypass_confirm
        echo
        if [[ "$keypass" != "$keypass_confirm" ]]; then
            echo -e "${RED}Passwords do not match.${NC}"
            continue
        fi
        break
    done
else
    keypass="$storepass"
fi

# --- Generate files ---

echo
echo "--- Generating Code Signing Files ---"
echo

dname="CN=$cn, O=$org, L=$city, ST=$state, C=$country"
echo "Distinguished Name: $dname"
echo

# Step 1: Create keystore with self-signed certificate
echo "1. Creating keystore with self-signed certificate..."
keytool -genkeypair \
    -alias codesigning \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$validity" \
    -keystore "$SCRIPT_DIR/keystore.jks" \
    -storepass "$storepass" \
    -keypass "$keypass" \
    -dname "$dname"
echo -e "${GREEN}✓${NC} Created keystore.jks"

# Step 2: Export certificate to PEM format
echo "2. Exporting certificate to PEM format..."
keytool -exportcert \
    -alias codesigning \
    -keystore "$SCRIPT_DIR/keystore.jks" \
    -storepass "$storepass" \
    -rfc \
    -file "$SCRIPT_DIR/codesigning.pem"
echo -e "${GREEN}✓${NC} Created codesigning.pem"

# Step 3: Convert to P7B (PKCS7) format
echo "3. Converting to P7B format..."
openssl crl2pkcs7 -nocrl -certfile "$SCRIPT_DIR/codesigning.pem" -outform DER -out "$SCRIPT_DIR/codesigning.p7b"
echo -e "${GREEN}✓${NC} Created codesigning.p7b"

# Step 4: Generate gradle.properties
echo "4. Generating gradle.properties..."

# Use relative paths from project root
cat > "$GRADLE_PROPS" << EOF
# Ignition Module Signing Configuration
# Generated by code-signing/generate-keystore.sh

ignition.signing.keystoreFile=./code-signing/keystore.jks
ignition.signing.keystorePassword=$storepass
ignition.signing.certAlias=codesigning
ignition.signing.certPassword=$keypass
ignition.signing.certFile=./code-signing/codesigning.p7b
EOF

echo -e "${GREEN}✓${NC} Created gradle.properties"

echo
echo "========================================"
echo -e "${GREEN}Code signing files generated successfully!${NC}"
echo "========================================"
echo
echo "Files created in $SCRIPT_DIR:"
echo "  - keystore.jks       (Java keystore with private key)"
echo "  - codesigning.pem    (Certificate in PEM format)"
echo "  - codesigning.p7b    (Certificate chain in PKCS7 format)"
echo
echo "File created in $PROJECT_DIR:"
echo "  - gradle.properties  (Gradle signing configuration)"
echo
echo "You can now build your module with: ./gradlew build"
echo
echo -e "${YELLOW}Note: Self-signed modules will show a warning in Ignition when installing.${NC}"
