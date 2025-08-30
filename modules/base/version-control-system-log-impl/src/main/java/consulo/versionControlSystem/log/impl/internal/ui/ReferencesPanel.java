/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.history.VcsHistoryUtil;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsRefType;
import consulo.versionControlSystem.log.impl.internal.ui.render.LabelIcon;
import consulo.versionControlSystem.log.impl.internal.ui.render.RectanglePainter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static consulo.versionControlSystem.log.impl.internal.ui.CommitPanel.getCommitDetailsBackground;

public class ReferencesPanel extends JPanel {
  public static final int H_GAP = 4;
  protected static final int V_GAP = 0;
  public static final int PADDING = 3;

  private final int myRefsLimit;
  @Nonnull
  private List<VcsRef> myReferences;
  @Nonnull
  protected MultiMap<VcsRefType, VcsRef> myGroupedVisibleReferences;

  public ReferencesPanel() {
    this(new WrappedFlowLayout(JBUI.scale(H_GAP), JBUI.scale(V_GAP)), -1);
  }

  public ReferencesPanel(LayoutManager layout, int limit) {
    super(layout);
    myRefsLimit = limit;
    myReferences = Collections.emptyList();
    myGroupedVisibleReferences = MultiMap.create();
    setOpaque(false);
  }

  public void setReferences(@Nonnull List<VcsRef> references) {
    myReferences = references;

    List<VcsRef> visibleReferences = (myRefsLimit > 0) ? myReferences.subList(0, Math.min(myReferences.size(), myRefsLimit)) : myReferences;
    myGroupedVisibleReferences = ContainerUtil.groupBy(visibleReferences, VcsRef::getType);

    update();
  }

  public void update() {
    removeAll();
    int height = getIconHeight();
    JBLabel firstLabel = null;

    for (Map.Entry<VcsRefType, Collection<VcsRef>> typeAndRefs : myGroupedVisibleReferences.entrySet()) {
      VcsRefType type = typeAndRefs.getKey();
      Collection<VcsRef> refs = typeAndRefs.getValue();
      int refIndex = 0;
      for (VcsRef reference : refs) {
        Image icon = createIcon(type, refs, refIndex, height);
        String ending = (refIndex != refs.size() - 1) ? "," : "";
        String text = reference.getName() + ending;
        JBLabel label = createLabel(text, icon);
        if (firstLabel == null) {
          firstLabel = label;
          add(label);
        }
        else {
          addWrapped(label, firstLabel);
        }
        refIndex++;
      }
    }
    if (getHiddenReferencesSize() > 0) {
      JBLabel label = createRestLabel(getHiddenReferencesSize());
      addWrapped(label, ObjectUtil.assertNotNull(firstLabel));
    }
    setVisible(!myGroupedVisibleReferences.isEmpty());
    revalidate();
    repaint();
  }

  private int getHiddenReferencesSize() {
    return (myRefsLimit > 0) ? myReferences.size() - Math.min(myReferences.size(), myRefsLimit) : 0;
  }

  protected int getIconHeight() {
    return getFontMetrics(getLabelsFont()).getHeight() + JBUI.scale(PADDING);
  }

  @Nonnull
  protected JBLabel createRestLabel(int restSize) {
    return createLabel("... " + restSize + " more", null);
  }

  @Nullable
  protected Image createIcon(@Nonnull VcsRefType type,
                            @Nonnull Collection<VcsRef> refs,
                            int refIndex, int height) {
    if (refIndex == 0) {
      Color color = type.getBackgroundColor();
      return new LabelIcon(height, getBackground(),
                           refs.size() > 1 ? new Color[]{color, color} : new Color[]{color});
    }
    return null;
  }

  private void addWrapped(@Nonnull JBLabel label, @Nonnull JBLabel referent) {
    Wrapper wrapper = new Wrapper(label);
    wrapper.setVerticalSizeReferent(referent);
    add(wrapper);
  }

  @Nonnull
  protected JBLabel createLabel(@Nonnull String text, @Nullable Image icon) {
    JBLabel label = new JBLabel(text, icon, SwingConstants.LEFT);
    label.setFont(getLabelsFont());
    label.setIconTextGap(0);
    label.setHorizontalAlignment(SwingConstants.LEFT);
    return label;
  }

  @Nonnull
  protected Font getLabelsFont() {
    return VcsHistoryUtil.getCommitDetailsFont();
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
  }

  @Override
  public Color getBackground() {
    return getCommitDetailsBackground();
  }

  @Nonnull
  public static Color getLabelColor(@Nonnull Color color) {
    color = StyleManager.get().getCurrentStyle().isDark() ? ColorUtil.darker(color, 6) : ColorUtil.brighter(color, 6);
    return ColorUtil.desaturate(color, 3);
  }

  private static class ReferencePanel extends JPanel {
    @Nonnull
    private final RectanglePainter myLabelPainter;
    @Nonnull
    private final VcsRef myReference;

    private ReferencePanel(@Nonnull VcsRef reference) {
      myReference = reference;
      myLabelPainter = new RectanglePainter(false);
      setOpaque(false);
    }

    @Override
    public void paint(Graphics g) {
      myLabelPainter.paint((Graphics2D)g, myReference.getName(), 0, 0,
                           getLabelColor(myReference.getType().getBackgroundColor()));
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension dimension = myLabelPainter.calculateSize(myReference.getName(), getFontMetrics(RectanglePainter.getFont()));
      return new Dimension(dimension.width, dimension.height + JBUI.scale(PADDING));
    }

    @Override
    public Color getBackground() {
      return getCommitDetailsBackground();
    }
  }
}
