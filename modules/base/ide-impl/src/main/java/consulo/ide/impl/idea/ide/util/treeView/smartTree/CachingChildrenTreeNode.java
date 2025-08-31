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

package consulo.ide.impl.idea.ide.util.treeView.smartTree;

import consulo.ide.impl.idea.ide.structureView.impl.StructureViewElementWrapper;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.tree.*;
import consulo.project.Project;
import consulo.navigation.Navigatable;

import jakarta.annotation.Nonnull;
import java.util.*;

public abstract class CachingChildrenTreeNode <Value> extends AbstractTreeNode<Value> {
  private List<CachingChildrenTreeNode> myChildren;
  private List<CachingChildrenTreeNode> myOldChildren = null;
  protected final TreeModel myTreeModel;

  public CachingChildrenTreeNode(Project project, Value value, TreeModel treeModel) {
    super(project,
          value instanceof StructureViewElementWrapper ? (Value) ((StructureViewElementWrapper) value).getWrappedElement() : value);
    myTreeModel = treeModel;
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    ensureChildrenAreInitialized();
    return new ArrayList<AbstractTreeNode>(myChildren);
  }

  private void ensureChildrenAreInitialized() {
    if (myChildren == null) {
      myChildren = new ArrayList<CachingChildrenTreeNode>();
      rebuildSubtree();
    }
  }

  protected void addSubElement(CachingChildrenTreeNode node) {
    ensureChildrenAreInitialized();
    myChildren.add(node);
    node.setParent(this);
  }

  protected void setChildren(Collection<AbstractTreeNode> children) {
    clearChildren();
    for (AbstractTreeNode node : children) {
      myChildren.add((CachingChildrenTreeNode)node);
      node.setParent(this);
    }
  }

  private static class CompositeComparator implements java.util.Comparator<CachingChildrenTreeNode> {
    private final Sorter[] mySorters;

    public CompositeComparator(Sorter[] sorters) {
      mySorters = sorters;
    }

    @Override
    public int compare(CachingChildrenTreeNode o1, CachingChildrenTreeNode o2) {
      Object value1 = o1.getValue();
      Object value2 = o2.getValue();
      for (Sorter sorter : mySorters) {
        int result = sorter.getComparator().compare(value1, value2);
        if (result != 0) return result;
      }
      return 0;
    }
  }

  protected void sortChildren(Sorter[] sorters) {
    if (myChildren == null) return;

    Collections.sort(myChildren, new CompositeComparator(sorters));

    for (CachingChildrenTreeNode child : myChildren) {
      if (child instanceof GroupWrapper) {
        child.sortChildren(sorters);
      }
    }
  }

  protected void filterChildren(Filter[] filters) {
    Collection<AbstractTreeNode> children = getChildren();
    for (Filter filter : filters) {
      for (Iterator<AbstractTreeNode> eachNode = children.iterator(); eachNode.hasNext();) {
        TreeElementWrapper eachChild = (TreeElementWrapper)eachNode.next();
        if (!filter.isVisible(eachChild.getValue())) {
          eachNode.remove();
        }
      }
    }
    setChildren(children);
  }

  protected void groupChildren(Grouper[] groupers) {
    for (Grouper grouper : groupers) {
      groupElements(grouper);
    }
    Collection<AbstractTreeNode> children = getChildren();
    for (AbstractTreeNode child : children) {
      if (child instanceof GroupWrapper) {
        ((GroupWrapper)child).groupChildren(groupers);
      }
    }
  }

  private void groupElements(Grouper grouper) {
    ArrayList<AbstractTreeNode<TreeElement>> ungrouped = new ArrayList<AbstractTreeNode<TreeElement>>();
    Collection<AbstractTreeNode> children = getChildren();
    for (AbstractTreeNode child : children) {
      CachingChildrenTreeNode<TreeElement> node = (CachingChildrenTreeNode<TreeElement>)child;
      if (node instanceof TreeElementWrapper) {
        ungrouped.add(node);
      }
    }

    if (!ungrouped.isEmpty()) {
      processUngrouped(ungrouped, grouper);
    }

    Collection<AbstractTreeNode> result = new LinkedHashSet<AbstractTreeNode>();
    for (AbstractTreeNode child : children) {
      AbstractTreeNode parent = (AbstractTreeNode)child.getParent();
      if (parent != this) {
        if (!result.contains(parent)) result.add(parent);
      }
      else {
        result.add(child);
      }
    }
    setChildren(result);
  }

