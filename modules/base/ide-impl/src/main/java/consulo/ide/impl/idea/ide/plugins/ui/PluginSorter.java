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
package consulo.ide.impl.idea.ide.plugins.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.ide.impl.idea.ide.plugins.PluginNode;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public enum PluginSorter {
    AS_IS(LocalizeValue.empty(), pluginDescriptor -> {
        throw new IllegalArgumentException();
    }),
    NAME(IdeLocalize.columnPluginsName(), PluginDescriptor::getName),
    RATING(IdeLocalize.columnPluginsRate(), pluginDescriptor -> ((PluginNode) pluginDescriptor).getRating()),
    DOWNLOADS(IdeLocalize.columnPluginsDownloads(), PluginDescriptor::getDownloads),
    LAST_UPDATED(IdeLocalize.columnPluginsDate(), pluginDescriptor -> ((PluginNode) pluginDescriptor).getDate()),
    STATUS(IdeLocalize.pluginStatusInstalled(), PluginDescriptor::getStatus);

    private final LocalizeValue mySortName;
    private final Function<PluginDescriptor, Comparable> myValueGetter;

    public static final PluginSorter DEFAULT_SORTER = NAME;

    PluginSorter(LocalizeValue fieldName, Function<PluginDescriptor, Comparable> valueGetter) {
        mySortName = fieldName;
        myValueGetter = valueGetter;
    }

    public LocalizeValue getSortName() {
        return mySortName;
    }

    public Function<PluginDescriptor, Comparable> getValueGetter() {
        return myValueGetter;
    }
}
