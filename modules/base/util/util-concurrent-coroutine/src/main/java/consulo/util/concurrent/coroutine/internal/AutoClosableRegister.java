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
package consulo.util.concurrent.coroutine.internal;

import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-01-29
 */
public class AutoClosableRegister {
    public static final Key<AutoClosableRegister> KEY = Key.create(AutoClosableRegister.class);

    private List<AutoCloseable> myCloseables = Lists.newLockFreeCopyOnWriteList();

    public static void register(UserDataHolder holder, AutoCloseable autoCloseable) {
        AutoClosableRegister register = holder.getUserData(KEY);
        if (register == null) {
            register = holder.putUserDataIfAbsent(KEY, new AutoClosableRegister());
        }

        register.myCloseables.add(autoCloseable);
    }

    public void closeAll(Consumer<Throwable> errorHandler) {
        for (AutoCloseable closeable : myCloseables) {
            try {
                closeable.close();
            }
            catch (Exception e) {
                errorHandler.accept(e);
            }
        }
    }
}
