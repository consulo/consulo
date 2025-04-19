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

import consulo.application.util.registry.Registry;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.idea.ide.actionMacro.ActionMacro;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapExtension;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.function.Predicate;

public class ActionsTreeUtil {
    private static final Logger LOG = Logger.getInstance(ActionsTreeUtil.class);

    private static final String EDITOR_PREFIX = "Editor";
    private static final String TOOL_ACTION_PREFIX = "Tool_";

    private ActionsTreeUtil() {
    }

    private static KeymapGroupImpl createPluginsActionsGroup(Predicate<AnAction> filtered) {
        KeymapGroupImpl pluginsGroup = new KeymapGroupImpl(KeyMapLocalize.pluginsGroupTitle());
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        ActionManagerEx managerEx = ActionManagerEx.getInstanceEx();

        List<PluginId> collected = new ArrayList<>();
        PluginManager.forEachEnabledPlugin(plugin -> {
            collected.add(plugin.getPluginId());
            KeymapGroupImpl pluginGroup;
            if (plugin.getPluginId().equals(PluginManagerCore.CORE_PLUGIN)) {
                return;
            }
            else {
                pluginGroup = new KeymapGroupImpl(plugin.getName());
            }
            String[] pluginActions = managerEx.getPluginActions(plugin.getPluginId());
            if (pluginActions == null || pluginActions.length == 0) {
                return;
            }
            Arrays.sort(pluginActions, (o1, o2) -> getTextToCompare(o1).compareTo(getTextToCompare(o2)));
            for (String pluginAction : pluginActions) {
                if (keymapManager.getBoundActions().contains(pluginAction)) {
                    continue;
                }
                AnAction anAction = managerEx.getActionOrStub(pluginAction);
                if (filtered == null || filtered.test(anAction)) {
                    pluginGroup.addActionId(pluginAction);
                }
            }
            if (pluginGroup.getSize() > 0) {
                pluginsGroup.addGroup(pluginGroup);
            }
        });

        for (PluginId pluginId : PluginId.getRegisteredIds().values()) {
            if (collected.contains(pluginId)) {
                continue;
            }
            KeymapGroupImpl pluginGroup = new KeymapGroupImpl(pluginId.getIdString());
            String[] pluginActions = managerEx.getPluginActions(pluginId);
            if (pluginActions == null || pluginActions.length == 0) {
                continue;
            }
            for (String pluginAction : pluginActions) {
                if (keymapManager.getBoundActions().contains(pluginAction)) {
                    continue;
                }
                AnAction anAction = managerEx.getActionOrStub(pluginAction);
                if (filtered == null || filtered.test(anAction)) {
                    pluginGroup.addActionId(pluginAction);
                }
            }
            if (pluginGroup.getSize() > 0) {
                pluginsGroup.addGroup(pluginGroup);
            }
        }

        return pluginsGroup;
    }

    private static KeymapGroupImpl createMainMenuGroup(Predicate<AnAction> filtered) {
        KeymapGroupImpl group = new KeymapGroupImpl(
            KeyMapLocalize.mainMenuActionTitle(),
            IdeActions.GROUP_MAIN_MENU,
            PlatformIconGroup.nodesKeymapmainmenu()
        );
        ActionGroup mainMenuGroup = (ActionGroup)ActionManager.getInstance().getActionOrStub(IdeActions.GROUP_MAIN_MENU);
        fillGroupIgnorePopupFlag(mainMenuGroup, group, filtered);
        return group;
    }

    @Nullable
    private static Predicate<AnAction> wrapFilter(
        @Nullable Predicate<AnAction> filter,
        Keymap keymap,
        ActionManager actionManager
    ) {
        if (Registry.is("keymap.show.alias.actions")) {
            return filter;
        }

        return action -> {
            if (action == null) {
                return false;
            }
            String id = action instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(action);
            if (id != null) {
                boolean actionBound = isActionBound(keymap, id);
                return filter == null ? !actionBound : !actionBound && filter.test(action);
            }

            return filter == null || filter.test(action);
        };
    }

