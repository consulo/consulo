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
package consulo.desktop.qt.ui.impl.image;

import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where the frontend answers a change of the icon library, the way the web frontend answers it by rewriting the
 * icon version of the page - every owner of a rendered icon is asked for that icon once more, which resolves it
 * against whatever library is active now.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class DesktopQtIconRefresher {
    private static final Logger LOG = Logger.getInstance(DesktopQtIconRefresher.class);

    /**
     * An owner is held weakly, so a component whose widget is long gone does not keep it alive - registering is
     * the whole of what an owner has to do, there is nothing to unregister.
     */
    private static final Set<DesktopQtIconOwner> ourOwners = Collections.newSetFromMap(new WeakHashMap<>());

    private DesktopQtIconRefresher() {
    }

    public static void register(DesktopQtIconOwner owner) {
        synchronized (ourOwners) {
            ourOwners.add(owner);
        }
    }

    @RequiredUIAccess
    public static void refreshAll() {
        List<DesktopQtIconOwner> owners;
        synchronized (ourOwners) {
            owners = new ArrayList<>(ourOwners);
        }

        for (DesktopQtIconOwner owner : owners) {
            try {
                owner.refreshIcons();
            }
            catch (Throwable e) {
                LOG.warn(e);
            }
        }
    }
}
