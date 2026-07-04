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
package consulo.navigationBar.model;

import org.jspecify.annotations.Nullable;

import java.util.EventListener;
import java.util.List;

/**
 * Listener for {@link NavBarVm} state changes: items, selected index, child popup and activation requests.
 * All events are delivered on the UI thread.
 */
public interface NavBarVmListener extends EventListener {
    default void itemsChanged(List<? extends NavBarItemVm> items) {
    }

    default void selectedIndexChanged(int selectedIndex) {
    }

    default void itemSelectionChanged(NavBarItemVm item, boolean selected) {
    }

    default void popupChanged(@Nullable NavBarPopupVm<?> popup) {
    }

    default void activationRequested(NavBarVmItem item) {
    }
}
