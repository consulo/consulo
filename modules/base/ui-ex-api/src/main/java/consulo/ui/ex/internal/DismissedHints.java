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
package consulo.ui.ex.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

/**
 * What the user said not to show again - banners, warnings, one-way hints. The answers belong to the user rather than to
 * the machine, so they travel with the exported and synchronized settings.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DismissedHints {
    static DismissedHints getInstance() {
        return Application.get().getInstance(DismissedHints.class);
    }

    /**
     * @param id a stable id of the hint, namespaced by its kind - {@code banner.dumbMode}
     */
    boolean isDismissed(String id);

    void dismiss(String id);
}
