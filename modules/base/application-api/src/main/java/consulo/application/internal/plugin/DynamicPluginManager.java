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
package consulo.application.internal.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

/**
 * Loads and unloads plugins while the application runs, without a restart.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DynamicPluginManager {
    /**
     * @return whether the plugin manager may load installed plugins without a restart
     */
    boolean isEnabled();

    /**
     * @return a new session which installs and uninstalls plugins together
     */
    DynamicPluginSession createSession();
}
