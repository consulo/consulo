/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tools;

import consulo.component.ComponentManager;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.ex.keymap.KeymapGroup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 * @author traff
 */
public abstract class BaseToolKeymapExtension implements KeymapExtension {
    @Override
    public KeymapGroup createGroup(Predicate<AnAction> filtered, ComponentManager project) {
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        String[] ids = actionManager.getActionIds(getActionIdPrefix());
        Arrays.sort(ids);
        KeymapGroupImpl group = new KeymapGroupImpl(getGroupName(), PlatformIconGroup.nodesKeymaptools());


        HashMap<String, KeymapGroupImpl> toolGroupNameToGroup = new HashMap<>();

        for (String id : ids) {
            if (filtered != null && !filtered.test(actionManager.getActionOrStub(id))) {
                continue;
            }
            String groupName = getGroupByActionId(id);

            if (groupName != null && groupName.trim().length() == 0) {
                groupName = null;
            }

            KeymapGroupImpl subGroup = toolGroupNameToGroup.get(groupName);
            if (subGroup == null) {
                subGroup = new KeymapGroupImpl(groupName);
                toolGroupNameToGroup.put(groupName, subGroup);
                if (groupName != null) {
                    group.addGroup(subGroup);
                }
            }

            subGroup.addActionId(id);
        }

        KeymapGroupImpl subGroup = toolGroupNameToGroup.get(null);
        if (subGroup != null) {
            group.addAll(subGroup);
        }

        return group;
    }

    protected abstract String getActionIdPrefix();

    protected abstract String getGroupByActionId(String id);

    protected abstract String getGroupName();
}
