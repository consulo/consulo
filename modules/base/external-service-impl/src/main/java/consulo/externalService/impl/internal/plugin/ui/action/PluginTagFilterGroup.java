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
package consulo.externalService.impl.internal.plugin.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginTagFilterGroup extends ActionGroup implements DumbAware {
    public PluginTagFilterGroup() {
        super("Tag", null, null);
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        assert e != null;
        PluginTab tab = e.getRequiredData(PluginTab.KEY);

        Map<LocalizeValue, String> allTags = new TreeMap<>();
        for (PluginDescriptor descriptor : tab.getPluginList().getAll()) {
            Set<String> tags = descriptor.getTags();
            if (!tags.isEmpty()) {
                for (String tag : tags) {
                    allTags.put(PluginTab.getTagLocalizeValue(tag), tag);
                }
            }
        }

        List<PluginTagFilterAction> actions = new ArrayList<>();
        for (Map.Entry<LocalizeValue, String> entry : allTags.entrySet()) {
            actions.add(new PluginTagFilterAction(entry.getValue(), entry.getKey()));
        }
        return actions.toArray(new AnAction[actions.size()]);
    }
}
