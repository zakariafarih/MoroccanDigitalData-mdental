# RSA Key Rotation Instructions

To rotate keys manually, follow these steps:

1. Generate new key pair using OpenSSL:
   ```
   openssl genrsa -out private_new.pem 2048
   openssl rsa -in private_new.pem -pubout -out public_new.pem
   ```

2. Back up the current keys:
   ```
   cp private.pem private_old.pem
   cp public.pem public_old.pem
   ```

3. Replace the current keys with the new ones:
   ```
   cp private_new.pem private.pem
   cp public_new.pem public.pem
   ```

4. Restart the service to load the new keys

5. Keep the old public key available for token verification
