// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.util.ColoredItem;
import consulo.execution.service.ServiceViewContributor;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewItemState;
import consulo.execution.service.ServiceViewOptions;
import consulo.navigation.ItemPresentation;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ServiceViewItem implements ColoredItem {
  private final Object myValue;
  private volatile ServiceViewItem myParent;
  private final ServiceViewContributor<?> myContributor;
  private ServiceViewDescriptor myViewDescriptor;
  final List<ServiceViewItem> myChildren = new CopyOnWriteArrayList<>();
  private volatile boolean myPresentationUpdated;
  private volatile boolean myRemoved;
  private PresentationData myPresentation;

  ServiceViewItem(@Nonnull Object value, @Nullable ServiceViewItem parent, @Nonnull ServiceViewContributor<?> contributor,
                  @Nonnull ServiceViewDescriptor viewDescriptor) {
    myValue = value;
    myParent = parent;
    myContributor = contributor;
    myViewDescriptor = viewDescriptor;
  }

  @Nonnull
  Object getValue() {
    return myValue;
  }

  @Nonnull
  ServiceViewContributor<?> getContributor() {
    return myContributor;
  }

  @Nonnull
  ServiceViewContributor<?> getRootContributor() {
    return myParent == null ? myContributor : myParent.getRootContributor();
  }

  @Nonnull
  ServiceViewDescriptor getViewDescriptor() {
    if (!myPresentationUpdated) {
      myPresentationUpdated = true;
      if (myValue instanceof NodeDescriptor) {
        ((NodeDescriptor<?>)myValue).update();
      }
    }
    return myViewDescriptor;
  }

  void setViewDescriptor(@Nonnull ServiceViewDescriptor viewDescriptor) {
    AppUIUtil.invokeOnEdt(() -> {
      myViewDescriptor = viewDescriptor;
      myPresentationUpdated = false;
    });
  }

  @Nullable
  ServiceViewItem getParent() {
    return myParent;
  }

  void setParent(@Nullable ServiceViewItem parent) {
    myParent = parent;
  }

  @Nonnull
  List<ServiceViewItem> getChildren() {
    return myChildren;
  }

  @Nullable
  @Override
  public ColorValue getColor() {
    ServiceViewDescriptor descriptor = getViewDescriptor();
    return descriptor instanceof ColoredItem ? ((ColoredItem)descriptor).getColor() : null;
  }

  void markRemoved() {
    myRemoved = true;
  }

  boolean isRemoved() {
    return myRemoved || myParent != null && myParent.isRemoved();
  }

  ItemPresentation getItemPresentation(@Nullable ServiceViewOptions viewOptions, @Nonnull ServiceViewItemState state) {
    if (isRemoved()) return myPresentation;

    ItemPresentation presentation =
      viewOptions == null ? getViewDescriptor().getPresentation() : getViewDescriptor().getCustomPresentation(viewOptions, state);
    myPresentation = presentation instanceof PresentationData ?
      (PresentationData)presentation :
      new PresentationData(presentation.getPresentableText(),
                           presentation.getLocationString(),
                           presentation.getIcon(false),
                           null);
    return myPresentation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceViewItem node = (ServiceViewItem)o;
    return myValue.equals(node.myValue);
  }

  @Override
  public int hashCode() {
    return myValue.hashCode();
  }

  @Override
  public String toString() {
    return myValue.toString();
  }
}
