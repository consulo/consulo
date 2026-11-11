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
package consulo.credentialStorage.impl.internal.linux;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Connects to the session bus with the context class loader of the thread set to the one of dbus-java. dbus-java
 * finds its transport with {@link java.util.ServiceLoader} through the context class loader, once, in a static
 * initializer - a thread whose context class loader does not see the module layer of the platform, as the AWT event
 * thread, finds no transport, and dbus-java stays broken for the rest of the session.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
final class DBusSessionBus {
    static DBusConnection connect() throws DBusException {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(DBusConnectionBuilder.class.getClassLoader());
        try {
            return DBusConnectionBuilder.forSessionBus().build();
        }
        finally {
            thread.setContextClassLoader(previous);
        }
    }

    private DBusSessionBus() {
    }
}
