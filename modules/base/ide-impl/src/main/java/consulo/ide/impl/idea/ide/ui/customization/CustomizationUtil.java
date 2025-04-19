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
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

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


    static AnAction[] getReordableChildren(ActionGroup group, CustomActionsSchemaImpl schema, String defaultGroupName, AnActionEvent e) {
        String text = group.getTemplatePresentation().getText();
        ActionManager actionManager = ActionManager.getInstance();
        ArrayList<AnAction> reorderedChildren = new ArrayList<>();
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
                        if (anAction.getTemplatePresentation().getText() == null
                            ? (componentAction.getTemplatePresentation().getText() != null
                            && componentAction.getTemplatePresentation().getText().length() > 0)
                            : !anAction.getTemplatePresentation().getText().equals(componentAction.getTemplatePresentation().getText())) {
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

    public static void optimizeSchema(JTree tree, CustomActionsSchemaImpl schema) {
        //noinspection HardCodedStringLiteral
        KeymapGroupImpl rootGroup = new KeymapGroupImpl("root");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootGroup);
        root.removeAllChildren();
        schema.fillActionGroups(root);
        JTree defaultTree = new Tree(new DefaultTreeModel(root));

        ArrayList<ActionUrl> actions = new ArrayList<>();

        TreeUtil.traverseDepth(
            (TreeNode)tree.getModel().getRoot(),
            node -> {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)node;
                if (treeNode.isLeaf()) {
                    return true;
                }
                ActionUrl url = getActionUrl(new TreePath(treeNode.getPath()), 0);
                url.getGroupPath().add(((KeymapGroupImpl)treeNode.getUserObject()).getName());
                TreePath treePath = getTreePath(defaultTree, url);
                if (treePath != null) {
                    DefaultMutableTreeNode visited = (DefaultMutableTreeNode)treePath.getLastPathComponent();
                    ActionUrl[] defaultUserObjects = getChildUserObjects(visited, url);
                    ActionUrl[] currentUserObjects = getChildUserObjects(treeNode, url);
                    computeDiff(defaultUserObjects, currentUserObjects, actions);
                }
                else {
                    //customizations at the new place
                    url.getGroupPath().remove(url.getParentGroup());
                    if (actions.contains(url)) {
                        url.getGroupPath().add(((KeymapGroupImpl)treeNode.getUserObject()).getName());
                        actions.addAll(schema.getChildActions(url));
                    }
                }
                return true;
            }
        );
        schema.setActions(actions);
    }

    private static void computeDiff(ActionUrl[] defaultUserObjects, ActionUrl[] currentUserObjects, ArrayList<ActionUrl> actions) {
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

    public static TreePath getPathByUserObjects(JTree tree, TreePath treePath) {
        List<String> path = new ArrayList<>();
        for (int i = 0; i < treePath.getPath().length; i++) {
            Object o = ((DefaultMutableTreeNode)treePath.getPath()[i]).getUserObject();
            if (o instanceof KeymapGroupImpl keymapGroup) {
                path.add(keymapGroup.getName());
            }
        }
        return getTreePath(0, path, tree.getModel().getRoot(), tree);
    }

    public static ActionUrl getActionUrl(TreePath treePath, int actionType) {
        ActionUrl url = new ActionUrl();
        for (int i = 0; i < treePath.getPath().length - 1; i++) {
            Object o = ((DefaultMutableTreeNode)treePath.getPath()[i]).getUserObject();
            if (o instanceof KeymapGroupImpl keymapGroup) {
                url.getGroupPath().add(keymapGroup.getName());
            }
        }

        DefaultMutableTreeNode component = ((DefaultMutableTreeNode)treePath.getLastPathComponent());
        url.setComponent(component.getUserObject());
        DefaultMutableTreeNode node = component;
        TreeNode parent = node.getParent();
        url.setAbsolutePosition(parent != null ? parent.getIndex(node) : 0);
        url.setActionType(actionType);
        return url;
    }


    public static TreePath getTreePath(JTree tree, ActionUrl url) {
        return getTreePath(0, url.getGroupPath(), tree.getModel().getRoot(), tree);
    }

    @Nullable
    private static TreePath getTreePath(int positionInPath, List<String> path, Object root, JTree tree) {
        if (!(root instanceof DefaultMutableTreeNode treeNode)) {
            return null;
        }

        Object userObject = treeNode.getUserObject();

        String pathElement;
        if (path.size() > positionInPath) {
            pathElement = path.get(positionInPath);
        }
        else {
            return null;
        }

        if (!(pathElement != null && userObject instanceof KeymapGroupImpl keymapGroup && pathElement.equals(keymapGroup.getName()))) {
            return null;
        }

        TreePath currentPath = new TreePath(treeNode.getPath());

        if (positionInPath == path.size() - 1) {
            return currentPath;
        }

        for (int j = 0; j < treeNode.getChildCount(); j++) {
            TreeNode child = treeNode.getChildAt(j);
            currentPath = getTreePath(positionInPath + 1, path, child, tree);
            if (currentPath != null) {
                break;
            }
        }

        return currentPath;
    }


    private static ActionUrl[] getChildUserObjects(DefaultMutableTreeNode node, ActionUrl parent) {
        ArrayList<ActionUrl> result = new ArrayList<>();
        ArrayList<String> groupPath = new ArrayList<>();
        groupPath.addAll(parent.getGroupPath());
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            ActionUrl url = new ActionUrl();
            url.setGroupPath(groupPath);
            Object userObject = child.getUserObject();
            url.setComponent(userObject instanceof Pair ? ((Pair)userObject).first : userObject);
            result.add(url);
        }
        return result.toArray(new ActionUrl[result.size()]);
    }

    @Deprecated
    public static MouseListener installPopupHandler(JComponent component, @Nonnull String groupId, String place) {
        return PopupHandler.installPopupHandlerFromCustomActions(component, groupId, place);
    }
}