    private static void fillGroupIgnorePopupFlag(ActionGroup actionGroup, KeymapGroupImpl group, Predicate<AnAction> filtered) {
        AnAction[] mainMenuTopGroups = actionGroup instanceof DefaultActionGroup defaultActionGroup
            ? defaultActionGroup.getChildActionsOrStubs() : actionGroup.getChildren(null);
        for (AnAction action : mainMenuTopGroups) {
            if (!(action instanceof ActionGroup topActionGroup)) {
                continue;
            }
            KeymapGroupImpl subGroup = createGroup(topActionGroup, false, filtered);
            if (subGroup.getSize() > 0) {
                group.addGroup(subGroup);
            }
        }
    }

    public static KeymapGroupImpl createGroup(ActionGroup actionGroup, boolean ignore, Predicate<AnAction> filtered) {
        return createGroup(actionGroup, getName(actionGroup), null, null, ignore, filtered);
    }

    private static String getName(AnAction action) {
        String name = action.getTemplatePresentation().getText();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        else {
            String id = action instanceof ActionStubBase actionStubBase
                ? actionStubBase.getId() : ActionManager.getInstance().getId(action);
            if (id != null) {
                return id;
            }
            if (action instanceof DefaultActionGroup group) {
                if (group.getChildrenCount() == 0) {
                    return "Empty group";
                }
                AnAction[] children = group.getChildActionsOrStubs();
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

    public static KeymapGroupImpl createGroup(
        ActionGroup actionGroup,
        String groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered
    ) {
        return createGroup(actionGroup, groupName, icon, openIcon, ignore, filtered, true);
    }

    public static KeymapGroupImpl createGroup(
        ActionGroup actionGroup,
        String groupName,
        Image icon,
        Image openIcon,
        boolean ignore,
        Predicate<AnAction> filtered,
        boolean normalizeSeparators
    ) {
        ActionManager actionManager = ActionManager.getInstance();
        KeymapGroupImpl group = new KeymapGroupImpl(groupName, actionManager.getId(actionGroup), icon);
        AnAction[] children = actionGroup instanceof DefaultActionGroup defaultActionGroup
            ? defaultActionGroup.getChildActionsOrStubs() : actionGroup.getChildren(null);

        for (AnAction action : children) {
            LOG.assertTrue(action != null, groupName + " contains null actions");
            if (action instanceof ActionGroup childActionGroup) {
                KeymapGroupImpl subGroup =
                    createGroup(childActionGroup, getName(action), null, null, ignore, filtered, normalizeSeparators);
                if (subGroup.getSize() > 0) {
                    if (!ignore && !childActionGroup.isPopup()) {
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
                String id = action instanceof ActionStubBase actionStubBase
                    ? actionStubBase.getId() : actionManager.getId(action);
                if (id != null) {
                    if (id.startsWith(TOOL_ACTION_PREFIX)) {
                        continue;
                    }
                    if (filtered == null || filtered.test(action)) {
                        group.addActionId(id);
                    }
                }
            }
        }
        if (normalizeSeparators) {
            group.normalizeSeparators();
        }
        return group;
    }

    private static KeymapGroupImpl createEditorActionsGroup(Predicate<AnAction> filtered) {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup editorGroup = (DefaultActionGroup)actionManager.getActionOrStub(IdeActions.GROUP_EDITOR);
        ArrayList<String> ids = new ArrayList<>();

        addEditorActions(filtered, editorGroup, ids);

        Collections.sort(ids);
        KeymapGroupImpl group = new KeymapGroupImpl(
            KeyMapLocalize.editorActionsGroupTitle(),
            IdeActions.GROUP_EDITOR,
            PlatformIconGroup.nodesKeymapeditor()
        );
        for (String id : ids) {
            group.addActionId(id);
        }

        return group;
    }

    private static boolean isActionBound(Keymap keymap, String id) {
        if (keymap == null) {
            return false;
        }
        Keymap parent = keymap.getParent();
        return ((KeymapImpl)keymap).isActionBound(id) || (parent != null && ((KeymapImpl)parent).isActionBound(id));
    }

    private static void addEditorActions(Predicate<AnAction> filtered, DefaultActionGroup editorGroup, ArrayList<String> ids) {
        AnAction[] editorActions = editorGroup.getChildActionsOrStubs();
        ActionManager actionManager = ActionManager.getInstance();
        for (AnAction editorAction : editorActions) {
            if (editorAction instanceof DefaultActionGroup defaultActionGroup) {
                addEditorActions(filtered, defaultActionGroup, ids);
            }
            else {
                String actionId =
                    editorAction instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(editorAction);
                if (actionId == null) {
                    continue;
                }
                if (actionId.startsWith(EDITOR_PREFIX)) {
                    AnAction action = actionManager.getActionOrStub('$' + actionId.substring(6));
                    if (action != null) {
                        continue;
                    }
                }
                if (filtered == null || filtered.test(editorAction)) {
                    ids.add(actionId);
                }
            }
        }
    }

    private static KeymapGroupImpl createExtensionGroup(Predicate<AnAction> filtered, Project project, KeymapExtension provider) {
        return (KeymapGroupImpl)provider.createGroup(filtered, project);
    }

    private static KeymapGroupImpl createMacrosGroup(Predicate<AnAction> filtered) {
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        String[] ids = actionManager.getActionIds(ActionMacro.MACRO_ACTION_PREFIX);
        Arrays.sort(ids);
        KeymapGroupImpl group = new KeymapGroupImpl(KeyMapLocalize.macrosGroupTitle());
        for (String id : ids) {
            if (filtered == null || filtered.test(actionManager.getActionOrStub(id))) {
                group.addActionId(id);
            }
        }
        return group;
    }

    private static KeymapGroupImpl createQuickListsGroup(
        Predicate<AnAction> filtered,
        String filter,
        boolean forceFiltering,
        QuickList[] quickLists
    ) {
        Arrays.sort(quickLists, (l1, l2) -> l1.getActionId().compareTo(l2.getActionId()));

        KeymapGroupImpl group = new KeymapGroupImpl(KeyMapLocalize.quickListsGroupTitle());
        for (QuickList quickList : quickLists) {
            if (filtered != null && filtered.test(ActionManagerEx.getInstanceEx().getAction(quickList.getActionId()))) {
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

    private static KeymapGroupImpl createOtherGroup(Predicate<AnAction> filtered, KeymapGroupImpl addedActions, Keymap keymap) {
        addedActions.initIds();
        ArrayList<String> result = new ArrayList<>();

        if (keymap != null) {
            String[] actionIds = keymap.getActionIds();
            for (String id : actionIds) {
                if (id.startsWith(EDITOR_PREFIX)) {
                    AnAction action = ActionManager.getInstance().getActionOrStub("$" + id.substring(6));
                    if (action != null) {
                        continue;
                    }
                }

                if (!id.startsWith(QuickList.QUICK_LIST_PREFIX) && !addedActions.containsId(id)) {
                    result.add(id);
                }
            }
        }

        // add all registered actions
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        String[] registeredActionIds = actionManager.getActionIds("");
        for (String id : registeredActionIds) {
            if (actionManager.getActionOrStub(id) instanceof ActionGroup) {
                continue;
            }
            if (id.startsWith(QuickList.QUICK_LIST_PREFIX) || addedActions.containsId(id) || result.contains(id)) {
                continue;
            }

            if (keymapManager.getBoundActions().contains(id)) {
                continue;
            }

            result.add(id);
        }

        filterOtherActionsGroup(result);

        ContainerUtil.quickSort(result, (id1, id2) -> getTextToCompare(id1).compareToIgnoreCase(getTextToCompare(id2)));

        KeymapGroupImpl group = new KeymapGroupImpl(KeyMapLocalize.otherGroupTitle(), PlatformIconGroup.nodesKeymapother());
        for (String id : result) {
            if (filtered == null || filtered.test(actionManager.getActionOrStub(id))) {
                group.addActionId(id);
            }
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
        if (action instanceof DefaultActionGroup group) {
            AnAction[] children = group.getChildActionsOrStubs();
            for (AnAction child : children) {
                String childId = child instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(child);
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
            if (child instanceof KeymapGroupImpl keymapGroup) {
                DefaultMutableTreeNode childNode = createNode(keymapGroup);
                node.add(childNode);
            }
            else {
                node.add(new DefaultMutableTreeNode(child));
            }
        }
        return node;
    }

    public static KeymapGroupImpl createMainGroup(Project project, Keymap keymap, QuickList[] quickLists) {
        return createMainGroup(project, keymap, quickLists, null, false, null);
    }

    public static KeymapGroupImpl createMainGroup(
        Project project,
        Keymap keymap,
        QuickList[] quickLists,
        String filter,
        boolean forceFiltering,
        Predicate<AnAction> filtered
    ) {
        Predicate<AnAction> wrappedFilter = wrapFilter(filtered, keymap, ActionManager.getInstance());
        KeymapGroupImpl mainGroup = new KeymapGroupImpl(KeyMapLocalize.allActionsGroupTitle());
        mainGroup.addGroup(createEditorActionsGroup(wrappedFilter));
        mainGroup.addGroup(createMainMenuGroup(wrappedFilter));
        for (KeymapExtension extension : KeymapExtension.EXTENSION_POINT_NAME.getExtensionList()) {
            KeymapGroupImpl group = createExtensionGroup(wrappedFilter, project, extension);
            if (group != null) {
                mainGroup.addGroup(group);
            }
        }
        mainGroup.addGroup(createMacrosGroup(wrappedFilter));
        mainGroup.addGroup(createQuickListsGroup(wrappedFilter, filter, forceFiltering, quickLists));
        mainGroup.addGroup(createPluginsActionsGroup(wrappedFilter));
        mainGroup.addGroup(createOtherGroup(wrappedFilter, mainGroup, keymap));
        if (!StringUtil.isEmpty(filter) || filtered != null) {
            ArrayList list = mainGroup.getChildren();
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                if (i.next() instanceof KeymapGroupImpl group && group.getSize() == 0
                    && !SearchUtil.isComponentHighlighted(group.getName(), filter, forceFiltering, null)) {
                    i.remove();
                }
            }
        }
        return mainGroup;
    }

    public static Predicate<AnAction> isActionFiltered(String filter, boolean force) {
        return action -> {
            if (filter == null) {
                return true;
            }
            if (action == null) {
                return false;
            }
            String insensitiveFilter = filter.toLowerCase();
            String text = action.getTemplatePresentation().getText();
            if (text != null) {
                String lowerText = text.toLowerCase();
                if (SearchUtil.isComponentHighlighted(lowerText, insensitiveFilter, force, null)) {
                    return true;
                }
                else if (lowerText.contains(insensitiveFilter)) {
                    return true;
                }
            }
            String description = action.getTemplatePresentation().getDescription();
            if (description != null) {
                String insensitiveDescription = description.toLowerCase();
                if (SearchUtil.isComponentHighlighted(insensitiveDescription, insensitiveFilter, force, null)) {
                    return true;
                }
                else if (insensitiveDescription.contains(insensitiveFilter)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<AnAction> isActionFiltered(ActionManager actionManager, Keymap keymap, KeyboardShortcut keyboardShortcut) {
        return action -> {
            if (keyboardShortcut == null) {
                return true;
            }
            if (action == null) {
                return false;
            }
            Shortcut[] actionShortcuts = keymap.getShortcuts(
                action instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(action)
            );
            for (Shortcut shortcut : actionShortcuts) {
                if (shortcut instanceof KeyboardShortcut keyboardActionShortcut
                    && Objects.equals(keyboardActionShortcut, keyboardShortcut)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<AnAction> isActionFiltered(
        ActionManager actionManager,
        Keymap keymap,
        Predicate<? super Shortcut> predicate
    ) {
        return action -> {
            if (action == null) {
                return false;
            }
            Shortcut[] actionShortcuts = keymap.getShortcuts(
                action instanceof ActionStubBase actionStubBase ? actionStubBase.getId() : actionManager.getId(action)
            );
            for (Shortcut actionShortcut : actionShortcuts) {
                if (predicate.test(actionShortcut)) {
                    return true;
                }
            }
            return false;
        };
    }
}
