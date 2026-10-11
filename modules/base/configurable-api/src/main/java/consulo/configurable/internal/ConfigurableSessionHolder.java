/*
 * Copyright 2013-2022 consulo.io
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
package consulo.configurable.internal;

import consulo.configurable.ConfigurableSession;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfigurableSessionHolder {
    private static final Key<List<ConfigurableSession>> SESSIONS = Key.create("ConfigurableSessionHolder.SESSIONS");

    @RequiredUIAccess
    public static ConfigurableSession get() {
        return Objects.requireNonNull(find(UIAccess.current()), "Session is not initialized");
    }

    public static @Nullable ConfigurableSession getNullable() {
        UIAccess uiAccess = findUIAccess();
        return uiAccess == null ? null : find(uiAccess);
    }

    @RequiredUIAccess
    public static void register(ConfigurableSession session) {
        UIAccess uiAccess = UIAccess.current();

        List<ConfigurableSession> sessions = uiAccess.getUserData(SESSIONS);
        if (sessions == null) {
            sessions = new CopyOnWriteArrayList<>();
            uiAccess.putUserData(SESSIONS, sessions);
        }

        sessions.add(session);
    }

    @RequiredUIAccess
    public static void unregister(ConfigurableSession session) {
        List<ConfigurableSession> sessions = UIAccess.current().getUserData(SESSIONS);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    private static @Nullable ConfigurableSession find(UIAccess uiAccess) {
        List<ConfigurableSession> sessions = uiAccess.getUserData(SESSIONS);
        if (sessions == null) {
            return null;
        }

        List<ConfigurableSession> snapshot = List.copyOf(sessions);
        return snapshot.isEmpty() ? null : snapshot.getLast();
    }

    private static @Nullable UIAccess findUIAccess() {
        if (UIAccess.isUIThread()) {
            return UIAccess.current();
        }

        List<UIAccess> all = UIAccess.listAll();
        return all.size() == 1 ? all.getFirst() : null;
    }
}
