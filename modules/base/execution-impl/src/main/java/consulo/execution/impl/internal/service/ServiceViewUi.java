// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

interface ServiceViewUi {
  @Nonnull
  JComponent getComponent();

  void saveState(@Nonnull ServiceViewState state);

  void setServiceToolbar(@Nonnull ServiceViewActionProvider actionManager);

  void setMasterComponent(@Nonnull JComponent component, @Nonnull ServiceViewActionProvider actionManager);

  void setDetailsComponent(@Nullable JComponent component);

  void setMasterComponentVisible(boolean visible);

  @Nullable
  JComponent getDetailsComponent();
}
