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
package consulo.ide.impl.idea.usages.impl;

import consulo.application.ApplicationManager;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.navigation.Navigatable;
import consulo.usage.MergeableUsage;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageView;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author max
 */
public class GroupNode extends Node implements Navigatable, Comparable<GroupNode> {
  private static final NodeComparator COMPARATOR = new NodeComparator();
  private final int myRuleIndex;
  private int myRecursiveUsageCount; // EDT only access
  private final List<Node> myChildren = new SmartList<>(); // guarded by this

  private GroupNode(@Nonnull Node parent, @Nonnull UsageGroup group, int ruleIndex) {
    setUserObject(group);
    setParent(parent);
    myRuleIndex = ruleIndex;
  }

  // only for root fake node
  private GroupNode() {
    myRuleIndex = 0;
  }

  @Override
  protected void updateNotify() {
    if (getGroup() != null) {
      getGroup().update();
    }
  }

  public String toString() {
    String result = getGroup() == null ? "" : getGroup().getText(null);
    synchronized (this) {
      List<Node> children = myChildren;
      return result + ContainerUtil.getFirstItems(children, 10);
    }
  }

  @Nonnull
  List<Node> getChildren() {
    return myChildren;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  List<Node> getSwingChildren() {
    // on java 9 - children is Vector<TreeNode>
    List children = this.children;
    return ObjectUtil.notNull(children, Collections.<Node>emptyList());
  }

  @Nonnull
  GroupNode addOrGetGroup(@Nonnull UsageGroup group, int ruleIndex, @Nonnull Consumer<? super Node> edtInsertedUnderQueue) {
    GroupNode newNode;
    synchronized (this) {
      newNode = new GroupNode(this, group, ruleIndex);
      int i = getNodeIndex(newNode, myChildren);
      if (i >= 0) {
        return (GroupNode)myChildren.get(i);
      }
      int insertionIndex = -i - 1;
      myChildren.add(insertionIndex, newNode);
    }
    edtInsertedUnderQueue.accept(this);
    return newNode;
  }

  // >= 0 if found, < 0 if not found
  private static int getNodeIndex(@Nonnull Node newNode, @Nonnull List<? extends Node> children) {
    return Collections.binarySearch(children, newNode, COMPARATOR);
  }

  // always >= 0
  private static int getNodeInsertionIndex(@Nonnull Node node, @Nonnull List<? extends Node> children) {
    int i = getNodeIndex(node, children);
    return i >= 0 ? i : -i - 1;
  }

  void addTargetsNode(@Nonnull Node node, @Nonnull DefaultTreeModel treeModel) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    int index;
    synchronized (this) {
      index = getNodeInsertionIndex(node, getSwingChildren());
      myChildren.add(index, node);
    }
    treeModel.insertNodeInto(node, this, index);
  }

