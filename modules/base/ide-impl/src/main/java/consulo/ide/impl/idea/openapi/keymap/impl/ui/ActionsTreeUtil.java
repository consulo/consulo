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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.AllIcons;
import consulo.application.util.registry.Registry;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.idea.ide.actionMacro.ActionMacro;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapImpl;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.KeyMapBundle;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.image.Image;
import consulo.util.lang.function.Condition;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.function.Predicate;

public class ActionsTreeUtil {
  private static final Logger LOG = Logger.getInstance(ActionsTreeUtil.class);

  public static final String MAIN_MENU_TITLE = KeyMapBundle.message("main.menu.action.title");
  public static final String MAIN_TOOLBAR = KeyMapBundle.message("main.toolbar.title");
  public static final String EDITOR_POPUP = KeyMapBundle.message("editor.popup.menu.title");

  public static final String EDITOR_TAB_POPUP = KeyMapBundle.message("editor.tab.popup.menu.title");
  public static final String FAVORITES_POPUP = KeyMapBundle.message("favorites.popup.title");
  public static final String PROJECT_VIEW_POPUP = KeyMapBundle.message("project.view.popup.menu.title");
  public static final String COMMANDER_POPUP = KeyMapBundle.message("commender.view.popup.menu.title");
  public static final String J2EE_POPUP = KeyMapBundle.message("j2ee.view.popup.menu.title");

  @NonNls
  private static final String EDITOR_PREFIX = "Editor";
  @NonNls
  private static final String TOOL_ACTION_PREFIX = "Tool_";

  private ActionsTreeUtil() {
  }

  private static KeymapGroupImpl createPluginsActionsGroup(Condition<AnAction> filtered) {
    KeymapGroupImpl pluginsGroup = new KeymapGroupImpl(KeyMapBundle.message("plugins.group.title"), null, null);
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    ActionManagerEx managerEx = ActionManagerEx.getInstanceEx();

    List<PluginId> collected = new ArrayList<PluginId>();
    PluginManager.forEachEnabledPlugin(plugin -> {
      collected.add(plugin.getPluginId());
      KeymapGroupImpl pluginGroup;
      if (plugin.getPluginId().equals(PluginManagerCore.CORE_PLUGIN)) {
        return;
      }
      else {
        pluginGroup = new KeymapGroupImpl(plugin.getName(), null, null);
      }
      final String[] pluginActions = managerEx.getPluginActions(plugin.getPluginId());
      if (pluginActions == null || pluginActions.length == 0) {
        return;
      }
      Arrays.sort(pluginActions, (o1, o2) -> getTextToCompare(o1).compareTo(getTextToCompare(o2)));
      for (String pluginAction : pluginActions) {
        if (keymapManager.getBoundActions().contains(pluginAction)) continue;
        final AnAction anAction = managerEx.getActionOrStub(pluginAction);
        if (filtered == null || filtered.value(anAction)) {
          pluginGroup.addActionId(pluginAction);
        }
      }
      if (pluginGroup.getSize() > 0) {
        pluginsGroup.addGroup(pluginGroup);
      }
    });

    for (PluginId pluginId : PluginId.getRegisteredIds().values()) {
      if (collected.contains(pluginId)) continue;
      KeymapGroupImpl pluginGroup = new KeymapGroupImpl(pluginId.getIdString(), null, null);
      final String[] pluginActions = managerEx.getPluginActions(pluginId);
      if (pluginActions == null || pluginActions.length == 0) {
        continue;
      }
      for (String pluginAction : pluginActions) {
        if (keymapManager.getBoundActions().contains(pluginAction)) continue;
        final AnAction anAction = managerEx.getActionOrStub(pluginAction);
        if (filtered == null || filtered.value(anAction)) {
          pluginGroup.addActionId(pluginAction);
        }
      }
      if (pluginGroup.getSize() > 0) {
        pluginsGroup.addGroup(pluginGroup);
      }
    }

    return pluginsGroup;
  }

