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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.logging.Logger;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.PopupHandler;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.QuickList;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author anna
 * @since 2005-03-30
 */
public class CustomizationUtil {
    private static final Logger LOG = Logger.getInstance(CustomizationUtil.class);

    private CustomizationUtil() {
    }

    public static ActionGroup correctActionGroup(ActionGroup group, CustomActionsSchemaImpl schema, String defaultGroupName) {
        if (!schema.isCorrectActionGroup(group, defaultGroupName)) {
            return group;
        }
        String text = group.getTemplatePresentation().getText();
        int mnemonic = TextWithMnemonic.parse(group.getTemplatePresentation().getTextWithMnemonic()).getMnemonic();
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                if (Character.toUpperCase(text.charAt(i)) == mnemonic) {
                    text = text.replaceFirst(String.valueOf(text.charAt(i)), "_" + text.charAt(i));
                    break;
                }
            }
        }

        return new CustomisedActionGroup(text, group.isPopup(), group, schema, defaultGroupName);
    }

    static AnAction[] getReorderableChildren(ActionGroup group, CustomActionsSchemaImpl schema, String defaultGroupName, AnActionEvent e) {
        String text = group.getTemplatePresentation().getText();
        ActionManager actionManager = ActionManager.getInstance();
        List<AnAction> reorderedChildren = new ArrayList<>();
        ContainerUtil.addAll(reorderedChildren, group.getChildren(e));
        List<ActionUrl> actions = schema.getActions();
        for (ActionUrl actionUrl : actions) {
            if ((actionUrl.getParentGroup().equals(text) || actionUrl.getParentGroup().equals(defaultGroupName)
                || actionUrl.getParentGroup().equals(actionManager.getId(group)))) {
                AnAction componentAction = actionUrl.getComponentAction();
                if (componentAction != null) {
                    if (actionUrl.getActionType() == ActionUrl.ADDED) {
                        if (componentAction == group) {
                            LOG.error("Attempt to add group to itself; group ID=" + actionManager.getId(group));
                            continue;
                        }
                        if (reorderedChildren.size() > actionUrl.getAbsolutePosition()) {
                            reorderedChildren.add(actionUrl.getAbsolutePosition(), componentAction);
                        }
                        else {
                            reorderedChildren.add(componentAction);
                        }
                    }
                    else if (actionUrl.getActionType() == ActionUrl.DELETED && reorderedChildren.size() > actionUrl.getAbsolutePosition()) {
                        AnAction anAction = reorderedChildren.get(actionUrl.getAbsolutePosition());
                        if (!anAction.getTemplatePresentation().getTextValue().equals(componentAction.getTemplatePresentation().getTextValue())) {
                            continue;
                        }
                        reorderedChildren.remove(actionUrl.getAbsolutePosition());
                    }
                }
            }
        }
        for (int i = 0; i < reorderedChildren.size(); i++) {
            if (reorderedChildren.get(i) instanceof ActionGroup groupToCorrect) {
                AnAction correctedAction = correctActionGroup(groupToCorrect, schema, "");
                reorderedChildren.set(i, correctedAction);
            }
        }

        return reorderedChildren.toArray(new AnAction[reorderedChildren.size()]);
    }

    public static void optimizeSchema(KeymapGroupImpl root, CustomActionsSchemaImpl schema, List<KeymapGroupImpl> defaultGroups) {
        KeymapGroupImpl defaultRoot = new KeymapGroupImpl(root.getName());
        for (KeymapGroupImpl group : defaultGroups) {
            defaultRoot.addGroup(group.copy());
        }

        List<ActionUrl> actions = new ArrayList<>();
        optimizeGroup(root, defaultRoot, schema, actions);
        schema.setActions(actions);
    }

    private static void optimizeGroup(KeymapGroupImpl group, KeymapGroupImpl defaultRoot, CustomActionsSchemaImpl schema, List<ActionUrl> actions) {
        if (group.getSize() == 0) {
            return;
        }

        List<String> groupPath = getGroupPath(group);
        KeymapGroupImpl defaultGroup = findGroup(defaultRoot, groupPath);
        if (defaultGroup != null) {
            ActionUrl[] defaultUserObjects = getChildUserObjects(defaultGroup, groupPath);
            ActionUrl[] currentUserObjects = getChildUserObjects(group, groupPath);
            computeDiff(defaultUserObjects, currentUserObjects, actions);
        }
        else {
            KeymapGroupImpl parent = group.getParent();
            List<String> parentPath = new ArrayList<>(groupPath.subList(0, groupPath.size() - 1));
            int position = parent == null ? 0 : indexOf(parent, group);
            ActionUrl url = new ActionUrl(parentPath, group, 0, position);
            if (actions.contains(url)) {
                actions.addAll(schema.getChildActions(new ActionUrl(new ArrayList<>(groupPath), group, 0, position)));
            }
        }

        for (Object child : List.copyOf(group.getChildren())) {
            if (child instanceof KeymapGroupImpl childGroup) {
                optimizeGroup(childGroup, defaultRoot, schema, actions);
            }
        }
    }

    private static int indexOf(KeymapGroupImpl parent, Object child) {
        List<Object> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == child) {
                return i;
            }
        }
        return 0;
    }

    private static void computeDiff(ActionUrl[] defaultUserObjects, ActionUrl[] currentUserObjects, List<ActionUrl> actions) {
        Diff.Change change = null;
        try {
            change = Diff.buildChanges(defaultUserObjects, currentUserObjects);
        }
        catch (FilesTooBigForDiffException e) {
            LOG.info(e);
        }
        while (change != null) {
            for (int i = 0; i < change.deleted; i++) {
                int idx = change.line0 + i;
                ActionUrl currentUserObject = defaultUserObjects[idx];
                currentUserObject.setActionType(ActionUrl.DELETED);
                currentUserObject.setAbsolutePosition(idx);
                actions.add(currentUserObject);
            }
            for (int i = 0; i < change.inserted; i++) {
                int idx = change.line1 + i;
                ActionUrl currentUserObject = currentUserObjects[idx];
                currentUserObject.setActionType(ActionUrl.ADDED);
                currentUserObject.setAbsolutePosition(idx);
                actions.add(currentUserObject);
            }
            change = change.link;
        }
    }

    /**
     * Names of the groups from the root down to {@code group}, both included - the path an {@link ActionUrl} addresses
     * the children of {@code group} by.
     */
    public static List<String> getGroupPath(KeymapGroupImpl group) {
        List<String> result = new ArrayList<>();
        KeymapGroupImpl current = group;
        while (current != null) {
            result.add(0, current.getName());
            current = current.getParent();
        }
        return result;
    }

    public static ActionUrl getActionUrl(CustomizationTreeItem item, int actionType) {
        return new ActionUrl(new ArrayList<>(item.getGroupPath()), item.getComponent(), actionType, item.getIndex());
    }

    public static @Nullable KeymapGroupImpl findGroup(KeymapGroupImpl root, List<String> path) {
        return findGroup(0, path, root);
    }

    private static @Nullable KeymapGroupImpl findGroup(int positionInPath, List<String> path, KeymapGroupImpl group) {
        if (path.size() <= positionInPath) {
            return null;
        }

        String pathElement = path.get(positionInPath);
        if (pathElement == null || !pathElement.equals(group.getName())) {
            return null;
        }

        if (positionInPath == path.size() - 1) {
            return group;
        }

        for (Object child : group.getChildren()) {
            if (child instanceof KeymapGroupImpl childGroup) {
                KeymapGroupImpl found = findGroup(positionInPath + 1, path, childGroup);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static ActionUrl[] getChildUserObjects(KeymapGroupImpl group, List<String> groupPath) {
        List<ActionUrl> result = new ArrayList<>();
        List<String> path = new ArrayList<>(groupPath);
        for (Object child : group.getChildren()) {
            ActionUrl url = new ActionUrl();
            url.setGroupPath(path);
            url.setComponent(child);
            result.add(url);
        }
        return result.toArray(new ActionUrl[result.size()]);
    }

    public static String getText(Object component) {
        if (component instanceof KeymapGroupImpl group) {
            String name = group.getName();
            return StringUtil.isNotEmpty(name) ? name : StringUtil.notNullize(group.getId());
        }
        if (component instanceof String actionId) {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            String name = action != null ? action.getTemplatePresentation().getText() : null;
            return !StringUtil.isEmptyOrSpaces(name) ? name : actionId;
        }
        if (component instanceof QuickList quickList) {
            return quickList.getDisplayName();
        }
        return "";
    }

    /**
     * The first item, depth first, the predicate takes.
     */
    public static @Nullable CustomizationTreeItem findItem(KeymapGroupImpl group, Predicate<CustomizationTreeItem> predicate) {
        for (CustomizationTreeItem item : CustomizationTreeItem.children(group)) {
            if (predicate.test(item)) {
                return item;
            }
            KeymapGroupImpl childGroup = item.getGroup();
            if (childGroup != null) {
                CustomizationTreeItem found = findItem(childGroup, predicate);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Opens the groups down to the item and selects it.
     */
    @RequiredUIAccess
    public static CompletableFuture<?> selectItem(Tree<CustomizationTreeItem> tree, CustomizationTreeItem item) {
        TreeNode<CustomizationTreeItem> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<KeymapGroupImpl> groups = new ArrayList<>();
        KeymapGroupImpl current = item.getParent();
        while (current != null && current.getParent() != null) {
            groups.add(0, current);
            current = current.getParent();
        }

        CompletableFuture<TreeNode<CustomizationTreeItem>> chain = CompletableFuture.completedFuture(rootNode);
        for (KeymapGroupImpl group : groups) {
            chain = chain.thenCompose(node -> node == null
                ? CompletableFuture.completedFuture(null)
                : node.findChild(value -> value != null && value.getGroup() == group));
        }

        UIAccess uiAccess = UIAccess.current();
        return chain
            .thenCompose(node -> node == null ? CompletableFuture.completedFuture(null) : node.findChild(item::equals))
            .whenCompleteAsync((node, error) -> {
                if (node != null) {
                    tree.select(node);
                }
            }, uiAccess);
    }

    /**
     * Text and icon of a customization tree row.
     *
     * @param iconOf the icon an action shows, the customized one included
     */
    public static void customizePresentation(
        Object component,
        TextItemPresentation presentation,
        boolean showIcon,
        Function<String, @Nullable Image> iconOf
    ) {
        Image icon = null;
        if (component instanceof KeymapGroupImpl group) {
            String name = group.getName();
            presentation.append(StringUtil.isNotEmpty(name) ? name : StringUtil.notNullize(group.getId()));
            icon = group.getId() != null ? iconOf.apply(group.getId()) : null;
            if (icon == null) {
                icon = group.getIcon() != null ? group.getIcon() : PlatformIconGroup.nodesFolder();
            }
        }
        else if (component instanceof String actionId) {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            String name = action != null ? action.getTemplatePresentation().getText() : null;
            presentation.append(!StringUtil.isEmptyOrSpaces(name) ? name : actionId);
            icon = iconOf.apply(actionId);
        }
        else if (component instanceof AnSeparator) {
            presentation.append("-------------");
        }
        else if (component instanceof QuickList quickList) {
            presentation.append(quickList.getDisplayName());
            icon = PlatformIconGroup.actionsQuicklist();
        }
        else {
            presentation.append(String.valueOf(component));
        }

        if (showIcon) {
            presentation.withIcon(icon != null ? icon : Image.empty(Image.DEFAULT_ICON_SIZE));
        }
    }

    @Deprecated
    public static MouseListener installPopupHandler(JComponent component, String groupId, String place) {
        return PopupHandler.installPopupHandlerFromCustomActions(component, groupId, place);
    }
}
