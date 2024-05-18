// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.AppUIExecutor;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class ServiceSingleView extends ServiceView {
  private final AtomicReference<ServiceViewItem> myRef = new AtomicReference<>();
  private boolean mySelected;
  private final ServiceViewModel.ServiceViewModelListener myListener;

  ServiceSingleView(@Nonnull Project project, @Nonnull ServiceViewModel model, @Nonnull ServiceViewUi ui) {
    super(new BorderLayout(), project, model, ui);
    ui.setServiceToolbar(ServiceViewActionProvider.getInstance());
    add(ui.getComponent(), BorderLayout.CENTER);
    myListener = this::updateItem;
    model.addModelListener(myListener);
    model.getInvoker().invokeLater(this::updateItem);
  }

  @Nonnull
  @Override
  Promise<Void> select(@Nonnull Object service,
                       @Nonnull Class<?> contributorClass) {
    ServiceViewItem item = myRef.get();
    if (item == null || !item.getValue().equals(service)) {
      return Promises.rejectedPromise("Service not found");
    }

    showContent();
    return Promises.resolvedPromise();
  }

  @Override
  Promise<Void> expand(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
    return matches(service);
  }

  @Override
  Promise<Void> extract(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
    return matches(service);
  }

  private Promise<Void> matches(@Nonnull Object service) {
    ServiceViewItem item = myRef.get();
    return item == null || !item.getValue().equals(service) ? Promises.rejectedPromise("Service not found") : Promises.resolvedPromise();
  }

  @Override
  void onViewSelected() {
    showContent();
  }

  @Override
  void onViewUnselected() {
    mySelected = false;
    ServiceViewItem item = myRef.get();
    if (item != null) {
      item.getViewDescriptor().onNodeUnselected();
    }
  }

  @Nonnull
  @Override
  List<ServiceViewItem> getSelectedItems() {
    ServiceViewItem item = myRef.get();
    return ContainerUtil.createMaybeSingletonList(item);
  }

  @Override
  public void jumpToServices() {
  }

  @Override
  public void dispose() {
    getModel().removeModelListener(myListener);
  }

  private void updateItem() {
    WeakReference<ServiceViewItem> oldValueRef = new WeakReference<>(myRef.get());
    ServiceViewItem newValue = ContainerUtil.getOnlyItem(getModel().getRoots());
    WeakReference<ServiceViewItem> newValueRef = new WeakReference<>(newValue);
    myRef.set(newValue);
    AppUIExecutor.onUiThread().expireWith(this).submit(() -> {
      if (mySelected) {
        ServiceViewItem value = newValueRef.get();
        if (value != null) {
          ServiceViewDescriptor descriptor = value.getViewDescriptor();
          if (oldValueRef.get() == null) {
            onViewSelected(descriptor);
          }
          myUi.setDetailsComponent(descriptor.getContentComponent());
        }
      }
    });
  }

  private void showContent() {
    if (mySelected) return;

    mySelected = true;
    ServiceViewItem item = myRef.get();
    if (item != null) {
      ServiceViewDescriptor descriptor = item.getViewDescriptor();
      onViewSelected(descriptor);

      myUi.setDetailsComponent(descriptor.getContentComponent());
    }
  }

  @Override
  List<Object> getChildrenSafe(@Nonnull List<Object> valueSubPath, @Nonnull Class<?> contributorClass) {
    return Collections.emptyList();
  }
}
