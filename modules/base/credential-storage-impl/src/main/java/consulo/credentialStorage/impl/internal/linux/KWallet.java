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
package consulo.credentialStorage.impl.internal.linux;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

import java.util.List;

/**
 * Inner DBus interface representing KWallet.
 */
@DBusInterfaceName("org.kde.KWallet")
interface KWallet extends DBusInterface {
    String localWallet();

    List<String> wallets();

    List<String> users(String wallet);

    boolean isOpen(int walletId);

    int open(String wallet, long wId, String appId);

    int close(int walletId, boolean force, String appId) throws DBusException;

    java.util.Map<String, Variant<String>> readPasswordList(int walletId, String folder, String key, String appId);

    boolean removeFolder(int walletId, String folder, String appId);

    int writePassword(int walletId, String folder, String key, String value, String appId);
}
