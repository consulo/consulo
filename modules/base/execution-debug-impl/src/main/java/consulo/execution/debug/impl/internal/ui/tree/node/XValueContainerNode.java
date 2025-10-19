/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.evaluation.InlineDebuggerHelper;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkListener;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class XValueContainerNode<ValueContainer extends XValueContainer> extends XDebuggerTreeNode implements XCompositeNode, TreeNode {
  private List<XValueNodeImpl> myValueChildren;
  private List<MessageTreeNode> myMessageChildren;
  private List<MessageTreeNode> myTemporaryMessageChildren;
  private MessageTreeNode myTemporaryEditorNode;
  private List<XValueGroupNodeImpl> myTopGroups;
  private List<XValueGroupNodeImpl> myBottomGroups;
  private List<TreeNode> myCachedAllChildren;
  protected final ValueContainer myValueContainer;
  private volatile boolean myObsolete;
  private volatile boolean myAlreadySorted;

  protected XValueContainerNode(XDebuggerTree tree, XDebuggerTreeNode parent, @Nonnull ValueContainer valueContainer) {
    super(tree, parent, true);
    myValueContainer = valueContainer;
  }

  private void loadChildren() {
    if (myValueChildren != null || myMessageChildren != null || myTemporaryMessageChildren != null) return;
    startComputingChildren();
  }

  public void startComputingChildren() {
    myCachedAllChildren = null;
    setTemporaryMessageNode(createLoadingMessageNode());
    myValueContainer.computeChildren(this);
  }

  protected MessageTreeNode createLoadingMessageNode() {
    return MessageTreeNode.createLoadingMessage(myTree, this);
  }

  @Override
  public void setAlreadySorted(boolean alreadySorted) {
    myAlreadySorted = alreadySorted;
  }

  @Override
  public void addChildren(@Nonnull XValueChildrenList children, boolean last) {
    if (myObsolete) return;
    invokeNodeUpdate(() -> {
      if (myObsolete) return;
      List<XValueContainerNode<?>> newChildren;
      if (children.size() > 0) {
        newChildren = new ArrayList<>(children.size());
        if (myValueChildren == null) {
          if (!myAlreadySorted && XDebuggerSettingsManager.getInstance().getDataViewSettings().isSortValues()) {
            myValueChildren = Lists.newSortedList(XValueNodeImpl.COMPARATOR);
          }
          else {
            myValueChildren = new ArrayList<>(children.size());
          }
        }
        boolean valuesInline = XDebuggerSettingsManager.getInstance().getDataViewSettings().isShowValuesInline();
        InlineDebuggerHelper inlineHelper = getTree().getEditorsProvider().getInlineDebuggerHelper();
        for (int i = 0; i < children.size(); i++) {
          XValueNodeImpl node = new XValueNodeImpl(myTree, this, children.getName(i), children.getValue(i));
          myValueChildren.add(node);
          newChildren.add(node);

          if (valuesInline && inlineHelper.shouldEvaluateChildrenByDefault(node) && isUseGetChildrenHack(myTree)) { //todo[kb]: try to generify this dirty hack
            node.getChildren();
          }
        }
      }
      else {
        newChildren = new SmartList<>();
        if (myValueChildren == null) {
          myValueChildren = new SmartList<>();
        }
      }

      myTopGroups = createGroupNodes(children.getTopGroups(), myTopGroups, newChildren);
      myBottomGroups = createGroupNodes(children.getBottomGroups(), myBottomGroups, newChildren);
      myCachedAllChildren = null;
      fireNodesInserted(newChildren);
      if (last && myTemporaryMessageChildren != null) {
        int[] ints = getNodesIndices(myTemporaryMessageChildren);
        TreeNode[] removed = myTemporaryMessageChildren.toArray(new TreeNode[myTemporaryMessageChildren.size()]);
        myCachedAllChildren = null;
        myTemporaryMessageChildren = null;
        fireNodesRemoved(ints, removed);
      }
      myTree.childrenLoaded(this, newChildren, last);
    });
  }

  private static boolean isUseGetChildrenHack(@Nonnull XDebuggerTree tree) {
    return !tree.isUnderRemoteDebug();
  }

  @Nullable
  private List<XValueGroupNodeImpl> createGroupNodes(
    List<XValueGroup> groups,
    @Nullable List<XValueGroupNodeImpl> prevNodes,
    List<XValueContainerNode<?>> newChildren
  ) {
    if (groups.isEmpty()) return prevNodes;

    List<XValueGroupNodeImpl> nodes = prevNodes != null ? prevNodes : new SmartList<>();
    for (XValueGroup group : groups) {
      XValueGroupNodeImpl node = new XValueGroupNodeImpl(myTree, this, group);
      nodes.add(node);
      newChildren.add(node);
    }
    return nodes;
  }

  @Override
  public void tooManyChildren(int remaining) {
    invokeNodeUpdate(() -> setTemporaryMessageNode(MessageTreeNode.createEllipsisNode(myTree, this, remaining)));
  }

  @Override
  public boolean isObsolete() {
    return myObsolete;
  }

  @Override
  public void clearChildren() {
    myCachedAllChildren = null;
    myMessageChildren = null;
    myTemporaryMessageChildren = null;
    myTemporaryEditorNode = null;
    myValueChildren = null;
    myTopGroups = null;
    myBottomGroups = null;
    fireNodeStructureChanged();
  }

  @Override
  public void setErrorMessage(@Nonnull LocalizeValue errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
    setMessage(
      errorMessage,
      XDebuggerUIConstants.ERROR_MESSAGE_ICON,
      XDebuggerUIConstants.ERROR_MESSAGE_ATTRIBUTES,
      link
    );
    invokeNodeUpdate(() -> setMessageNodes(Collections.emptyList(), true)); // clear temporary nodes
  }

  @Override
  public void setMessage(
    @Nonnull LocalizeValue message,
    Image icon,
    @Nonnull SimpleTextAttributes attributes,
    @Nullable XDebuggerTreeNodeHyperlink link
  ) {
    invokeNodeUpdate(
      () -> setMessageNodes(
        MessageTreeNode.createMessages(myTree, this, message, link, icon, attributes),
        false)
    );
  }

  public void setInfoMessage(@Nonnull LocalizeValue message, @Nullable HyperlinkListener hyperlinkListener) {
    invokeNodeUpdate(
      () -> setMessageNodes(
        Collections.singletonList(MessageTreeNode.createInfoMessage(myTree, message, hyperlinkListener)),
        false
      )
    );
  }

  private void setTemporaryMessageNode(MessageTreeNode messageNode) {
    setMessageNodes(Collections.singletonList(messageNode), true);
  }

  private void setMessageNodes(List<MessageTreeNode> messages, boolean temporary) {
    myCachedAllChildren = null;
    List<MessageTreeNode> toDelete = temporary ? myTemporaryMessageChildren : myMessageChildren;
    if (toDelete != null) {
      fireNodesRemoved(getNodesIndices(toDelete), toDelete.toArray(new TreeNode[toDelete.size()]));
    }
    if (temporary) {
      myTemporaryMessageChildren = messages;
    }
    else {
      myMessageChildren = messages;
    }
    myCachedAllChildren = null;
    fireNodesInserted(messages);
  }

  @Nonnull
  public XDebuggerTreeNode addTemporaryEditorNode(@Nullable Image icon, @Nullable String text) {
    if (isLeaf()) {
      setLeaf(false);
    }
    myTree.expandPath(getPath());
    MessageTreeNode node = new MessageTreeNode(myTree, this, true);
    node.setIcon(icon);
    if (!StringUtil.isEmpty(text)) {
      node.getText().append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    myTemporaryEditorNode = node;
    myCachedAllChildren = null;
    fireNodesInserted(Collections.singleton(node));
    return node;
  }

  public void removeTemporaryEditorNode(XDebuggerTreeNode node) {
    if (myTemporaryEditorNode != null) {
      int index = getIndex(myTemporaryEditorNode);
      myTemporaryEditorNode = null;
      myCachedAllChildren = null;
      fireNodesRemoved(new int[]{index}, new TreeNode[]{node});
    }
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  protected int removeChildNode(List children, XDebuggerTreeNode node) {
    int index = children.indexOf(node);
    if (index != -1) {
      children.remove(node);
      fireNodesRemoved(new int[]{index}, new TreeNode[]{node});
    }
    return index;
  }

  @Nonnull
  @Override
  public List<? extends TreeNode> getChildren() {
    loadChildren();

    if (myCachedAllChildren == null) {
      myCachedAllChildren = new ArrayList<>();
      if (myTemporaryEditorNode != null) {
        myCachedAllChildren.add(myTemporaryEditorNode);
      }
      if (myMessageChildren != null) {
        myCachedAllChildren.addAll(myMessageChildren);
      }
      if (myTopGroups != null) {
        myCachedAllChildren.addAll(myTopGroups);
      }
      if (myValueChildren != null) {
        myCachedAllChildren.addAll(myValueChildren);
      }
      if (myBottomGroups != null) {
        myCachedAllChildren.addAll(myBottomGroups);
      }
      if (myTemporaryMessageChildren != null) {
        myCachedAllChildren.addAll(myTemporaryMessageChildren);
      }
    }
    return myCachedAllChildren;
  }

  @Nonnull
  public ValueContainer getValueContainer() {
    return myValueContainer;
  }

  @Override
  @Nonnull
  public List<? extends XValueContainerNode<?>> getLoadedChildren() {
    List<? extends XValueContainerNode<?>> empty = Collections.<XValueGroupNodeImpl>emptyList();
    return ContainerUtil.concat(
      ObjectUtil.notNull(myTopGroups, empty),
      ObjectUtil.notNull(myValueChildren, empty),
      ObjectUtil.notNull(myBottomGroups, empty)
    );
  }

  public void setObsolete() {
    myObsolete = true;
  }
}
