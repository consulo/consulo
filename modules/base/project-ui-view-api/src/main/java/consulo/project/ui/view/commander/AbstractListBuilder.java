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

package consulo.project.ui.view.commander;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Belyaev
 */
public abstract class AbstractListBuilder {
  protected final Project myProject;
  protected final JList myList;
  protected final Model myModel;
  protected final AbstractTreeStructure myTreeStructure;
  private final Comparator myComparator;

  protected JLabel myParentTitle = null;
  private boolean myIsDisposed;
  private AbstractTreeNode myCurrentParent = null;
  private final AbstractTreeNode myShownRoot;

  public interface Model {
    void removeAllElements();

    void addElement(Object node);

    void replaceElements(List newElements);

    Object[] toArray();

    int indexOf(Object o);

    int getSize();

    Object getElementAt(int idx);
  }

  public AbstractListBuilder(Project project,
                             JList list,
                             Model model,
                             AbstractTreeStructure treeStructure,
                             Comparator comparator,
                             boolean showRoot) {
    myProject = project;
    myList = list;
    myModel = model;
    myTreeStructure = treeStructure;
    myComparator = comparator;

    Object rootElement = myTreeStructure.getRootElement();
    Object[] rootChildren = myTreeStructure.getChildElements(rootElement);

    if (!showRoot && rootChildren.length == 1 && shouldEnterSingleTopLevelElement(rootChildren[0])) {
      myShownRoot = (AbstractTreeNode)rootChildren[0];
    }
    else {
      myShownRoot = (AbstractTreeNode)rootElement;
    }
  }

  protected abstract boolean shouldEnterSingleTopLevelElement(Object rootChild);

  public final void setParentTitle(JLabel parentTitle) {
    myParentTitle = parentTitle;
  }

  public final void drillDown() {
    Object value = getSelectedValue();
    if (value instanceof AbstractTreeNode node) {
      try {
        buildList(node);
        ensureSelectionExist();
      }
      finally {
        updateParentTitle();
      }
    }
    else { // an element that denotes parent
      goUp();
    }
  }

  public final void goUp() {
    if (myCurrentParent == myShownRoot.getParent()) {
      return;
    }
    AbstractTreeNode element = (AbstractTreeNode)myCurrentParent.getParent();
    if (element == null) {
      return;
    }

    try {
      AbstractTreeNode oldParent = myCurrentParent;

      buildList(element);

      for (int i = 0; i < myModel.getSize(); i++) {
        if (myModel.getElementAt(i) instanceof NodeDescriptor desc) {
          Object elem = desc.getElement();
          if (oldParent.equals(elem)) {
            selectItem(i);
            break;
          }
        }
      }
    }
    finally {
      updateParentTitle();
    }
  }

  protected Object getSelectedValue() {
    return myList.getSelectedValue();
  }

  protected void selectItem(int i) {
    ScrollingUtil.selectItem(myList, i);
  }

  protected void ensureSelectionExist() {
    ScrollingUtil.ensureSelectionExists(myList);
  }

  @RequiredUIAccess
  public final void selectElement(Object element, VirtualFile virtualFile) {
    if (element == null) {
      return;
    }

    try {
      AbstractTreeNode node = goDownToElement(element, virtualFile);
      if (node == null) return;
      AbstractTreeNode parentElement = (AbstractTreeNode)node.getParent();
      if (parentElement == null) return;

      buildList(parentElement);

      for (int i = 0; i < myModel.getSize(); i++) {
        if (myModel.getElementAt(i) instanceof AbstractTreeNode desc) {
          if (desc.getValue() instanceof StructureViewTreeElement treeElement) {
            if (element.equals(treeElement.getValue())) {
              selectItem(i);
              break;
            }
          }
          else {
            if (element.equals(desc.getValue())) {
              selectItem(i);
              break;
            }
          }
        }
      }
    }
    finally {
      updateParentTitle();
    }
  }

  public final void enterElement(PsiElement element, VirtualFile file) {
    try {
      AbstractTreeNode lastPathNode = goDownToElement(element, file);
      if (lastPathNode == null) return;
      buildList(lastPathNode);
      ensureSelectionExist();
    }
    finally {
      updateParentTitle();
    }
  }

