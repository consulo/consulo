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
package consulo.externalService.internal.startup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.concurrent.CompletableFuture;

/**
 * The startup wizard. It runs before the normal startup on the first start, and on request later.
 *
 * @author VISTALL
 * @since 2021-09-01
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface StartupCustomizeManager {
    /**
     * Called once by the starter of the frontend. A start with a new configuration leaves the wizard due until it is
     * finished or skipped, so a start interrupted while the wizard was shown brings it back.
     */
    void initialize(boolean newConfigFolder);

    /**
     * Whether the wizard is due before the normal startup.
     */
    boolean isPending();

    /**
     * @return completes once the wizard is closed without a restart - the normal startup follows then
     */
    @RequiredUIAccess
    CompletableFuture<?> showAsync(boolean firstShow);
}
