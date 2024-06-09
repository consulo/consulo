
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

package consulo.ide.impl.idea.tools;

import consulo.ui.ex.action.ActionManager;
import consulo.component.persist.RoamingType;
import consulo.component.persist.scheme.SchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.ArrayUtil;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    if (s != null && s.trim().length() == 0) return null;
    return s;
  }

  public List<T> getTools() {
    ArrayList<T> result = new ArrayList<T>();
    for (ToolsGroup group : mySchemeManager.getAllSchemes()) {
      result.addAll(group.getElements());
    }
    return result;
  }

  public List<T> getTools(String group) {
    ArrayList<T> list = new ArrayList<T>();
    ToolsGroup groupByName = mySchemeManager.findSchemeByName(group);
    if (groupByName != null) {
      list.addAll(groupByName.getElements());
    }
    return list;
  }

  /**
   * Get all not empty group names of tools in array
   */
  String[] getGroups(T[] tools) {
    ArrayList<String> list = new ArrayList<String>();
    for (int i = 0; i < tools.length; i++) {
      T tool = tools[i];
      if (!list.contains(tool.getGroup())) {
        list.add(tool.getGroup());
      }
    }
    return ArrayUtil.toStringArray(list);
  }

  public String getGroupByActionId(String actionId) {
    for (T tool : getTools()) {
      if (Comparing.equal(actionId, tool.getActionId())) {
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
    HashSet registeredIds = new HashSet(); // to prevent exception if 2 or more targets have the same name

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
    String[] oldIds = myActionManager.getActionIds(getActionIdPrefix());
    for (int i = 0; i < oldIds.length; i++) {
      String oldId = oldIds[i];
      myActionManager.unregisterAction(oldId);
    }
  }
}
