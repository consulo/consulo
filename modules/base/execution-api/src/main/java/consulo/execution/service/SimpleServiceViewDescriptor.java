// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.navigation.ItemPresentation;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class SimpleServiceViewDescriptor implements ServiceViewDescriptor {
  private final ItemPresentation myPresentation;
  private final String myId;

  public SimpleServiceViewDescriptor(@Nonnull String name, @Nullable Image icon) {
    this(name, icon, name);
  }

  public SimpleServiceViewDescriptor(@Nonnull String name, @Nullable Image icon, @Nonnull String id) {
    myPresentation = new PresentationData(name, null, icon, null);
    myId = id;
  }

  @Override
  public @Nonnull ItemPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public @Nonnull String getId() {
    return myId;
  }
}