  private static KeymapGroupImpl createMainMenuGroup(Condition<AnAction> filtered) {
    KeymapGroupImpl group = new KeymapGroupImpl(MAIN_MENU_TITLE, IdeActions.GROUP_MAIN_MENU, AllIcons.Nodes.KeymapMainMenu);
    ActionGroup mainMenuGroup = (ActionGroup)ActionManager.getInstance().getActionOrStub(IdeActions.GROUP_MAIN_MENU);
    fillGroupIgnorePopupFlag(mainMenuGroup, group, filtered);
    return group;
  }

  @Nullable
  private static Condition<AnAction> wrapFilter(@Nullable final Condition<AnAction> filter, final Keymap keymap, final ActionManager actionManager) {
    if (Registry.is("keymap.show.alias.actions")) return filter;

    return new Condition<AnAction>() {
      @Override
      public boolean value(final AnAction action) {
        if (action == null) return false;
        final String id = action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : actionManager.getId(action);
        if (id != null) {
          boolean actionBound = isActionBound(keymap, id);
          return filter == null ? !actionBound : !actionBound && filter.value(action);
        }

        return filter == null ? true : filter.value(action);
      }
    };
  }

  private static void fillGroupIgnorePopupFlag(ActionGroup actionGroup, KeymapGroupImpl group, Condition<AnAction> filtered) {
    AnAction[] mainMenuTopGroups = actionGroup instanceof DefaultActionGroup ? ((DefaultActionGroup)actionGroup).getChildActionsOrStubs() : actionGroup.getChildren(null);
    for (AnAction action : mainMenuTopGroups) {
      if (!(action instanceof ActionGroup)) continue;
      KeymapGroupImpl subGroup = createGroup((ActionGroup)action, false, filtered);
      if (subGroup.getSize() > 0) {
        group.addGroup(subGroup);
      }
    }
  }

  public static KeymapGroupImpl createGroup(ActionGroup actionGroup, boolean ignore, Predicate<AnAction> filtered) {
    return createGroup(actionGroup, getName(actionGroup), null, null, ignore, filtered);
  }

  private static String getName(AnAction action) {
    final String name = action.getTemplatePresentation().getText();
    if (name != null && !name.isEmpty()) {
      return name;
    }
    else {
      final String id = action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : ActionManager.getInstance().getId(action);
      if (id != null) {
        return id;
      }
      if (action instanceof DefaultActionGroup) {
        final DefaultActionGroup group = (DefaultActionGroup)action;
        if (group.getChildrenCount() == 0) return "Empty group";
        final AnAction[] children = group.getChildActionsOrStubs();
        for (AnAction child : children) {
          if (!(child instanceof AnSeparator)) {
            return "group." + getName(child);
          }
        }
        return "Empty unnamed group";
      }
      return action.getClass().getName();
    }
  }

  public static KeymapGroupImpl createGroup(ActionGroup actionGroup, String groupName, Image icon, Image openIcon, boolean ignore, Predicate<AnAction> filtered) {
    return createGroup(actionGroup, groupName, icon, openIcon, ignore, filtered, true);
  }

  public static KeymapGroupImpl createGroup(ActionGroup actionGroup, String groupName, Image icon, Image openIcon, boolean ignore, Predicate<AnAction> filtered, boolean normalizeSeparators) {
    ActionManager actionManager = ActionManager.getInstance();
    KeymapGroupImpl group = new KeymapGroupImpl(groupName, actionManager.getId(actionGroup), icon);
    AnAction[] children = actionGroup instanceof DefaultActionGroup ? ((DefaultActionGroup)actionGroup).getChildActionsOrStubs() : actionGroup.getChildren(null);

    for (int i = 0; i < children.length; i++) {
      AnAction action = children[i];
      LOG.assertTrue(action != null, groupName + " contains null actions");
      if (action instanceof ActionGroup) {
        KeymapGroupImpl subGroup = createGroup((ActionGroup)action, getName(action), null, null, ignore, filtered, normalizeSeparators);
        if (subGroup.getSize() > 0) {
          if (!ignore && !((ActionGroup)action).isPopup()) {
            group.addAll(subGroup);
          }
          else {
            group.addGroup(subGroup);
          }
        }
        else if (filtered == null || filtered.test(actionGroup)) {
          group.addGroup(subGroup);
        }
      }
      else if (action instanceof AnSeparator) {
        group.addSeparator();
      }
      else if (action != null) {
        String id = action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : actionManager.getId(action);
        if (id != null) {
          if (id.startsWith(TOOL_ACTION_PREFIX)) continue;
          if (filtered == null || filtered.test(action)) {
            group.addActionId(id);
          }
        }
      }
    }
    if (normalizeSeparators) group.normalizeSeparators();
    return group;
  }