  private AbstractTreeNode goDownToElement(Object element, VirtualFile file) {
    return goDownToNode((AbstractTreeNode)myTreeStructure.getRootElement(), element, file);
  }

  public final void enterElement(AbstractTreeNode element) {
    try {
      buildList(element);
      ensureSelectionExist();
    }
    finally {
      updateParentTitle();
    }
  }

  private AbstractTreeNode goDownToNode(AbstractTreeNode lastPathNode, Object lastPathElement, VirtualFile file) {
    if (file == null) return lastPathNode;
    AbstractTreeNode found = lastPathNode;
    while (found != null) {
      if (nodeIsAcceptableForElement(lastPathNode, lastPathElement)) {
        break;
      }
      else {
        found = findInChildren(lastPathNode, file, lastPathElement);
        if (found != null) {
          lastPathNode = found;
        }
      }
    }
    return lastPathNode;
  }

  private AbstractTreeNode findInChildren(AbstractTreeNode rootElement, VirtualFile file, Object element) {
    Object[] childElements = getChildren(rootElement);
    List<AbstractTreeNode> nodes = getAllAcceptableNodes(childElements, file);
    if (nodes.size() == 1) return nodes.get(0);
    if (nodes.isEmpty()) return null;
    if (file.isDirectory()) {
      return nodes.get(0);
    }
    else {
      return performDeepSearch(nodes.toArray(), element, new HashSet<AbstractTreeNode>());
    }
  }

  private AbstractTreeNode performDeepSearch(Object[] nodes, Object element, Set<AbstractTreeNode> visited) {
    for (Object node1 : nodes) {
      AbstractTreeNode node = (AbstractTreeNode)node1;
      if (nodeIsAcceptableForElement(node, element)) return node;
      Object[] children = getChildren(node);
      if (visited.add(node)) {
        AbstractTreeNode nodeResult = performDeepSearch(children, element, visited);
        if (nodeResult != null) {
          return nodeResult;
        }
      }
    }
    return null;
  }

  protected abstract boolean nodeIsAcceptableForElement(AbstractTreeNode node, Object element);

  protected abstract List<AbstractTreeNode> getAllAcceptableNodes(Object[] childElements, VirtualFile file);

  public void dispose() {
    myIsDisposed = true;
  }

