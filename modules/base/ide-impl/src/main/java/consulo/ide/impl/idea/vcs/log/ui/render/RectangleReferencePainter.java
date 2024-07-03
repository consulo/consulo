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
package consulo.ide.impl.idea.vcs.log.ui.render;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.paint.PaintParameters;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.style.StyleManager;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.VcsLogRefManager;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsRefType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RectangleReferencePainter implements ReferencePainter {
  @Nonnull
  private List<Pair<String, Color>> myLabels = new ArrayList<>();
  private int myHeight = JBUI.scale(22);
  private int myWidth = 0;

  private final RectanglePainter myLabelPainter = new RectanglePainter(false) {
    @Override
    protected Font getLabelFont() {
      return getReferenceFont();
    }
  };

  @Override
  public void customizePainter(
    @Nonnull JComponent component,
    @Nonnull Collection<VcsRef> references,
    @Nullable VcsLogRefManager manager,
    @Nonnull Color background,
    @Nonnull Color foreground
  ) {
    FontMetrics metrics = component.getFontMetrics(getReferenceFont());
    myHeight = metrics.getHeight() + RectanglePainter.TOP_TEXT_PADDING + RectanglePainter.BOTTOM_TEXT_PADDING;
    myWidth = 2 * PaintParameters.LABEL_PADDING;

    myLabels = new ArrayList<>();
    if (manager == null) return;

    List<VcsRef> sorted = ContainerUtil.sorted(references, manager.getLabelsOrderComparator());

    for (Map.Entry<VcsRefType, Collection<VcsRef>> entry : ContainerUtil.groupBy(sorted, VcsRef::getType).entrySet()) {
      VcsRef ref = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(entry.getValue()));
      String text = ref.getName() + (entry.getValue().size() > 1 ? " +" : "");
      myLabels.add(Pair.create(text, entry.getKey().getBackgroundColor()));

      myWidth += myLabelPainter.calculateSize(text, metrics).getWidth() + PaintParameters.LABEL_PADDING;
    }
  }

  public void paint(@Nonnull Graphics2D g2, int x, int y, int height) {
    if (myLabels.isEmpty()) return;

    GraphicsConfig config = GraphicsUtil.setupAAPainting(g2);
    g2.setFont(getReferenceFont());
    g2.setStroke(new BasicStroke(1.5f));

    FontMetrics fontMetrics = g2.getFontMetrics();

    x += PaintParameters.LABEL_PADDING;
    for (Pair<String, Color> label : myLabels) {
      Dimension size = myLabelPainter.calculateSize(label.first, fontMetrics);
      int paddingY = y + (height - size.height) / 2;
      myLabelPainter.paint(g2, label.first, x, paddingY, getLabelColor(label.second));
      x += size.width + PaintParameters.LABEL_PADDING;
    }

    config.restore();
  }

  @Nonnull
  public static Color getLabelColor(@Nonnull Color color) {
    color = StyleManager.get().getCurrentStyle().isDark() ? ColorUtil.darker(color, 6) : ColorUtil.brighter(color, 6);
    return ColorUtil.desaturate(color, 3);
  }

  @Override
  public Dimension getSize() {
    if (myLabels.isEmpty()) return new Dimension();
    return new Dimension(myWidth, myHeight);
  }

  @Override
  public boolean isLeftAligned() {
    return true;
  }
}