  private static KeymapGroupImpl createEditorActionsGroup(Condition<AnAction> filtered) {
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup editorGroup = (DefaultActionGroup)actionManager.getActionOrStub(IdeActions.GROUP_EDITOR);
    ArrayList<String> ids = new ArrayList<String>();

    addEditorActions(filtered, editorGroup, ids);

    Collections.sort(ids);
    KeymapGroupImpl group = new KeymapGroupImpl(KeyMapBundle.message("editor.actions.group.title"), IdeActions.GROUP_EDITOR, AllIcons.Nodes.KeymapEditor);
    for (String id : ids) {
      group.addActionId(id);
    }

    return group;
  }

  private static boolean isActionBound(final Keymap keymap, final String id) {
    if (keymap == null) return false;
    Keymap parent = keymap.getParent();
    return ((KeymapImpl)keymap).isActionBound(id) || (parent != null && ((KeymapImpl)parent).isActionBound(id));
  }

  private static void addEditorActions(final Condition<AnAction> filtered, final DefaultActionGroup editorGroup, final ArrayList<String> ids) {
    AnAction[] editorActions = editorGroup.getChildActionsOrStubs();
    final ActionManager actionManager = ActionManager.getInstance();
    for (AnAction editorAction : editorActions) {
      if (editorAction instanceof DefaultActionGroup) {
        addEditorActions(filtered, (DefaultActionGroup)editorAction, ids);
      }
      else {
        String actionId = editorAction instanceof ActionStubBase ? ((ActionStubBase)editorAction).getId() : actionManager.getId(editorAction);
        if (actionId == null) continue;
        if (actionId.startsWith(EDITOR_PREFIX)) {
          AnAction action = actionManager.getActionOrStub('$' + actionId.substring(6));
          if (action != null) continue;
        }
        if (filtered == null || filtered.value(editorAction)) {
          ids.add(actionId);
        }
      }
    }
  }

  private static KeymapGroupImpl createExtensionGroup(Condition<AnAction> filtered, final Project project, KeymapExtension provider) {
    return (KeymapGroupImpl)provider.createGroup(filtered, project);
  }

  private static KeymapGroupImpl createMacrosGroup(Condition<AnAction> filtered) {
    final ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    String[] ids = actionManager.getActionIds(ActionMacro.MACRO_ACTION_PREFIX);
    Arrays.sort(ids);
    KeymapGroupImpl group = new KeymapGroupImpl(KeyMapBundle.message("macros.group.title"), null, null);
    for (String id : ids) {
      if (filtered == null || filtered.value(actionManager.getActionOrStub(id))) {
        group.addActionId(id);
      }
    }
    return group;
  }

  private static KeymapGroupImpl createQuickListsGroup(final Condition<AnAction> filtered, final String filter, final boolean forceFiltering, final QuickList[] quickLists) {
    Arrays.sort(quickLists, new Comparator<QuickList>() {
      public int compare(QuickList l1, QuickList l2) {
        return l1.getActionId().compareTo(l2.getActionId());
      }
    });

    KeymapGroupImpl group = new KeymapGroupImpl(KeyMapBundle.message("quick.lists.group.title"), null, null);
    for (QuickList quickList : quickLists) {
      if (filtered != null && filtered.value(ActionManagerEx.getInstanceEx().getAction(quickList.getActionId()))) {
        group.addQuickList(quickList);
      }
      else if (SearchUtil.isComponentHighlighted(quickList.getDisplayName(), filter, forceFiltering, null)) {
        group.addQuickList(quickList);
      }
      else if (filtered == null && StringUtil.isEmpty(filter)) {
        group.addQuickList(quickList);
      }
    }
    return group;
  }


