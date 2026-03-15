// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.service;

import org.jspecify.annotations.Nullable;

import javax.swing.*;

interface ServiceViewUi {
  
  JComponent getComponent();

  void saveState(ServiceViewState state);

  void setServiceToolbar(ServiceViewActionProvider actionManager);

  void setMasterComponent(JComponent component, ServiceViewActionProvider actionManager);

  void setDetailsComponent(@Nullable JComponent component);

  void setMasterComponentVisible(boolean visible);

  @Nullable
  JComponent getDetailsComponent();
}
