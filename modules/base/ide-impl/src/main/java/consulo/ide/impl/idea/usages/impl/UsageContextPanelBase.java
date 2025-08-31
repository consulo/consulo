/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.usages.impl;

import consulo.application.AppUIExecutor;
import consulo.project.Project;
import consulo.ui.ex.awt.JBPanelWithEmptyText;
import consulo.usage.UsageInfo;
import consulo.usage.UsageContextPanel;
import consulo.usage.UsageViewPresentation;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author cdr
 */
public abstract class UsageContextPanelBase extends JBPanelWithEmptyText implements UsageContextPanel {
  protected final Project myProject;
  @Nonnull
  protected final UsageViewPresentation myPresentation;
  protected volatile boolean isDisposed;

  public UsageContextPanelBase(@Nonnull Project project, @Nonnull UsageViewPresentation presentation) {
    myProject = project;
    myPresentation = presentation;
    setLayout(new BorderLayout());
    setBorder(JBUI.Borders.empty());
  }

  @Nonnull
  @Override
  public final JComponent createComponent() {
    isDisposed = false;
    return this;
  }

  @Override
  public void dispose() {
    isDisposed = true;
  }

  @Override
  public final void updateLayout(@Nullable List<? extends UsageInfo> infos) {
    AppUIExecutor.onUiThread().withDocumentsCommitted(myProject).expireWith(this).execute(() -> updateLayoutLater(infos));
  }

  protected abstract void updateLayoutLater(@Nullable List<? extends UsageInfo> infos);
}
