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
package consulo.desktop.awt.internal.diff.util;

import consulo.diff.internal.DiffImplUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.AbstractLayoutManager;
import consulo.ui.ex.awt.Wrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class DiffContentPanel extends JPanel {
  //@Nullable
  //private DiffBreadcrumbsPanel myBreadcrumbs;

  private final Wrapper myTitle = new InvisibleWrapper();
  private final Wrapper myTopBreadcrumbs = new InvisibleWrapper();
  private final JComponent myContent;
  private final Wrapper myBottomBreadcrumbs = new InvisibleWrapper();

  public DiffContentPanel(@Nonnull JComponent content) {
    myContent = content;

    initLayout(this, myTitle, myTopBreadcrumbs, myContent, myBottomBreadcrumbs);
  }

  public void setTitle(@Nullable JComponent titles) {
    myTitle.setContent(titles);
  }

  //public void setBreadcrumbs(@Nullable DiffBreadcrumbsPanel breadcrumbs) {
  //  if (breadcrumbs != null) {
  //    myBreadcrumbs = breadcrumbs;
  //  }
  //}
  //
  //public void updateBreadcrumbsPlacement(@Nonnull BreadcrumbsPlacement placement) {
  //  if (myBreadcrumbs == null) return;
  //
  //  myTopBreadcrumbs.setContent(placement == BreadcrumbsPlacement.TOP ? myBreadcrumbs : null);
  //  myBottomBreadcrumbs.setContent(placement == BreadcrumbsPlacement.BOTTOM ? myBreadcrumbs : null);
  //  myBreadcrumbs.setCrumbsShown(placement != BreadcrumbsPlacement.HIDDEN);
  //
  //  validate();
  //  repaint();
  //}

  private static void initLayout(@Nonnull DiffContentPanel contentPanel,
                                 @Nonnull JComponent title,
                                 @Nonnull JComponent topBreadcrumbs,
                                 @Nonnull JComponent content,
                                 @Nonnull JComponent bottomBreadcrumbs) {
    contentPanel.removeAll();
    contentPanel.setLayout(new DiffContentLayout(title, topBreadcrumbs, content, bottomBreadcrumbs));
    contentPanel.add(title);
    contentPanel.add(topBreadcrumbs);
    contentPanel.add(content);
    contentPanel.add(bottomBreadcrumbs);
  }

  public static void syncTitleHeights(@Nonnull List<DiffContentPanel> panels) {
    List<JComponent> titles = ContainerUtil.map(panels, it -> it.myTitle);
    List<JComponent> topBreadcrumbs = ContainerUtil.map(panels, it -> it.myTopBreadcrumbs);

    List<JComponent> syncTitles = AWTDiffUtil.createSyncHeightComponents(titles);
    List<JComponent> syncTopBreadcrumbs = AWTDiffUtil.createSyncHeightComponents(topBreadcrumbs);

    for (int i = 0; i < panels.size(); i++) {
      DiffContentPanel contentPanel = panels.get(i);
      JComponent title = syncTitles.get(i);
      JComponent topBreadcrumb = syncTopBreadcrumbs.get(i);
      initLayout(contentPanel, title, topBreadcrumb, contentPanel.myContent, contentPanel.myBottomBreadcrumbs);
    }
  }

  private static class DiffContentLayout extends AbstractLayoutManager {
    @Nonnull
    private final JComponent myTitle;
    @Nonnull
    private final JComponent myTopBreadcrumbs;
    @Nonnull
    private final JComponent myContent;
    @Nonnull
    private final JComponent myBottomBreadcrumbs;

    DiffContentLayout(@Nonnull JComponent title,
                      @Nonnull JComponent topBreadcrumbs,
                      @Nonnull JComponent content,
                      @Nonnull JComponent bottomBreadcrumbs) {
      myTitle = title;
      myTopBreadcrumbs = topBreadcrumbs;
      myContent = content;
      myBottomBreadcrumbs = bottomBreadcrumbs;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      int totalWidth = 0;
      int totalHeight = 0;

      for (JComponent component : Arrays.asList(myTitle, myTopBreadcrumbs, myContent, myBottomBreadcrumbs)) {
        Dimension size = getPreferredSize(component);

        totalWidth = Math.max(size.width, totalWidth);
        totalHeight += size.height;

        if (component == myTitle && size.height != 0) {
          totalHeight += DiffImplUtil.TITLE_GAP.get();
        }
      }

      return new Dimension(totalWidth, totalHeight);
    }

    @Override
    public void layoutContainer(@Nonnull Container parent) {
      int y = 0;

      int width = parent.getWidth();
      int totalHeight = parent.getHeight();

      Dimension titleSize = getPreferredSize(myTitle);
      Dimension topSize = getPreferredSize(myTopBreadcrumbs);
      Dimension bottomSize = getPreferredSize(myBottomBreadcrumbs);
      int bottomY = totalHeight - bottomSize.height;

      myTitle.setBounds(0, y, width, titleSize.height);
      y += titleSize.height;
      if (titleSize.height != 0) y += DiffImplUtil.TITLE_GAP.get();

      myTopBreadcrumbs.setBounds(0, y, width, topSize.height);
      y += topSize.height;

      myContent.setBounds(0, y, width, Math.max(0, bottomY - y));

      myBottomBreadcrumbs.setBounds(0, bottomY, width, bottomSize.height);
    }

    @Nonnull
    private static Dimension getPreferredSize(@Nonnull JComponent component) {
      return component.isVisible() ? component.getPreferredSize() : new Dimension();
    }
  }
}
