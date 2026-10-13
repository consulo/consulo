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
package consulo.it.internal;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps credentials in memory, so a test never touches the keychain of the machine it runs on.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public class HeadlessCredentialStore implements CredentialStore {
    private record Entry(@Nullable String userName, String password) {
    }

    private final Map<String, Entry> myEntries = new ConcurrentHashMap<>();

    @Override
    public @Nullable Credentials get(CredentialAttributes attributes) {
        Entry entry = myEntries.get(attributes.getServiceName());
        if (entry == null) {
            return null;
        }
        String userName = StringUtil.nullize(attributes.getUserName());
        if (userName != null && !Objects.equals(userName, entry.userName())) {
            return null;
        }
        return new Credentials(entry.userName(), entry.password());
    }

    @Override
    public void set(CredentialAttributes attributes, @Nullable Credentials credentials) {
        String password = credentials == null ? null : credentials.getPasswordAsString();
        if (StringUtil.isEmpty(password)) {
            myEntries.remove(attributes.getServiceName());
            return;
        }
        String userName = StringUtil.nullize(attributes.getUserName());
        myEntries.put(attributes.getServiceName(), new Entry(userName == null ? credentials.getUserName() : userName, password));
    }
}