  @RequiredUIAccess
  private void buildList(AbstractTreeNode parentElement) {
    myCurrentParent = parentElement;
    Future<?> future = AppExecutorUtil.getAppScheduledExecutorService()
            .schedule(() -> myList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)), 200, TimeUnit.MILLISECONDS);

    Object[] children = getChildren(parentElement);
    myModel.removeAllElements();
    if (shouldAddTopElement()) {
      myModel.addElement(new TopLevelNode(myProject, parentElement.getValue()));
    }

    for (Object aChildren : children) {
      AbstractTreeNode child = (AbstractTreeNode)aChildren;
      child.update();
    }
    if (myComparator != null) {
      Arrays.sort(children, myComparator);
    }
    for (Object aChildren : children) {
      myModel.addElement(aChildren);
    }

    boolean canceled = future.cancel(false);
    if (!canceled) {
      AppExecutorUtil.getAppExecutorService().execute(() -> myList.setCursor(Cursor.getDefaultCursor()));
    }
  }

  protected boolean shouldAddTopElement() {
    return !myShownRoot.equals(myCurrentParent);
  }

  private Object[] getChildren(AbstractTreeNode parentElement) {
    if (parentElement == null) {
      return new Object[]{myTreeStructure.getRootElement()};
    }
    else {
      return myTreeStructure.getChildElements(parentElement);
    }
  }

  protected final void updateList() {
    if (myIsDisposed || myCurrentParent == null) {
      return;
    }
    if (myTreeStructure.hasSomethingToCommit()) {
      myTreeStructure.commit();
    }

    AbstractTreeNode parentDescriptor = myCurrentParent;

    while (true) {
      parentDescriptor.update();
      if (parentDescriptor.getValue() != null) break;
      parentDescriptor = (AbstractTreeNode)parentDescriptor.getParent();
    }

    Object[] children = getChildren(parentDescriptor);
    Map<Object, Integer> elementToIndexMap = new HashMap<>();
    for (int i = 0; i < children.length; i++) {
      elementToIndexMap.put(children[i], Integer.valueOf(i));
    }

    List<NodeDescriptor> resultDescriptors = new ArrayList<NodeDescriptor>();
    Object[] listChildren = myModel.toArray();
    for (Object child : listChildren) {
      if (!(child instanceof NodeDescriptor)) {
        continue;
      }
      NodeDescriptor descriptor = (NodeDescriptor)child;
      descriptor.update();
      Object newElement = descriptor.getElement();
      Integer index = newElement != null ? elementToIndexMap.get(newElement) : null;
      if (index != null) {
        resultDescriptors.add(descriptor);
        descriptor.setIndex(index.intValue());
        elementToIndexMap.remove(newElement);
      }
    }

    for (Object child : elementToIndexMap.keySet()) {
      Integer index = elementToIndexMap.get(child);
      if (index != null) {
        NodeDescriptor childDescr = myTreeStructure.createDescriptor(child, parentDescriptor);
        childDescr.setIndex(index.intValue());
        childDescr.update();
        resultDescriptors.add(childDescr);
      }
    }

    SelectionInfo selection = storeSelection();
    if (myComparator != null) {
      Collections.sort(resultDescriptors, myComparator);
    }
    else {
      Collections.sort(resultDescriptors, IndexComparator.INSTANCE);
    }

    if (shouldAddTopElement()) {
      List elems = new ArrayList();
      elems.add(new TopLevelNode(myProject, parentDescriptor.getValue()));
      elems.addAll(resultDescriptors);
      myModel.replaceElements(elems);
    }
    else {
      myModel.replaceElements(resultDescriptors);
    }

    restoreSelection(selection);
    updateParentTitle();
  }

  private static final class SelectionInfo {
    public final ArrayList<Object> mySelectedObjects;
    public final Object myLeadSelection;
    public final int myLeadSelectionIndex;

    public SelectionInfo(ArrayList<Object> selectedObjects, int leadSelectionIndex, Object leadSelection) {
      myLeadSelection = leadSelection;
      myLeadSelectionIndex = leadSelectionIndex;
      mySelectedObjects = selectedObjects;
    }
  }

  private SelectionInfo storeSelection() {
    ListSelectionModel selectionModel = myList.getSelectionModel();
    ArrayList<Object> selectedObjects = new ArrayList<Object>();
    int[] selectedIndices = myList.getSelectedIndices();
    int leadSelectionIndex = selectionModel.getLeadSelectionIndex();
    Object leadSelection = null;
    for (int index : selectedIndices) {
      if (index < myList.getModel().getSize()) {
        Object o = myModel.getElementAt(index);
        selectedObjects.add(o);
        if (index == leadSelectionIndex) {
          leadSelection = o;
        }
      }
    }
    return new SelectionInfo(selectedObjects, leadSelectionIndex, leadSelection);
  }

  private void restoreSelection(SelectionInfo selection) {
    ArrayList<Object> selectedObjects = selection.mySelectedObjects;

    ListSelectionModel selectionModel = myList.getSelectionModel();

    selectionModel.clearSelection();
    if (!selectedObjects.isEmpty()) {
      int leadIndex = -1;
      for (int i = 0; i < selectedObjects.size(); i++) {
        Object o = selectedObjects.get(i);
        int index = myModel.indexOf(o);
        if (index > -1) {
          selectionModel.addSelectionInterval(index, index);
          if (o == selection.myLeadSelection) {
            leadIndex = index;
          }
        }
      }

      if (selectionModel.getMinSelectionIndex() == -1) {
        int toSelect = Math.min(selection.myLeadSelectionIndex, myModel.getSize() - 1);
        if (toSelect >= 0) {
          myList.setSelectedIndex(toSelect);
        }
      }
      else if (leadIndex != -1) {
        selectionModel.setLeadSelectionIndex(leadIndex);
      }
    }
  }

  public final AbstractTreeNode getParentNode() {
    return myCurrentParent;
  }

  protected abstract void updateParentTitle();

  public final void buildRoot() {
    buildList(myShownRoot);
  }
}