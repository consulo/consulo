// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.structureView.newStructureView;

import consulo.disposer.Disposer;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.event.FileEditorPositionListener;
import consulo.fileEditor.structureView.event.ModelListener;
import consulo.fileEditor.structureView.tree.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TreeModelWrapper implements StructureViewModel, ProvidingTreeModel {
  private final StructureViewModel myModel;
  private final TreeActionsOwner myStructureView;

  public TreeModelWrapper(StructureViewModel model, TreeActionsOwner structureView) {
    myModel = model;
    myStructureView = structureView;
  }

  @Override
  
  public StructureViewTreeElement getRoot() {
    return myModel.getRoot();
  }

  @Override
  
  public Grouper[] getGroupers() {
    List<Grouper> filtered = filterActive(myModel.getGroupers());
    return filtered.toArray(Grouper.EMPTY_ARRAY);
  }

  
  private <T extends TreeAction> List<T> filterActive(T[] actions) {
    List<T> filtered = new ArrayList<>();
    for (T action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  
  private List<NodeProvider> filterProviders(Collection<NodeProvider> actions) {
    List<NodeProvider> filtered = new ArrayList<>();
    for (NodeProvider action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  private boolean isFiltered(TreeAction action) {
    return action instanceof Sorter && !((Sorter)action).isVisible() || myStructureView.isActionActive(action.getName());
  }

  @Override
  
  public Sorter[] getSorters() {
    List<Sorter> filtered = filterActive(myModel.getSorters());
    return filtered.toArray(Sorter.EMPTY_ARRAY);
  }

  @Override
  
  public Filter[] getFilters() {
    List<Filter> filtered = filterActive(myModel.getFilters());
    return filtered.toArray(Filter.EMPTY_ARRAY);
  }

  @Override
  public Object getCurrentEditorElement() {
    return myModel.getCurrentEditorElement();
  }

  
  @Override
  public Collection<NodeProvider> getNodeProviders() {
    if (myModel instanceof ProvidingTreeModel) {
      return filterProviders(((ProvidingTreeModel)myModel).getNodeProviders());
    }
    return Collections.emptyList();
  }

  public static boolean isActive(TreeAction action, TreeActionsOwner actionsOwner) {
    if (shouldRevert(action)) {
      return !actionsOwner.isActionActive(action.getName());
    }
    return action instanceof Sorter && !((Sorter)action).isVisible() || actionsOwner.isActionActive(action.getName());
  }

  public static boolean shouldRevert(TreeAction action) {
    return action instanceof Filter && ((Filter)action).isReverted();
  }

  @Override
  public void addEditorPositionListener(FileEditorPositionListener listener) {
    myModel.addEditorPositionListener(listener);
  }

  @Override
  public void removeEditorPositionListener(FileEditorPositionListener listener) {
    myModel.removeEditorPositionListener(listener);
  }

  @Override
  public void dispose() {
    Disposer.dispose(myModel);
  }

  @Override
  public boolean shouldEnterElement(Object element) {
    return false;
  }

  @Override
  public void addModelListener(ModelListener modelListener) {
    myModel.addModelListener(modelListener);
  }

  @Override
  public void removeModelListener(ModelListener modelListener) {
    myModel.removeModelListener(modelListener);
  }

  public StructureViewModel getModel() {
    return myModel;
  }

  @Override
  public boolean isEnabled(NodeProvider provider) {
    return myStructureView.isActionActive(provider.getName());
  }
}
