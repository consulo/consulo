// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.navigation.ItemPresentation;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class SimpleServiceViewDescriptor implements ServiceViewDescriptor {
  private final ItemPresentation myPresentation;
  private final String myId;

  public SimpleServiceViewDescriptor(String name, @Nullable Image icon) {
    this(name, icon, name);
  }

  public SimpleServiceViewDescriptor(String name, @Nullable Image icon, String id) {
    myPresentation = new PresentationData(name, null, icon, null);
    myId = id;
  }

  @Override
  public ItemPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public String getId() {
    return myId;
  }
}
