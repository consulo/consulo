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

import consulo.application.progress.ProgressIndicator;

/**
 * Work the startup wizard runs off the user interface thread once the user finishes it.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
@FunctionalInterface
public interface StartupCustomizeFinishTask {
    /**
     * @return whether the application has to restart for the work to take effect
     */
    boolean run(StartupCustomizeContext context, ProgressIndicator indicator) throws Exception;
}