  @Override
  public void removeAllChildren() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    super.removeAllChildren();
    synchronized (this) {
      myChildren.clear();
    }
    myRecursiveUsageCount = 0;
  }

  @Nullable
  private UsageNode tryMerge(@Nonnull Usage usage) {
    if (!(usage instanceof MergeableUsage)) return null;
    MergeableUsage mergeableUsage = (MergeableUsage)usage;
    for (UsageNode node : getUsageNodes()) {
      Usage original = node.getUsage();
      if (original == mergeableUsage) {
        // search returned duplicate usage, ignore
        return node;
      }
      if (original instanceof MergeableUsage) {
        if (((MergeableUsage)original).merge(mergeableUsage)) return node;
      }
    }

    return null;
  }

  int removeUsagesBulk(@Nonnull Set<UsageNode> usages, @Nonnull DefaultTreeModel treeModel) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    int removed = 0;
    synchronized (this) {
      List<MutableTreeNode> removedNodes = new SmartList<>();
      for (UsageNode usage : usages) {
        if (myChildren.remove(usage)) {
          removedNodes.add(usage);
          removed++;
        }
      }

      if (removed == 0) {
        for (GroupNode groupNode : getSubGroups()) {
          int delta = groupNode.removeUsagesBulk(usages, treeModel);
          if (delta > 0) {
            if (groupNode.getRecursiveUsageCount() == 0) {
              myChildren.remove(groupNode);
              removedNodes.add(groupNode);
            }
            removed += delta;
            if (removed == usages.size()) break;
          }
        }
      }
      if (!myChildren.isEmpty()) {
        removeNodesFromParent(treeModel, this, removedNodes);
      }
    }

    if (removed > 0) {
      myRecursiveUsageCount -= removed;
      if (myRecursiveUsageCount != 0) {
        treeModel.nodeChanged(this);
      }
    }

    return removed;
  }

  /**
   * Implementation of javax.swing.tree.DefaultTreeModel#removeNodeFromParent(javax.swing.tree.MutableTreeNode) for multiple nodes.
   * Fires a single event, or does nothing when nodes is empty.
   *
   * @param treeModel to fire the treeNodesRemoved event on
   * @param parent    the parent
   * @param nodes     must all be children of parent
   */
  private static void removeNodesFromParent(@Nonnull DefaultTreeModel treeModel, @Nonnull GroupNode parent, @Nonnull List<? extends MutableTreeNode> nodes) {
    int count = nodes.size();
    if (count == 0) {
      return;
    }
    ObjectIntMap<MutableTreeNode> ordering = ObjectMaps.newObjectIntHashMap(count);
    for (MutableTreeNode node : nodes) {
      ordering.putInt(node, parent.getIndex(node));
    }
    Collections.sort(nodes, Comparator.comparingInt(ordering::getInt)); // need ascending order
    int[] indices = ordering.values().toArray();
    Arrays.sort(indices);
    for (int i = count - 1; i >= 0; i--) {
      parent.remove(indices[i]);
    }
    treeModel.nodesWereRemoved(parent, indices, nodes.toArray());
  }

  @Nonnull
  UsageNode addOrGetUsage(@Nonnull Usage usage, boolean filterDuplicateLines, @Nonnull Consumer<? super Node> edtInsertedUnderQueue) {
    UsageNode newNode;
    synchronized (this) {
      if (filterDuplicateLines) {
        UsageNode mergedWith = tryMerge(usage);
        if (mergedWith != null) {
          return mergedWith;
        }
      }
      newNode = new UsageNode(this, usage);
      int i = getNodeIndex(newNode, myChildren);
      // i>=0 means the usage already there (might happen when e.g. find usages was interrupted by typing and resumed with the same file)
      if (i >= 0) {
        newNode = (UsageNode)myChildren.get(i);
      }
      else {
        int insertionIndex = -i - 1;
        myChildren.add(insertionIndex, newNode);
      }
    }
    edtInsertedUnderQueue.accept(this);
    return newNode;
  }

  void incrementUsageCount() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    GroupNode groupNode = this;
    while (true) {
      groupNode.myRecursiveUsageCount++;
      TreeNode parent = groupNode.getParent();
      if (!(parent instanceof GroupNode)) return;
      groupNode = (GroupNode)parent;
    }
  }

  @Override
  public String tree2string(int indent, String lineSeparator) {
    StringBuffer result = new StringBuffer();
    StringUtil.repeatSymbol(result, ' ', indent);

    if (getGroup() != null) result.append(getGroup());
    result.append("[");
    result.append(lineSeparator);

    for (Node node : myChildren) {
      result.append(node.tree2string(indent + 4, lineSeparator));
      result.append(lineSeparator);
    }

    StringUtil.repeatSymbol(result, ' ', indent);
    result.append("]");
    result.append(lineSeparator);

    return result.toString();
  }

  @Override
  protected boolean isDataValid() {
    UsageGroup group = getGroup();
    return group == null || group.isValid();
  }

  @Override
  protected boolean isDataReadOnly() {
    Enumeration enumeration = children();
    while (enumeration.hasMoreElements()) {
      Object element = enumeration.nextElement();
      if (element instanceof Node && ((Node)element).isReadOnly()) return true;
    }
    return false;
  }

  private static class NodeComparator implements Comparator<DefaultMutableTreeNode> {
    enum ClassIndex {
      UNKNOWN,
      USAGE_TARGET,
      GROUP,
      USAGE
    }

    private static ClassIndex getClassIndex(DefaultMutableTreeNode node) {
      if (node instanceof UsageNode) return ClassIndex.USAGE;
      if (node instanceof GroupNode) return ClassIndex.GROUP;
      if (node instanceof UsageTargetNode) return ClassIndex.USAGE_TARGET;
      return ClassIndex.UNKNOWN;
    }

    @Override
    public int compare(DefaultMutableTreeNode n1, DefaultMutableTreeNode n2) {
      ClassIndex classIdx1 = getClassIndex(n1);
      ClassIndex classIdx2 = getClassIndex(n2);
      if (classIdx1 != classIdx2) return classIdx1.compareTo(classIdx2);
      if (classIdx1 == ClassIndex.GROUP) {
        int c = ((GroupNode)n1).compareTo((GroupNode)n2);
        if (c != 0) return c;
      }
      else if (classIdx1 == ClassIndex.USAGE) {
        int c = ((UsageNode)n1).compareTo((UsageNode)n2);
        if (c != 0) return c;
      }

      // return 0 only for the same Usages inside
      // (e.g. when tried to insert the UsageNode for the same Usage when interrupted by write action and resumed)
      Object u1 = n1.getUserObject();
      Object u2 = n2.getUserObject();
      if (Comparing.equal(u1, u2)) return 0;
      return System.identityHashCode(u1) - System.identityHashCode(u2);
    }
  }

  @Override
  public int compareTo(@Nonnull GroupNode groupNode) {
    if (myRuleIndex == groupNode.myRuleIndex) {
      return getGroup().compareTo(groupNode.getGroup());
    }

    return Integer.compare(myRuleIndex, groupNode.myRuleIndex);
  }

  public synchronized UsageGroup getGroup() {
    return (UsageGroup)getUserObject();
  }

  int getRecursiveUsageCount() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myRecursiveUsageCount;
  }

  @Override
  public void navigate(boolean requestFocus) {
    if (getGroup() != null) {
      getGroup().navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return getGroup() != null && getGroup().canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getGroup() != null && getGroup().canNavigateToSource();
  }


  @Override
  protected boolean isDataExcluded() {
    for (Node node : myChildren) {
      if (!node.isExcluded()) return false;
    }
    return true;
  }

  @Nonnull
  @Override
  protected String getText(@Nonnull UsageView view) {
    return getGroup().getText(view);
  }

  @Nonnull
  public synchronized Collection<GroupNode> getSubGroups() {
    List<GroupNode> list = new ArrayList<>();
    for (Node n : myChildren) {
      if (n instanceof GroupNode) {
        list.add((GroupNode)n);
      }
    }
    return list;
  }

  @Nonnull
  public synchronized Collection<UsageNode> getUsageNodes() {
    List<UsageNode> list = new ArrayList<>();
    for (Node n : myChildren) {
      if (n instanceof UsageNode) {
        list.add((UsageNode)n);
      }
    }
    return list;
  }

  @Nonnull
  static Root createRoot() {
    return new Root();
  }

  static class Root extends GroupNode {
    @NonNls
    public String toString() {
      return "Root " + super.toString();
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull UsageView view) {
      return "";
    }
  }
}
