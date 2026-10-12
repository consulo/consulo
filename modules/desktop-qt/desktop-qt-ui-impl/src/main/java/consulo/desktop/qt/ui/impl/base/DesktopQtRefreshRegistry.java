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
package consulo.desktop.qt.ui.impl.base;

import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class DesktopQtRefreshRegistry<T> {
    private static final Logger LOG = Logger.getInstance(DesktopQtRefreshRegistry.class);

    private final Set<T> myOwners = Collections.newSetFromMap(new WeakHashMap<>());
    private final Consumer<T> myRefresh;

    public DesktopQtRefreshRegistry(Consumer<T> refresh) {
        myRefresh = refresh;
    }

    public void register(T owner) {
        synchronized (myOwners) {
            myOwners.add(owner);
        }
    }

    @RequiredUIAccess
    public void refreshAll() {
        List<T> owners;
        synchronized (myOwners) {
            owners = new ArrayList<>(myOwners);
        }

        for (T owner : owners) {
            try {
                myRefresh.accept(owner);
            }
            catch (Throwable e) {
                LOG.warn(e);
            }
        }
    }
}