  private static KeymapGroupImpl createOtherGroup(Condition<AnAction> filtered, KeymapGroupImpl addedActions, final Keymap keymap) {
    addedActions.initIds();
    ArrayList<String> result = new ArrayList<String>();

    if (keymap != null) {
      String[] actionIds = keymap.getActionIds();
      for (String id : actionIds) {
        if (id.startsWith(EDITOR_PREFIX)) {
          AnAction action = ActionManager.getInstance().getActionOrStub("$" + id.substring(6));
          if (action != null) continue;
        }

        if (!id.startsWith(QuickList.QUICK_LIST_PREFIX) && !addedActions.containsId(id)) {
          result.add(id);
        }
      }
    }

    // add all registered actions
    final ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    String[] registeredActionIds = actionManager.getActionIds("");
    for (String id : registeredActionIds) {
      if (actionManager.getActionOrStub(id) instanceof ActionGroup) {
        continue;
      }
      if (id.startsWith(QuickList.QUICK_LIST_PREFIX) || addedActions.containsId(id) || result.contains(id)) {
        continue;
      }

      if (keymapManager.getBoundActions().contains(id)) continue;

      result.add(id);
    }

    filterOtherActionsGroup(result);

    ContainerUtil.quickSort(result, new Comparator<String>() {
      public int compare(String id1, String id2) {
        return getTextToCompare(id1).compareToIgnoreCase(getTextToCompare(id2));
      }
    });

    KeymapGroupImpl group = new KeymapGroupImpl(KeyMapBundle.message("other.group.title"), AllIcons.Nodes.KeymapOther);
    for (String id : result) {
      if (filtered == null || filtered.value(actionManager.getActionOrStub(id))) group.addActionId(id);
    }
    return group;
  }

  private static String getTextToCompare(String id) {
    AnAction action = ActionManager.getInstance().getActionOrStub(id);
    if (action == null) {
      return id;
    }
    String text = action.getTemplatePresentation().getText();
    return text != null ? text : id;
  }

  private static void filterOtherActionsGroup(ArrayList<String> actions) {
    filterOutGroup(actions, IdeActions.GROUP_GENERATE);
    filterOutGroup(actions, IdeActions.GROUP_NEW);
    filterOutGroup(actions, IdeActions.GROUP_CHANGE_SCHEME);
  }

  private static void filterOutGroup(ArrayList<String> actions, String groupId) {
    if (groupId == null) {
      throw new IllegalArgumentException();
    }
    ActionManager actionManager = ActionManager.getInstance();
    AnAction action = actionManager.getActionOrStub(groupId);
    if (action instanceof DefaultActionGroup) {
      DefaultActionGroup group = (DefaultActionGroup)action;
      AnAction[] children = group.getChildActionsOrStubs();
      for (AnAction child : children) {
        String childId = child instanceof ActionStubBase ? ((ActionStubBase)child).getId() : actionManager.getId(child);
        if (childId == null) {
          // SCR 35149
          continue;
        }
        if (child instanceof DefaultActionGroup) {
          filterOutGroup(actions, childId);
        }
        else {
          actions.remove(childId);
        }
      }
    }
  }

