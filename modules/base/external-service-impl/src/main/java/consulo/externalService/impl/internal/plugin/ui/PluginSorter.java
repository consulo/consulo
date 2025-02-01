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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeManager;
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
    NAME(ExternalServiceLocalize.columnPluginsName(), pluginDescriptor -> {
        String name = pluginDescriptor.getName();
        name = name.toLowerCase(LocalizeManager.get().getLocale());

        if (!Character.isAlphabetic(name.charAt(0))) {
            // find first alphabetic symbol
            for (int i = 0; i < name.length(); i++) {
                if (Character.isAlphabetic(name.charAt(i))) {
                    return name.substring(i, name.length());
                }
            }
            return name;
        } else {
            return name;
        }
    }),
    RATING(ExternalServiceLocalize.columnPluginsRate(), pluginDescriptor -> ((PluginNode) pluginDescriptor).getRating()),
    DOWNLOADS(ExternalServiceLocalize.columnPluginsDownloads(), PluginDescriptor::getDownloads),
    LAST_UPDATED(ExternalServiceLocalize.columnPluginsDate(), pluginDescriptor -> ((PluginNode) pluginDescriptor).getDate()),
    STATUS(LocalizeValue.localizeTODO("Status"), PluginDescriptor::getStatus);

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
