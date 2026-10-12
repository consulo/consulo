/*
 * Copyright 2013-2024 consulo.io
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
package consulo.localize.internal;

import java.nio.file.Path;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-09-01
 */
public interface LocalizeManagerEx {
    void visitPlugins(List<Runnable> actions);

    void afterInit();

    /**
     * Reads the texts of a plugin which is on disk but not loaded - one installed during this run, which the application
     * only loads at its next start. Only the prepared index of the plugin is read, so texts kept in files of their own
     * stay missing until then.
     *
     * @return whether the plugin had an index to read
     */
    boolean loadPluginIndex(Path pluginPath);
}
