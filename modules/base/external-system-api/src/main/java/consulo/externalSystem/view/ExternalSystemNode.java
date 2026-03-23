/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.view;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.manage.ExternalProjectsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for all nodes in the external system project tree.
 * Uses lazy-cached children, error-level propagation, and ignore-state rendering.
 *
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemNode<T> extends SimpleNode implements Comparable<ExternalSystemNode<?>> {
    public static final int BUILTIN_TASKS_DATA_NODE_ORDER = 10;
    public static final int BUILTIN_DEPENDENCIES_DATA_NODE_ORDER = BUILTIN_TASKS_DATA_NODE_ORDER + 10;
    public static final int BUILTIN_RUN_CONFIGURATIONS_DATA_NODE_ORDER = BUILTIN_DEPENDENCIES_DATA_NODE_ORDER + 10;
    public static final int BUILTIN_MODULE_DATA_NODE_ORDER = BUILTIN_RUN_CONFIGURATIONS_DATA_NODE_ORDER + 10;

    private static final Comparator<ExternalSystemNode<?>> ORDER_AWARE_COMPARATOR = (o1, o2) -> {
        int order1 = getOrder(o1);
        int order2 = getOrder(o2);
        if (order1 == order2) return o1.compareTo(o2);
        return order1 < order2 ? -1 : 1;
    };

    private static int getOrder(ExternalSystemNode<?> o) {
        consulo.externalSystem.util.Order annotation = o.getClass().getAnnotation(consulo.externalSystem.util.Order.class);
        return annotation != null ? annotation.value() : 0;
    }

    protected static final ExternalSystemNode<?>[] NO_CHILDREN = new ExternalSystemNode[0];
    private static final List<ExternalSystemNode<?>> NO_CHILDREN_LIST = Collections.emptyList();

    private final ExternalProjectsView myExternalProjectsView;
    private List<ExternalSystemNode<?>> myChildrenList = NO_CHILDREN_LIST;
    DataNode<T> myDataNode;
    private @Nullable ExternalSystemNode<?> myParent;
    private ExternalSystemNode<?>[] myChildren;
    private ExternalProjectsStructure.ErrorLevel myErrorLevel = ExternalProjectsStructure.ErrorLevel.NONE;
    private final List<String> myErrors = new ArrayList<>();
    private ExternalProjectsStructure.ErrorLevel myTotalErrorLevel = null;

    public ExternalSystemNode(ExternalProjectsView externalProjectsView,
                              @Nullable ExternalSystemNode<?> parent) {
        this(externalProjectsView, parent, null);
    }

    public ExternalSystemNode(ExternalProjectsView externalProjectsView,
                              @Nullable ExternalSystemNode<?> parent,
                              @Nullable DataNode<T> dataNode) {
        super(externalProjectsView.getProject(), null);
        myExternalProjectsView = externalProjectsView;
        myDataNode = dataNode;
        myParent = parent;
    }

    @Override
    public boolean isAutoExpandNode() {
        SimpleNode parent = getParent();
        return parent != null && parent.getChildCount() == 1;
    }

    public void setParent(@Nullable ExternalSystemNode<?> parent) {
        myParent = parent;
    }

    @Nullable
    public T getData() {
        return myDataNode != null ? myDataNode.getData() : null;
    }

    @Override
    @Nullable
    public NodeDescriptor getParentDescriptor() {
        return myParent;
    }

    @Override
    public String getName() {
        String displayName = myExternalProjectsView.getDisplayName(myDataNode);
        return displayName == null ? super.getName() : displayName;
    }

   
    protected ExternalProjectsView getExternalProjectsView() {
        return myExternalProjectsView;
    }

   
    protected ExternalSystemUiAware getUiAware() {
        return myExternalProjectsView.getUiAware();
    }

    @Nullable
    protected ExternalProjectsStructure getStructure() {
        return myExternalProjectsView.getStructure();
    }

   
    protected ExternalSystemShortcutsManager getShortcutsManager() {
        return myExternalProjectsView.getShortcutsManager();
    }

   
    protected ExternalSystemTaskActivator getTaskActivator() {
        return myExternalProjectsView.getTaskActivator();
    }

    @Nullable
    public <DataType extends ExternalSystemNode<?>> DataType findNode(Class<DataType> aClass) {
        ExternalSystemNode<?> node = this;
        while (node != null) {
            if (aClass.isInstance(node)) {
                //noinspection unchecked
                return (DataType) node;
            }
            node = node.myParent;
        }
        return null;
    }

    @Nullable
    public <DataType extends ExternalSystemNode<?>> DataType findParent(Class<DataType> aClass) {
        return myParent != null ? myParent.findNode(aClass) : null;
    }

    public boolean isVisible() {
        return getDisplayKind() != ExternalProjectsStructure.DisplayKind.NEVER
            && !(isIgnored() && !myExternalProjectsView.getShowIgnored());
    }

    public boolean isIgnored() {
        if (myDataNode != null) {
            return myDataNode.isIgnored();
        }
        SimpleNode parent = getParent();
        return parent instanceof ExternalSystemNode && ((ExternalSystemNode<?>) parent).isIgnored();
    }

    public void setIgnored(boolean ignored) {
        if (myDataNode != null) {
            ExternalProjectsManager.getInstance(myExternalProjectsView.getProject()).setIgnored(myDataNode, ignored);
        }
    }

    private ExternalProjectsStructure.DisplayKind getDisplayKind() {
        ExternalProjectsStructure structure = myExternalProjectsView.getStructure();
        if (structure == null) return ExternalProjectsStructure.DisplayKind.NORMAL;
        Class<?>[] visibles = structure.getVisibleNodesClasses();
        if (visibles == null) return ExternalProjectsStructure.DisplayKind.NORMAL;
        for (Class<?> each : visibles) {
            if (each.isInstance(this)) return ExternalProjectsStructure.DisplayKind.ALWAYS;
        }
        return ExternalProjectsStructure.DisplayKind.NEVER;
    }

    @Override
    public final ExternalSystemNode<?>[] getChildren() {
        if (myChildren == null) {
            myChildren = buildChildren();
        }
        return myChildren;
    }

    private ExternalSystemNode<?>[] buildChildren() {
        // Copy doBuildChildren() result BEFORE resetting myChildrenList to avoid duplication
        // when doBuildChildren() returns myChildrenList itself (e.g. for RootNode with no dataNode).
        List<? extends ExternalSystemNode<?>> newCandidates = new ArrayList<>(doBuildChildren());
        if (newCandidates.isEmpty()) return NO_CHILDREN;

        myChildrenList = NO_CHILDREN_LIST;
        addAll(newCandidates, true);
        sort(myChildrenList);
        List<ExternalSystemNode<?>> visibleNodes = new ArrayList<>();
        for (ExternalSystemNode<?> each : myChildrenList) {
            if (each.isVisible()) visibleNodes.add(each);
        }
        return visibleNodes.toArray(new ExternalSystemNode[0]);
    }

    void cleanUpCache() {
        myChildren = null;
        myChildrenList = NO_CHILDREN_LIST;
        myTotalErrorLevel = null;
    }

    ExternalSystemNode<?>[] getCached() {
        return myChildren;
    }

    protected void sort(List<ExternalSystemNode<?>> list) {
        if (!list.isEmpty()) {
            list.sort(ORDER_AWARE_COMPARATOR);
        }
    }

    public boolean addAll(Collection<? extends ExternalSystemNode<?>> nodes) {
        return addAll(nodes, false);
    }

    private boolean addAll(Collection<? extends ExternalSystemNode<?>> nodes, boolean silently) {
        if (nodes.isEmpty()) return false;
        if (myChildrenList == NO_CHILDREN_LIST) {
            myChildrenList = new CopyOnWriteArrayList<>();
        }
        for (ExternalSystemNode<?> node : nodes) {
            node.setParent(this);
            myChildrenList.add(node);
        }
        if (!silently) {
            childrenChanged();
        }
        return true;
    }

    public boolean add(ExternalSystemNode<?> node) {
        return addAll(Collections.singletonList(node));
    }

    public boolean removeAll(Collection<? extends ExternalSystemNode<?>> nodes) {
        return removeAll(nodes, false);
    }

    private boolean removeAll(Collection<? extends ExternalSystemNode<?>> nodes, boolean silently) {
        if (nodes.isEmpty()) return false;
        for (ExternalSystemNode<?> node : nodes) {
            node.setParent(null);
            if (myChildrenList != NO_CHILDREN_LIST) myChildrenList.remove(node);
        }
        if (!silently) {
            childrenChanged();
        }
        return true;
    }

    public void remove(ExternalSystemNode<?> node) {
        removeAll(Collections.singletonList(node));
    }

    protected void childrenChanged() {
        ExternalSystemNode<?> each = this;
        while (each != null) {
            each.myTotalErrorLevel = null;
            each = (ExternalSystemNode<?>) each.getParent();
        }
        sort(myChildrenList);
        List<ExternalSystemNode<?>> visible = new ArrayList<>();
        for (ExternalSystemNode<?> node : myChildrenList) {
            if (node.isVisible()) visible.add(node);
        }
        myChildren = visible.toArray(new ExternalSystemNode[0]);
        myExternalProjectsView.updateUpTo(this);
    }

    public boolean hasChildren() {
        return getChildren().length > 0;
    }

   
    protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
        if (myDataNode != null && !myDataNode.getChildren().isEmpty()) {
            return myExternalProjectsView.createNodes(myExternalProjectsView, this, myDataNode);
        }
        return myChildrenList;
    }

    private void setDataNode(@Nullable DataNode<T> dataNode) {
        myDataNode = dataNode;
    }

   
    private ExternalProjectsStructure.ErrorLevel getTotalErrorLevel() {
        ExternalProjectsStructure.ErrorLevel level = myTotalErrorLevel;
        if (level == null) {
            myTotalErrorLevel = level = calcTotalErrorLevel();
        }
        return level;
    }

   
    private ExternalProjectsStructure.ErrorLevel calcTotalErrorLevel() {
        ExternalProjectsStructure.ErrorLevel childrenLevel = getChildrenErrorLevel();
        return childrenLevel.compareTo(myErrorLevel) > 0 ? childrenLevel : myErrorLevel;
    }

   
    public ExternalProjectsStructure.ErrorLevel getChildrenErrorLevel() {
        if (myChildren == null && myDataNode != null) {
            return myExternalProjectsView.getErrorLevelRecursively(myDataNode);
        }
        ExternalProjectsStructure.ErrorLevel result = ExternalProjectsStructure.ErrorLevel.NONE;
        for (ExternalSystemNode<?> each : getChildren()) {
            ExternalProjectsStructure.ErrorLevel level = each.getTotalErrorLevel();
            if (level.compareTo(result) > 0) result = level;
            if (result == ExternalProjectsStructure.ErrorLevel.ERROR) break;
        }
        return result;
    }

    public void setErrorLevel(ExternalProjectsStructure.ErrorLevel level, String... errors) {
        if (myErrorLevel == level) return;
        myErrorLevel = level;
        myErrors.clear();
        myErrors.addAll(Arrays.asList(errors));
        myExternalProjectsView.updateUpTo(this);
    }

    @Override
    protected void update(PresentationData presentation) {
        setNameAndTooltip(presentation, getName(), null);
    }

    protected void setNameAndTooltip(PresentationData presentation, @Nullable String name, @Nullable String tooltip) {
        setNameAndTooltip(presentation, name, tooltip, (String) null);
    }

    protected void setNameAndTooltip(PresentationData presentation,
                                     @Nullable String name,
                                     @Nullable String tooltip,
                                     @Nullable String hint) {
        boolean ignored = isIgnored();
        SimpleTextAttributes textAttributes = ignored ? SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES : getPlainAttributes();
        setNameAndTooltip(presentation, name, tooltip, textAttributes);
        if (!StringUtil.isEmptyOrSpaces(hint)) {
            presentation.addText(" (" + hint + ")",
                ignored ? SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }

    protected void setNameAndTooltip(PresentationData presentation,
                                     @Nullable String name,
                                     @Nullable String tooltip,
                                     SimpleTextAttributes attributes) {
        presentation.clearText();
        if (name != null) {
            presentation.addText(name, prepareAttributes(attributes));
        }
        String s = (tooltip != null ? tooltip + "\n\r" : "") + StringUtil.join(myErrors, "\n\r");
        presentation.setTooltip(s);
    }

    private SimpleTextAttributes prepareAttributes(SimpleTextAttributes from) {
        ExternalProjectsStructure.ErrorLevel level = getTotalErrorLevel();
        Color waveColor = level == ExternalProjectsStructure.ErrorLevel.NONE ? null : JBColor.RED;
        int style = from.getStyle();
        if (waveColor != null) style |= SimpleTextAttributes.STYLE_WAVED;
        return new SimpleTextAttributes(from.getBgColor(), from.getFgColor(), waveColor, style);
    }

    @Nullable
    protected String getActionId() {
        return null;
    }

    @Nullable
    protected String getMenuId() {
        return null;
    }

    @Override
    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
        String actionId = getActionId();
        myExternalProjectsView.handleDoubleClickOrEnter(this, actionId, inputEvent);
    }

    @Override
    public int compareTo(ExternalSystemNode<?> node) {
        return StringUtil.compare(this.getName(), node.getName(), true);
    }

    public void mergeWith(ExternalSystemNode<T> newNode) {
        setDataNode(newNode.myDataNode);
    }
}
