/*
 * Copyright 2013-2026 consulo.io
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
package consulo.web.internal.credential;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.internal.AsyncCredentialStore;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Keeps only what the browser encrypted: the file holds ciphertext, the key stays in the browser, and the plain
 * value lives in memory once a page decrypted it.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public class WebBrowserCredentialStore implements AsyncCredentialStore {
    private static final Logger LOG = Logger.getInstance(WebBrowserCredentialStore.class);

    private static final long BLOCKING_GET_TIMEOUT_SECONDS = 30;

    private record Entry(@Nullable String userName, String cipherText) {
    }

    private final WebBrowserCrypto myCrypto;
    private final Path myFile;
    private final Map<String, Entry> myEntries = new ConcurrentHashMap<>();
    private final Map<String, Credentials> myCache = new ConcurrentHashMap<>();

    public WebBrowserCredentialStore(WebBrowserCrypto crypto, Path file) {
        myCrypto = crypto;
        myFile = file;
        load();
    }

    @Override
    public @Nullable Credentials get(CredentialAttributes attributes) {
        Credentials cached = cached(attributes);
        if (cached != null || entry(attributes) == null || UIAccess.isUIThread()) {
            return cached;
        }

        try {
            return getAsync(attributes).get(BLOCKING_GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            LOG.warn("No browser page decrypted " + attributes.getServiceName() + " in time", e);
            return null;
        }
    }

    @Override
    public CompletableFuture<@Nullable Credentials> getAsync(CredentialAttributes attributes) {
        Credentials cached = cached(attributes);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        Entry entry = entry(attributes);
        if (entry == null) {
            return CompletableFuture.completedFuture(null);
        }

        String serviceName = attributes.getServiceName();
        return myCrypto.decrypt(entry.cipherText()).<@Nullable Credentials>thenApply(password -> {
            Credentials credentials = new Credentials(entry.userName(), password);
            if (myEntries.get(serviceName) == entry) {
                myCache.put(serviceName, credentials);
            }
            return credentials;
        }).exceptionally(error -> {
            LOG.warn("The browser could not decrypt " + serviceName + " - it was saved from another browser or its site data was cleared", error);
            return null;
        });
    }

    @Override
    public void set(CredentialAttributes attributes, @Nullable Credentials credentials) {
        String serviceName = attributes.getServiceName();
        String password = credentials == null ? null : credentials.getPasswordAsString();
        if (StringUtil.isEmpty(password)) {
            myCache.remove(serviceName);
            if (myEntries.remove(serviceName) != null) {
                save();
            }
            return;
        }

        String userName = StringUtil.nullize(attributes.getUserName());
        if (userName == null) {
            userName = credentials.getUserName();
        }
        Credentials value = new Credentials(userName, password);
        myCache.put(serviceName, value);
        if (attributes.isPasswordMemoryOnly()) {
            return;
        }

        String storedUserName = userName;
        myCrypto.encrypt(password).whenComplete((cipherText, error) -> {
            if (error != null) {
                LOG.warn("The browser could not encrypt " + serviceName + ", it is kept in memory only", error);
                return;
            }
            if (myCache.get(serviceName) != value) {
                return;
            }
            myEntries.put(serviceName, new Entry(storedUserName, cipherText));
            save();
        });
    }

    private @Nullable Credentials cached(CredentialAttributes attributes) {
        Credentials credentials = myCache.get(attributes.getServiceName());
        return credentials != null && matches(attributes, credentials.getUserName()) ? credentials : null;
    }

    private @Nullable Entry entry(CredentialAttributes attributes) {
        Entry entry = myEntries.get(attributes.getServiceName());
        return entry != null && matches(attributes, entry.userName()) ? entry : null;
    }

    private static boolean matches(CredentialAttributes attributes, @Nullable String userName) {
        String expected = StringUtil.nullize(attributes.getUserName());
        return expected == null || Objects.equals(expected, userName);
    }

    private void load() {
        if (!Files.exists(myFile)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(myFile)) {
            properties.load(stream);
        }
        catch (IOException e) {
            LOG.error("Cannot read " + myFile, e);
            return;
        }

        for (String serviceName : properties.stringPropertyNames()) {
            String value = properties.getProperty(serviceName);
            int separator = value.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String encodedUserName = value.substring(0, separator);
            String userName = encodedUserName.isEmpty()
                ? null
                : new String(Base64.getDecoder().decode(encodedUserName), StandardCharsets.UTF_8);
            myEntries.put(serviceName, new Entry(userName, value.substring(separator + 1)));
        }
    }

    private synchronized void save() {
        Properties properties = new Properties();
        myEntries.forEach((serviceName, entry) -> {
            String userName = entry.userName();
            String encodedUserName = userName == null
                ? ""
                : Base64.getEncoder().encodeToString(userName.getBytes(StandardCharsets.UTF_8));
            properties.setProperty(serviceName, encodedUserName + ":" + entry.cipherText());
        });

        try {
            Files.createDirectories(myFile.getParent());
            Path temp = myFile.resolveSibling(myFile.getFileName() + ".tmp");
            try (OutputStream stream = Files.newOutputStream(temp)) {
                properties.store(stream, null);
            }
            try {
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
            }
            catch (UnsupportedOperationException ignored) {
            }
            Files.move(temp, myFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException e) {
            LOG.error("Cannot write " + myFile, e);
        }
    }
}
