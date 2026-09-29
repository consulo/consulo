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
import consulo.container.plugin.PluginId;
import consulo.ui.ex.font.BundledFont;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface BundledFontRegistry {
    Map<PluginId, List<BundledFont>> getFonts();
}
