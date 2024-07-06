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

import consulo.codeEditor.EditorEx;
import consulo.desktop.awt.internal.diff.EditorHolder;
import consulo.desktop.awt.internal.diff.TextEditorHolder;
import consulo.desktop.awt.internal.diff.util.DiffContentPanel;
import consulo.desktop.awt.internal.diff.util.DiffSplitter;
import consulo.desktop.awt.internal.diff.util.ThreeDiffSplitter;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ThreesideContentPanel extends JPanel {
  @Nonnull
  private final ThreeDiffSplitter mySplitter;
  @Nonnull
  private final List<DiffContentPanel> myPanels;

  public ThreesideContentPanel(@Nonnull List<? extends JComponent> contents) {
    super(new BorderLayout());
    assert contents.size() == 3;

    myPanels = ContainerUtil.map(contents, it -> new DiffContentPanel(it));
    DiffContentPanel.syncTitleHeights(myPanels);

    mySplitter = new ThreeDiffSplitter(myPanels);
    add(mySplitter, BorderLayout.CENTER);
  }

  public void setTitles(@Nonnull List<JComponent> titleComponents) {
    for (ThreeSide side : ThreeSide.values()) {
      DiffContentPanel panel = side.select(myPanels);
      JComponent title = side.select(titleComponents);
      panel.setTitle(title);
    }
  }

  //public void setBreadcrumbs(@Nonnull ThreeSide side, @Nullable DiffBreadcrumbsPanel breadcrumbs, @Nonnull TextDiffSettings settings) {
  //  if (breadcrumbs != null) {
  //    DiffContentPanel panel = side.select(myPanels);
  //    panel.setBreadcrumbs(breadcrumbs);
  //    panel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //    settings.addListener(new TextDiffSettings.Listener.Adapter() {
  //      @Override
  //      public void breadcrumbsPlacementChanged() {
  //        panel.updateBreadcrumbsPlacement(settings.getBreadcrumbsPlacement());
  //        repaintDividers();
  //      }
  //    }, breadcrumbs);
  //  }
  //}

  @RequiredUIAccess
  public void setPainter(@Nullable DiffSplitter.Painter painter, @Nonnull Side side) {
    mySplitter.setPainter(painter, side);
  }

  public void repaintDividers() {
    repaintDivider(Side.LEFT);
    repaintDivider(Side.RIGHT);
  }

  public void repaintDivider(@Nonnull Side side) {
    mySplitter.repaintDivider(side);
  }

  public static class Holders extends ThreesideContentPanel {
    @Nullable
    private final EditorEx myBaseEditor;

    public Holders(@Nonnull List<? extends EditorHolder> holders) {
      super(ContainerUtil.map(holders, holder -> holder.getComponent()));


      EditorHolder baseHolder = ThreeSide.BASE.select(holders);
      myBaseEditor = baseHolder instanceof TextEditorHolder ? ((TextEditorHolder)baseHolder).getEditor() : null;
    }

    @Override
    public void repaintDivider(@Nonnull Side side) {
      if (side == Side.RIGHT && myBaseEditor != null) {
        myBaseEditor.getScrollPane().getVerticalScrollBar().repaint();
      }
      super.repaintDivider(side);
    }
  }
}
