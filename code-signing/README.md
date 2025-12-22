Here's how to generate a self-signed code signing certificate for Ignition module signing:

# Easy Way

Run generate-keystore.sh and answer the questions

# Manual Way

## 1. Create a Keystore with a Self-Signed Certificate

```
  keytool -genkeypair \
    -alias codesigning \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -keystore keystore.jks \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=Your Name, O=Your Organization, L=City, ST=State, C=US"
```

## 2. Export the Certificate to PEM Format

```
  keytool -exportcert \
    -alias codesigning \
    -keystore keystore.jks \
    -storepass changeit \
    -rfc \
    -file codesigning.pem
```

## 3. Convert to P7B (PKCS7) Format

  Using OpenSSL:
  `openssl crl2pkcs7 -nocrl -certfile codesigning.pem -outform DER -out codesigning.p7b`

## 4a. Sign your Module during Build

Copy `gradle.properties.example` to `gradle.properties`
Update `gradle.properties` with the passwords used

## 4b. Sign Your Module Manually

  Once you have both files, use the https://github.com/inductiveautomation/module-signer:

```
  java -jar module-signer.jar \
    -keystore=keystore.jks \
    -keystore-pwd=changeit \
    -alias=codesigning \
    -alias-pwd=changeit \
    -chain=codesigning.p7b \
    -module-in=your-module-unsigned.modl \
    -module-out=your-module-signed.modl
```

  Alternative: Use Keystore Explorer

  The https://github.com/inductiveautomation/module-signer recommends https://keystore-explorer.org/ as an easy GUI tool
  for creating and managing keystores and certificates.

# Important Notes

  - For a self-signed certificate, the "chain" is just your single certificate
  - If you have a CA-signed certificate with intermediates, the p7b must contain certificates in order: your cert first,
  then intermediates, then root
  - Self-signed modules will show a warning in Ignition's gateway when installing, but will function normally

  Sources:
  - https://github.com/inductiveautomation/module-signer
  - https://www.sdk-docs.inductiveautomation.com/docs/getting-started/create-a-module/module-signing/
  - https://gist.github.com/elton-alves/871df55a00cc6b4759cafef496834406