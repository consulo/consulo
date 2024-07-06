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
import consulo.desktop.awt.internal.diff.util.DiffSplitter;
import consulo.diff.util.Side;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TwosideContentPanel extends JPanel {
  @Nonnull
  private final DiffSplitter mySplitter;
  @Nonnull
  private final List<DiffContentPanel> myPanels;

  public TwosideContentPanel(@Nonnull List<? extends JComponent> contents) {
    super(new BorderLayout());
    assert contents.size() == 2;

    myPanels = ContainerUtil.map(contents, DiffContentPanel::new);
    DiffContentPanel.syncTitleHeights(myPanels);

    mySplitter = new DiffSplitter();
    mySplitter.setFirstComponent(Side.LEFT.select(myPanels));
    mySplitter.setSecondComponent(Side.RIGHT.select(myPanels));
    mySplitter.setHonorComponentsMinimumSize(false);
    add(mySplitter, BorderLayout.CENTER);
  }

  public void setTitles(@Nonnull List<JComponent> titleComponents) {
    for (Side side : Side.values()) {
      DiffContentPanel panel = side.select(myPanels);
      JComponent title = side.select(titleComponents);
      panel.setTitle(title);
    }
  }

  //public void setBreadcrumbs(@Nonnull Side side, @Nullable DiffBreadcrumbsPanel breadcrumbs, @Nonnull TextDiffSettings settings) {
  //  if (breadcrumbs != null) {
  //    DiffContentPanel panel = side.select(myPanels);
  //    panel.setBreadcrumbs(breadcrumbs);
  //    panel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //    settings.addListener(new TextDiffSettings.Listener.Adapter() {
  //      @Override
  //      public void breadcrumbsPlacementChanged() {
  //        panel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //        repaintDivider();
  //      }
  //    }, breadcrumbs);
  //  }
  //}

  public void setBottomAction(@Nullable AnAction value) {
    mySplitter.setBottomAction(value);
  }

  public void setTopAction(@Nullable AnAction value) {
    mySplitter.setTopAction(value);
  }

  @RequiredUIAccess
  public void setPainter(@Nullable DiffSplitter.Painter painter) {
    mySplitter.setPainter(painter);
  }

  public void repaintDivider() {
    mySplitter.repaintDivider();
  }

  @Nonnull
  public DiffSplitter getSplitter() {
    return mySplitter;
  }

  @Nonnull
  public static TwosideContentPanel createFromHolders(@Nonnull List<? extends EditorHolder> holders) {
    return new TwosideContentPanel(ContainerUtil.map(holders, holder -> holder.getComponent()));
  }
}
