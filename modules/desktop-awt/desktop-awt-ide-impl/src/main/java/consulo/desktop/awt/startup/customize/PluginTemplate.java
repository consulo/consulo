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
package consulo.desktop.awt.startup.customize;

import consulo.container.plugin.PluginId;
import consulo.ui.image.Image;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-08-02
 */
public record PluginTemplate(PluginId id,
                             Set<PluginId> pluginIds,
                             String name,
                             String description,
                             Image[] images,
                             int downloadsAll) {
}