  public static DefaultMutableTreeNode createNode(KeymapGroupImpl group) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(group);
    for (Object child : group.getChildren()) {
      if (child instanceof KeymapGroupImpl) {
        DefaultMutableTreeNode childNode = createNode((KeymapGroupImpl)child);
        node.add(childNode);
      }
      else {
        node.add(new DefaultMutableTreeNode(child));
      }
    }
    return node;
  }

  public static KeymapGroupImpl createMainGroup(final Project project, final Keymap keymap, final QuickList[] quickLists) {
    return createMainGroup(project, keymap, quickLists, null, false, null);
  }

  public static KeymapGroupImpl createMainGroup(final Project project, final Keymap keymap, final QuickList[] quickLists, final String filter, final boolean forceFiltering, final Condition<AnAction> filtered) {
    final Condition<AnAction> wrappedFilter = wrapFilter(filtered, keymap, ActionManager.getInstance());
    KeymapGroupImpl mainGroup = new KeymapGroupImpl(KeyMapBundle.message("all.actions.group.title"), null, null);
    mainGroup.addGroup(createEditorActionsGroup(wrappedFilter));
    mainGroup.addGroup(createMainMenuGroup(wrappedFilter));
    for (KeymapExtension extension : KeymapExtension.EXTENSION_POINT_NAME.getExtensionList()) {
      final KeymapGroupImpl group = createExtensionGroup(wrappedFilter, project, extension);
      if (group != null) {
        mainGroup.addGroup(group);
      }
    }
    mainGroup.addGroup(createMacrosGroup(wrappedFilter));
    mainGroup.addGroup(createQuickListsGroup(wrappedFilter, filter, forceFiltering, quickLists));
    mainGroup.addGroup(createPluginsActionsGroup(wrappedFilter));
    mainGroup.addGroup(createOtherGroup(wrappedFilter, mainGroup, keymap));
    if (!StringUtil.isEmpty(filter) || filtered != null) {
      final ArrayList list = mainGroup.getChildren();
      for (Iterator i = list.iterator(); i.hasNext(); ) {
        final Object o = i.next();
        if (o instanceof KeymapGroupImpl) {
          final KeymapGroupImpl group = (KeymapGroupImpl)o;
          if (group.getSize() == 0) {
            if (!SearchUtil.isComponentHighlighted(group.getName(), filter, forceFiltering, null)) {
              i.remove();
            }
          }
        }
      }
    }
    return mainGroup;
  }

  public static Condition<AnAction> isActionFiltered(final String filter, final boolean force) {
    return new Condition<AnAction>() {
      public boolean value(final AnAction action) {
        if (filter == null) return true;
        if (action == null) return false;
        final String insensitiveFilter = filter.toLowerCase();
        final String text = action.getTemplatePresentation().getText();
        if (text != null) {
          final String lowerText = text.toLowerCase();
          if (SearchUtil.isComponentHighlighted(lowerText, insensitiveFilter, force, null)) {
            return true;
          }
          else if (lowerText.indexOf(insensitiveFilter) != -1) {
            return true;
          }
        }
        final String description = action.getTemplatePresentation().getDescription();
        if (description != null) {
          final String insensitiveDescription = description.toLowerCase();
          if (SearchUtil.isComponentHighlighted(insensitiveDescription, insensitiveFilter, force, null)) {
            return true;
          }
          else if (insensitiveDescription.indexOf(insensitiveFilter) != -1) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static Condition<AnAction> isActionFiltered(final ActionManager actionManager, final Keymap keymap, final KeyboardShortcut keyboardShortcut) {
    return new Condition<AnAction>() {
      public boolean value(final AnAction action) {
        if (keyboardShortcut == null) return true;
        if (action == null) return false;
        final Shortcut[] actionShortcuts = keymap.getShortcuts(action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : actionManager.getId(action));
        for (Shortcut shortcut : actionShortcuts) {
          if (shortcut instanceof KeyboardShortcut) {
            final KeyboardShortcut keyboardActionShortcut = (KeyboardShortcut)shortcut;
            if (Comparing.equal(keyboardActionShortcut, keyboardShortcut)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  public static Condition<AnAction> isActionFiltered(final ActionManager actionManager, final Keymap keymap, final Condition<? super Shortcut> predicat) {
    return action -> {
      if (action == null) return false;
      final Shortcut[] actionShortcuts = keymap.getShortcuts(action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : actionManager.getId(action));
      for (Shortcut actionShortcut : actionShortcuts) {
        if (predicat.value(actionShortcut)) return true;
      }
      return false;
    };
  }
}
