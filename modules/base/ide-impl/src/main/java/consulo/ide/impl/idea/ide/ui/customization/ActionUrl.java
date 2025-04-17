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

import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;

/**
 * User: anna
 * Date: Mar 18, 2005
 */
public class ActionUrl implements JDOMExternalizable {
    public static final int ADDED = 1;
    public static final int DELETED = -1;

    //temp action only
    public static final int MOVE = 2;

    private ArrayList<String> myGroupPath;
    private Object myComponent;
    private int myActionType;
    private int myAbsolutePosition;


    public int myInitialPosition = -1;

    @NonNls
    private static final String IS_GROUP = "is_group";
    @NonNls
    private static final String SEPARATOR = "seperator";
    @NonNls
    private static final String IS_ACTION = "is_action";
    @NonNls
    private static final String VALUE = "value";
    @NonNls
    private static final String PATH = "path";
    @NonNls
    private static final String ACTION_TYPE = "action_type";
    @NonNls
    private static final String POSITION = "position";


    public ActionUrl() {
        myGroupPath = new ArrayList<>();
    }

    public ActionUrl(final ArrayList<String> groupPath, final Object component, final int actionType, final int position) {
        myGroupPath = groupPath;
        myComponent = component;
        myActionType = actionType;
        myAbsolutePosition = position;
    }

    public ArrayList<String> getGroupPath() {
        return myGroupPath;
    }

    public String getParentGroup() {
        return myGroupPath.get(myGroupPath.size() - 1);
    }

    public Object getComponent() {
        return myComponent;
    }

    @Nullable
    public AnAction getComponentAction() {
        if (myComponent instanceof AnSeparator) {
            return AnSeparator.getInstance();
        }
        if (myComponent instanceof String actionId) {
            return ActionManager.getInstance().getAction(actionId);
        }
        if (myComponent instanceof KeymapGroupImpl keymapGroup) {
            final String id = keymapGroup.getId();
            if (id == null || id.length() == 0) {
                return keymapGroup.constructActionGroup(true);
            }
            return ActionManager.getInstance().getAction(id);
        }
        return null;
    }

    public int getActionType() {
        return myActionType;
    }

    public void setActionType(final int actionType) {
        myActionType = actionType;
    }

    public int getAbsolutePosition() {
        return myAbsolutePosition;
    }

    public void setAbsolutePosition(final int absolutePosition) {
        myAbsolutePosition = absolutePosition;
    }

    public int getInitialPosition() {
        return myInitialPosition;
    }

    public void setInitialPosition(final int initialPosition) {
        myInitialPosition = initialPosition;
    }

    public void readExternal(Element element) throws InvalidDataException {
        myGroupPath = new ArrayList<>();
        for (Object o : element.getChildren(PATH)) {
            myGroupPath.add(((Element)o).getAttributeValue(VALUE));
        }
        final String attributeValue = element.getAttributeValue(VALUE);
        if (element.getAttributeValue(IS_ACTION) != null) {
            myComponent = attributeValue;
        }
        else if (element.getAttributeValue(SEPARATOR) != null) {
            myComponent = AnSeparator.getInstance();
        }
        else if (element.getAttributeValue(IS_GROUP) != null) {
            final AnAction action = ActionManager.getInstance().getAction(attributeValue);
            myComponent = action instanceof ActionGroup actionGroup
                ? ActionsTreeUtil.createGroup(actionGroup, true, null)
                : new KeymapGroupImpl(attributeValue, attributeValue, null);
        }
        myActionType = Integer.parseInt(element.getAttributeValue(ACTION_TYPE));
        myAbsolutePosition = Integer.parseInt(element.getAttributeValue(POSITION));
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        for (String s : myGroupPath) {
            Element path = new Element(PATH);
            path.setAttribute(VALUE, s);
            element.addContent(path);
        }
        if (myComponent instanceof String value) {
            element.setAttribute(VALUE, value);
            element.setAttribute(IS_ACTION, Boolean.TRUE.toString());
        }
        else if (myComponent instanceof AnSeparator) {
            element.setAttribute(SEPARATOR, Boolean.TRUE.toString());
        }
        else if (myComponent instanceof KeymapGroupImpl keymapGroup) {
            final String groupId = keymapGroup.getId() != null && keymapGroup.getId().length() != 0
                ? keymapGroup.getId() : keymapGroup.getName();
            element.setAttribute(VALUE, groupId != null ? groupId : "");
            element.setAttribute(IS_GROUP, Boolean.TRUE.toString());
        }
        element.setAttribute(ACTION_TYPE, Integer.toString(myActionType));
        element.setAttribute(POSITION, Integer.toString(myAbsolutePosition));
        DefaultJDOMExternalizer.writeExternal(this, element);
    }

