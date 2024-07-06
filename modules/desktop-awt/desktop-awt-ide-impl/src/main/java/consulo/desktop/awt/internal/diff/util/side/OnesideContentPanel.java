/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.util.side;

import consulo.desktop.awt.internal.diff.EditorHolder;
import consulo.desktop.awt.internal.diff.util.DiffContentPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class OnesideContentPanel extends JPanel {
  private final DiffContentPanel myPanel;

  public OnesideContentPanel(@Nonnull JComponent content) {
    super(new BorderLayout());

    myPanel = new DiffContentPanel(content);
    add(myPanel, BorderLayout.CENTER);
  }

  public void setTitle(@Nullable JComponent titles) {
    myPanel.setTitle(titles);
  }

  //public void setBreadcrumbs(@Nullable DiffBreadcrumbsPanel breadcrumbs, @Nonnull TextDiffSettings settings) {
  //  if (breadcrumbs != null) {
  //    myPanel.setBreadcrumbs(breadcrumbs);
  //    myPanel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //    settings.addListener(new TextDiffSettings.Listener.Adapter() {
  //      @Override
  //      public void breadcrumbsPlacementChanged() {
  //        myPanel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //      }
  //    }, breadcrumbs);
  //  }
  //}

  @Nonnull
  public static OnesideContentPanel createFromHolder(@Nonnull EditorHolder holder) {
    return new OnesideContentPanel(holder.getComponent());
  }
}