  private void processUngrouped(List<AbstractTreeNode<TreeElement>> ungrouped, Grouper grouper) {
    Map<TreeElement,AbstractTreeNode> ungroupedObjects = collectValues(ungrouped);
    Collection<Group> groups = grouper.group(this, ungroupedObjects.keySet());

    Map<Group, GroupWrapper> groupNodes = createGroupNodes(groups);

    for (Group group : groups) {
      GroupWrapper groupWrapper = groupNodes.get(group);
      Collection<TreeElement> children = group.getChildren();
      for (TreeElement node : children) {
        CachingChildrenTreeNode child = createChildNode(node);
        groupWrapper.addSubElement(child);
        AbstractTreeNode abstractTreeNode = ungroupedObjects.get(node);
        abstractTreeNode.setParent(groupWrapper);
      }
    }
  }

  protected TreeElementWrapper createChildNode(TreeElement child) {
    return new TreeElementWrapper(getProject(), child, myTreeModel);
  }

  private static Map<TreeElement, AbstractTreeNode> collectValues(List<AbstractTreeNode<TreeElement>> ungrouped) {
    Map<TreeElement, AbstractTreeNode> objects = new LinkedHashMap<TreeElement, AbstractTreeNode>();
    for (AbstractTreeNode<TreeElement> node : ungrouped) {
      objects.put(node.getValue(), node);
    }
    return objects;
  }

  private Map<Group, GroupWrapper> createGroupNodes(Collection<Group> groups) {
    Map<Group, GroupWrapper> result = new HashMap<Group, GroupWrapper>();
    for (Group group : groups) {
      result.put(group, createGroupWrapper(getProject(), group, myTreeModel));
    }
    return result;
  }

  protected GroupWrapper createGroupWrapper(Project project, Group group, TreeModel treeModel) {
    return new GroupWrapper(project, group, treeModel);
  }


  private void rebuildSubtree() {
    initChildren();
    performTreeActions();

    synchronizeChildren();

  }

  protected void synchronizeChildren() {
    if (myOldChildren != null && myChildren != null) {
      HashMap<Object, CachingChildrenTreeNode> oldValuesToChildrenMap = new HashMap<Object, CachingChildrenTreeNode>();
      for (CachingChildrenTreeNode oldChild : myOldChildren) {
        Object oldValue = oldChild instanceof TreeElementWrapper ? oldChild.getValue() : oldChild;
        if (oldValue != null) {
          oldValuesToChildrenMap.put(oldValue, oldChild);
        }
      }

      for (int i = 0; i < myChildren.size(); i++) {
        CachingChildrenTreeNode newChild = myChildren.get(i);
        Object newValue = newChild instanceof TreeElementWrapper ? newChild.getValue() : newChild;
        if (newValue != null) {
          CachingChildrenTreeNode oldChild = oldValuesToChildrenMap.get(newValue);
          if (oldChild != null) {
            oldChild.copyFromNewInstance(newChild);
            oldChild.setValue(newChild.getValue());
            myChildren.set(i, oldChild);
          }
        }
      }

      myOldChildren = null;
    }
  }

  protected abstract void copyFromNewInstance(CachingChildrenTreeNode newInstance);

  protected abstract void performTreeActions();

  protected abstract void initChildren();

  @Override
  public void navigate(boolean requestFocus) {
    ((Navigatable)getValue()).navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return getValue() instanceof Navigatable && ((Navigatable)getValue()).canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getValue() instanceof Navigatable && ((Navigatable)getValue()).canNavigateToSource();
  }

  protected void clearChildren() {
    if (myChildren != null) {
      myChildren.clear();
    } else {
      myChildren = new ArrayList<CachingChildrenTreeNode>();
    }
  }

  public void rebuildChildren() {
    if (myChildren != null) {
      myOldChildren = myChildren;
      for (CachingChildrenTreeNode node : myChildren) {
        node.rebuildChildren();
      }
      myChildren = null;
    }
  }

  protected void resetChildren() {
    myChildren = null;
  }
}
