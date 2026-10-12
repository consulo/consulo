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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public interface PluginFilterHost {
    Key<PluginFilterHost> KEY = Key.of(PluginFilterHost.class);

    List<PluginDescriptor> getAllPlugins();

    PluginSorter getSorter();

    @RequiredUIAccess
    void reSort(PluginSorter sorter);

    @Nullable String getTagFilter();

    @RequiredUIAccess
    void setTagFilter(@Nullable String tag);
}
