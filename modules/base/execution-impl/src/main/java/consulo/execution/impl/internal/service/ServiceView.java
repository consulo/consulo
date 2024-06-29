// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.HelpManager;
import consulo.application.util.RecursionManager;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.service.*;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ServiceView extends JPanel implements Disposable {
  private final Project myProject;
  private final ServiceViewModel myModel;
  protected final ServiceViewUi myUi;
  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

  protected ServiceView(LayoutManager layout, @Nonnull Project project, @Nonnull ServiceViewModel model, @Nonnull ServiceViewUi ui) {
    super(layout);
    myProject = project;
    myModel = model;
    myUi = ui;
  }

  @Override
  public void dispose() {
  }

  Project getProject() {
    return myProject;
  }

  public ServiceViewModel getModel() {
    return myModel;
  }

  ServiceViewUi getUi() {
    return myUi;
  }

  void saveState(@Nonnull ServiceViewState state) {
    myModel.saveState(state);
  }

  @Nonnull
  abstract List<ServiceViewItem> getSelectedItems();

  abstract Promise<Void> select(@Nonnull Object service, @Nonnull Class<?> contributorClass);

  abstract Promise<Void> expand(@Nonnull Object service, @Nonnull Class<?> contributorClass);

  abstract Promise<Void> extract(@Nonnull Object service, @Nonnull Class<?> contributorClass);

  abstract void onViewSelected();

  abstract void onViewUnselected();

  public boolean isGroupByServiceGroups() {
    return myModel.isGroupByServiceGroups();
  }

  public void setGroupByServiceGroups(boolean value) {
    myModel.setGroupByServiceGroups(value);
  }

  public boolean isGroupByContributor() {
    return myModel.isGroupByContributor();
  }

  public void setGroupByContributor(boolean value) {
    myModel.setGroupByContributor(value);
  }

  abstract List<Object> getChildrenSafe(@Nonnull List<Object> valueSubPath, @Nonnull Class<?> contributorClass);

  void setAutoScrollToSourceHandler(@Nonnull AutoScrollToSourceHandler autoScrollToSourceHandler) {
    myAutoScrollToSourceHandler = autoScrollToSourceHandler;
  }

  void onViewSelected(@Nonnull ServiceViewDescriptor descriptor) {
    descriptor.onNodeSelected(ContainerUtil.map(getSelectedItems(), ServiceViewItem::getValue));
    if (myAutoScrollToSourceHandler != null) {
      myAutoScrollToSourceHandler.onMouseClicked(this);
    }
  }

  public abstract void jumpToServices();

  static ServiceView createView(@Nonnull Project project, @Nonnull ServiceViewModel viewModel, @Nonnull ServiceViewState viewState) {
    setViewModelState(viewModel, viewState);
    ServiceView serviceView = createTreeView(project, viewModel, viewState);
    setDataProvider(serviceView);
    return serviceView;
  }

  private static ServiceView createTreeView(@Nonnull Project project, @Nonnull ServiceViewModel model, @Nonnull ServiceViewState state) {
    return new ServiceTreeView(project, model, new ServiceViewTreeUi(state), state);
  }

  private static void setDataProvider(ServiceView serviceView) {
    ServiceViewOptions viewOptions = new ServiceViewOptions() {
      @Override
      public boolean isGroupByContributor() {
        return serviceView.isGroupByContributor();
      }

      @Override
      public boolean isGroupByServiceGroups() {
        return serviceView.isGroupByServiceGroups();
      }
    };
    DataManager.registerDataProvider(serviceView, dataId -> {
      if (HelpManager.HELP_ID.is(dataId)) {
        return ServiceViewManagerImpl.getToolWindowContextHelpId();
      }
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(serviceView.getSelectedItems(), ServiceViewItem::getValue);
      }
      if (PlatformDataKeys.SELECTED_ITEM.is(dataId)) {
        ServiceViewItem item = ContainerUtil.getOnlyItem(serviceView.getSelectedItems());
        return item != null ? item.getValue() : null;
      }
      if (ServiceViewActionProvider.SERVICES_SELECTED_ITEMS.is(dataId)) {
        return serviceView.getSelectedItems();
      }
      if (DeleteProvider.KEY.is(dataId)) {
        List<ServiceViewItem> selection = serviceView.getSelectedItems();
        ServiceViewContributor<?> contributor = ServiceViewDragHelper.getTheOnlyRootContributor(selection);
        DataProvider delegate = contributor == null ? null : contributor.getViewDescriptor(serviceView.getProject()).getDataProvider();
        DeleteProvider deleteProvider = delegate == null ? null : delegate.getDataUnchecked(DeleteProvider.KEY);
        if (deleteProvider == null) return new ServiceViewDeleteProvider(serviceView);

        if (deleteProvider instanceof ServiceViewContributorDeleteProvider) {
          ((ServiceViewContributorDeleteProvider)deleteProvider).setFallbackProvider(new ServiceViewDeleteProvider(serviceView));
        }
        return deleteProvider;
      }
      if (CopyProvider.KEY.is(dataId)) {
        return new ServiceViewCopyProvider(serviceView);
      }
      if (ServiceViewActionUtils.CONTRIBUTORS_KEY.is(dataId)) {
        return serviceView.getModel().getRoots().stream().map(ServiceViewItem::getRootContributor).collect(Collectors.toSet());
      }
      if (ServiceViewActionUtils.OPTIONS_KEY.is(dataId)) {
        return viewOptions;
      }
      List<ServiceViewItem> selectedItems = serviceView.getSelectedItems();
      if (Navigatable.KEY_OF_ARRAY.is(dataId)) {
        List<Navigatable> navigatables = ContainerUtil.mapNotNull(selectedItems, item -> item.getViewDescriptor().getNavigatable());
        return navigatables.toArray(Navigatable.EMPTY_ARRAY);
      }
      ServiceViewItem selectedItem = ContainerUtil.getOnlyItem(selectedItems);
      ServiceViewDescriptor descriptor = selectedItem == null || selectedItem.isRemoved() ? null : selectedItem.getViewDescriptor();
      DataProvider dataProvider = descriptor == null ? null : descriptor.getDataProvider();
      if (dataProvider != null) {
        return RecursionManager.doPreventingRecursion(serviceView, false, () -> dataProvider.getData(dataId));
      }
      return null;
    });
  }

  private static void setViewModelState(@Nonnull ServiceViewModel viewModel, @Nonnull ServiceViewState viewState) {
    viewModel.setGroupByServiceGroups(viewState.groupByServiceGroups);
    viewModel.setGroupByContributor(viewState.groupByContributor);
  }
}