    public boolean isGroupContainsInPath(ActionGroup group) {
        for (String s : myGroupPath) {
            if (s.equals(group.getTemplatePresentation().getText())) {
                return true;
            }
        }
        return false;
    }

    public static void changePathInActionsTree(JTree tree, ActionUrl url) {
        if (url.myActionType == ADDED) {
            addPathToActionsTree(tree, url);
        }
        else if (url.myActionType == DELETED) {
            removePathFromActionsTree(tree, url);
        }
        else if (url.myActionType == MOVE) {
            movePathInActionsTree(tree, url);
        }
    }

    private static void addPathToActionsTree(JTree tree, ActionUrl url) {
        final TreePath treePath = CustomizationUtil.getTreePath(tree, url);
        if (treePath == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        final int absolutePosition = url.getAbsolutePosition();
        if (node.getChildCount() >= absolutePosition && absolutePosition >= 0) {
            if (url.getComponent() instanceof KeymapGroupImpl keymapGroup) {
                node.insert(ActionsTreeUtil.createNode(keymapGroup), absolutePosition);
            }
            else {
                node.insert(new DefaultMutableTreeNode(url.getComponent()), absolutePosition);
            }
        }
    }

    private static void removePathFromActionsTree(JTree tree, ActionUrl url) {
        if (url.myComponent == null) {
            return;
        }
        final TreePath treePath = CustomizationUtil.getTreePath(tree, url);
        if (treePath == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        final int absolutePosition = url.getAbsolutePosition();
        if (node.getChildCount() > absolutePosition && absolutePosition >= 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(absolutePosition);
            if (child.getUserObject().equals(url.getComponent())) {
                node.remove(child);
            }
        }
    }

    private static void movePathInActionsTree(JTree tree, ActionUrl url) {
        final TreePath treePath = CustomizationUtil.getTreePath(tree, url);
        if (treePath != null) {
            if (treePath.getLastPathComponent() != null) {
                final DefaultMutableTreeNode parent = ((DefaultMutableTreeNode)treePath.getLastPathComponent());
                final int absolutePosition = url.getAbsolutePosition();
                final int initialPosition = url.getInitialPosition();
                if (parent.getChildCount() > absolutePosition && absolutePosition >= 0) {
                    if (parent.getChildCount() > initialPosition && initialPosition >= 0) {
                        final DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(initialPosition);
                        if (child.getUserObject().equals(url.getComponent())) {
                            parent.remove(child);
                            parent.insert(child, absolutePosition);
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<String> getGroupPath(final TreePath treePath) {
        final ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < treePath.getPath().length - 1; i++) {
            Object o = ((DefaultMutableTreeNode)treePath.getPath()[i]).getUserObject();
            if (o instanceof KeymapGroupImpl keymapGroup) {
                result.add(keymapGroup.getName());
            }
        }
        return result;
    }

    public boolean equals(Object object) {
        if (!(object instanceof ActionUrl)) {
            return false;
        }
        ActionUrl url = (ActionUrl)object;
        Object comp = myComponent instanceof Pair pair ? pair.first : myComponent;
        Object thatComp = url.myComponent instanceof Pair urlPair ? urlPair.first : url.myComponent;
        return Comparing.equal(comp, thatComp) && myGroupPath.equals(url.myGroupPath) && myAbsolutePosition == url.getAbsolutePosition();
    }

    public int hashCode() {
        int result = myComponent != null ? myComponent.hashCode() : 0;
        result += 29 * myGroupPath.hashCode();
        return result;
    }

    public void setComponent(final Object object) {
        myComponent = object;
    }

    public void setGroupPath(final ArrayList<String> groupPath) {
        myGroupPath = groupPath;
    }
}
