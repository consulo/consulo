/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.credentialStorage.impl.internal.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.impl.internal.SharedLogger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2025-04-04
 */
public class WindowsCredentialStore implements CredentialStore {
    private static final CredAdvapi32 INSTANCE = CredAdvapi32.INSTANCE;

    @Nullable
    @Override
    public Credentials get(@Nonnull CredentialAttributes attributes) {
        final CredAdvapi32.PCREDENTIAL pcredential = new CredAdvapi32.PCREDENTIAL();

        boolean read = false;
        try {
            // MSDN doc doesn't mention threading safety, so let's just be careful and synchronize the access
            synchronized (INSTANCE) {
                read = INSTANCE.CredRead(attributes.getServiceName(), CredAdvapi32.CRED_TYPE_GENERIC, 0, pcredential);
            }

            if (read) {
                final CredAdvapi32.CREDENTIAL credential = new CredAdvapi32.CREDENTIAL(pcredential.credential);

                byte[] secretBytes = credential.CredentialBlob.getByteArray(0, credential.CredentialBlobSize);
                final String secret = new String(secretBytes, StandardCharsets.UTF_8);
                final String username = credential.UserName;

                return new Credentials(username, secret);
            }
        }
        finally {
            if (pcredential.credential != null) {
                synchronized (INSTANCE) {
                    INSTANCE.CredFree(pcredential.credential);
                }
            }
        }

        return null;
    }

    @Override
    public void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials) {
        String key = attributes.getServiceName();

        if (credentials == null) {
            synchronized (INSTANCE) {
                try {
                    INSTANCE.CredDelete(key, CredAdvapi32.CRED_TYPE_GENERIC, 0);
                }
                catch (LastErrorException e) {
                    SharedLogger.LOG.warn(e);
                }
            }
        }  else {
            byte[] credBlob = credentials.getPassword().toByteArray(false);

            final String username = credentials.getUserName();

            final CredAdvapi32.CREDENTIAL cred = buildCred(key, username, credBlob);

            try {
                synchronized (INSTANCE) {
                    INSTANCE.CredWrite(cred, 0);
                }
            }
            catch (LastErrorException e) {
                SharedLogger.LOG.warn(e);
            }
            finally {
                cred.CredentialBlob.clear(credBlob.length);
            }
        }
    }

    private CredAdvapi32.CREDENTIAL buildCred(String key, String username, byte[] credentialBlob) {
        final CredAdvapi32.CREDENTIAL credential = new CredAdvapi32.CREDENTIAL();

        credential.Flags = 0;
        credential.Type = CredAdvapi32.CRED_TYPE_GENERIC;
        credential.TargetName = key;


        credential.CredentialBlobSize = credentialBlob.length;
        credential.CredentialBlob = getPointer(credentialBlob);

        credential.Persist = CredAdvapi32.CRED_PERSIST_LOCAL_MACHINE;
        credential.UserName = username;

        return credential;
    }

    private Pointer getPointer(byte[] array) {
        Pointer p = new Memory(array.length);
        p.write(0, array, 0, array.length);

        return p;
    }
}
