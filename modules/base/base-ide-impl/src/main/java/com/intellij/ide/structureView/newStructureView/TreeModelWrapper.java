// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.structureView.newStructureView;

import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.*;
import consulo.disposer.Disposer;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TreeModelWrapper implements StructureViewModel, ProvidingTreeModel {
  private final StructureViewModel myModel;
  private final TreeActionsOwner myStructureView;

  public TreeModelWrapper(@Nonnull StructureViewModel model, @Nonnull TreeActionsOwner structureView) {
    myModel = model;
    myStructureView = structureView;
  }

  @Override
  @Nonnull
  public StructureViewTreeElement getRoot() {
    return myModel.getRoot();
  }

  @Override
  @Nonnull
  public Grouper[] getGroupers() {
    List<Grouper> filtered = filterActive(myModel.getGroupers());
    return filtered.toArray(Grouper.EMPTY_ARRAY);
  }

  @Nonnull
  private <T extends TreeAction> List<T> filterActive(@Nonnull T[] actions) {
    List<T> filtered = new ArrayList<>();
    for (T action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  @Nonnull
  private List<NodeProvider> filterProviders(@Nonnull Collection<NodeProvider> actions) {
    List<NodeProvider> filtered = new ArrayList<>();
    for (NodeProvider action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  private boolean isFiltered(@Nonnull TreeAction action) {
    return action instanceof Sorter && !((Sorter)action).isVisible() || myStructureView.isActionActive(action.getName());
  }

  @Override
  @Nonnull
  public Sorter[] getSorters() {
    List<Sorter> filtered = filterActive(myModel.getSorters());
    return filtered.toArray(Sorter.EMPTY_ARRAY);
  }

  @Override
  @Nonnull
  public Filter[] getFilters() {
    List<Filter> filtered = filterActive(myModel.getFilters());
    return filtered.toArray(Filter.EMPTY_ARRAY);
  }

  @Override
  public Object getCurrentEditorElement() {
    return myModel.getCurrentEditorElement();
  }

  @Nonnull
  @Override
  public Collection<NodeProvider> getNodeProviders() {
    if (myModel instanceof ProvidingTreeModel) {
      return filterProviders(((ProvidingTreeModel)myModel).getNodeProviders());
    }
    return Collections.emptyList();
  }

  public static boolean isActive(@Nonnull TreeAction action, @Nonnull TreeActionsOwner actionsOwner) {
    if (shouldRevert(action)) {
      return !actionsOwner.isActionActive(action.getName());
    }
    return action instanceof Sorter && !((Sorter)action).isVisible() || actionsOwner.isActionActive(action.getName());
  }

  public static boolean shouldRevert(@Nonnull TreeAction action) {
    return action instanceof Filter && ((Filter)action).isReverted();
  }

  @Override
  public void addEditorPositionListener(@Nonnull FileEditorPositionListener listener) {
    myModel.addEditorPositionListener(listener);
  }

  @Override
  public void removeEditorPositionListener(@Nonnull FileEditorPositionListener listener) {
    myModel.removeEditorPositionListener(listener);
  }

  @Override
  public void dispose() {
    Disposer.dispose(myModel);
  }

  @Override
  public boolean shouldEnterElement(final Object element) {
    return false;
  }

  @Override
  public void addModelListener(@Nonnull ModelListener modelListener) {
    myModel.addModelListener(modelListener);
  }

  @Override
  public void removeModelListener(@Nonnull ModelListener modelListener) {
    myModel.removeModelListener(modelListener);
  }

  public StructureViewModel getModel() {
    return myModel;
  }

  @Override
  public boolean isEnabled(@Nonnull NodeProvider provider) {
    return myStructureView.isActionActive(provider.getName());
  }
}
