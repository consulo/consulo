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
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.RepositoryTagLocalize;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class PluginTagUtil {
    private PluginTagUtil() {
    }

    public static Collection<LocalizeValue> getLocalizedTags(PluginDescriptor pluginDescriptor) {
        Set<String> tags = pluginDescriptor.getTags();
        if (!tags.isEmpty()) {
            return tags.stream().map(PluginTagUtil::getTagLocalizeValue).toList();
        }

        return List.of();
    }

    public static LocalizeValue getTagLocalizeValue(String tagId) {
        LocalizeKey key = LocalizeKey.of(RepositoryTagLocalize.ID, tagId);
        LocalizeManager manager = LocalizeManager.get();
        if (manager.getUnformattedText(key, manager.getDefaultLocale()) == null) {
            return LocalizeValue.of(tagId);
        }
        return key.getValue();
    }
}
