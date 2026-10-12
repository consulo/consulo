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
import consulo.annotation.component.ExtensionAPI;

import java.util.function.Consumer;

/**
 * Contributes pages to the startup wizard, in the order of the extensions. Only the platform contributes pages - a
 * plugin is not installed yet when the wizard first runs.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface StartupCustomizeStepProvider {
    /**
     * Called once for every run of the wizard - the steps hold the state of their pages, so they are made anew each time.
     */
    void collectSteps(StartupCustomizeContext context, Consumer<StartupCustomizeStep> consumer);
}
