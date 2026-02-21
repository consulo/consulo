/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.externalTool.impl.internal;

import consulo.component.persist.RoamingType;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.component.persist.scheme.SchemeProcessor;
import consulo.ui.ex.action.ActionManager;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nullable;

import java.util.*;

public abstract class BaseToolManager<T extends Tool> {
    private final ActionManager myActionManager;
    private final SchemeManager<ToolsGroup<T>, ToolsGroup<T>> mySchemeManager;

    public BaseToolManager(ActionManager actionManagerEx, SchemeManagerFactory factory) {
        myActionManager = actionManagerEx;

        mySchemeManager = factory.createSchemeManager(getSchemesPath(), createProcessor(), RoamingType.DEFAULT);

        mySchemeManager.loadSchemes();
        registerActions();
    }

    protected abstract String getSchemesPath();

    protected abstract SchemeProcessor<ToolsGroup<T>, ToolsGroup<T>> createProcessor();

    @Nullable
    public static String convertString(String s) {
        if (s != null && s.trim().length() == 0) {
            return null;
        }
        return s;
    }

    public List<T> getTools() {
        List<T> result = new ArrayList<>();
        for (ToolsGroup<T> group : mySchemeManager.getAllSchemes()) {
            result.addAll(group.getElements());
        }
        return result;
    }

    public List<T> getTools(String group) {
        List<T> list = new ArrayList<>();
        ToolsGroup<T> groupByName = mySchemeManager.findSchemeByName(group);
        if (groupByName != null) {
            list.addAll(groupByName.getElements());
        }
        return list;
    }

    /**
     * Get all not empty group names of tools in array
     */
    String[] getGroups(T[] tools) {
        List<String> list = new ArrayList<>();
        for (T tool : tools) {
            if (!list.contains(tool.getGroup())) {
                list.add(tool.getGroup());
            }
        }
        return ArrayUtil.toStringArray(list);
    }

    public String getGroupByActionId(String actionId) {
        for (T tool : getTools()) {
            if (Objects.equals(actionId, tool.getActionId())) {
                return tool.getGroup();
            }
        }
        return null;
    }

    public List<ToolsGroup<T>> getGroups() {
        return mySchemeManager.getAllSchemes();
    }

    public void setTools(ToolsGroup[] tools) {
        mySchemeManager.clearAllSchemes();
        for (ToolsGroup newGroup : tools) {
            mySchemeManager.addNewScheme(newGroup, true);
        }
        registerActions();
    }


    void registerActions() {
        unregisterActions();

        // register
        Set<String> registeredIds = new HashSet<>(); // to prevent exception if 2 or more targets have the same name

        List<T> tools = getTools();
        for (T tool : tools) {
            String actionId = tool.getActionId();

            if (!registeredIds.contains(actionId)) {
                registeredIds.add(actionId);
                myActionManager.registerAction(actionId, createToolAction(tool));
            }
        }
    }

    protected ToolAction createToolAction(T tool) {
        return new ToolAction(tool);
    }

    protected abstract String getActionIdPrefix();

    private void unregisterActions() {
        // unregister Tool actions
        for (String oldId : myActionManager.getActionIds(getActionIdPrefix())) {
            myActionManager.unregisterAction(oldId);
        }
    }
}
