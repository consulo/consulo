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
package consulo.versionControlSystem.log.impl.internal.ui.render;

import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsRefType;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.ui.ReferencesPanel;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Map;

class TooltipReferencesPanel extends ReferencesPanel {
  private static final int REFS_LIMIT = 10;
  @Nonnull
  private final LabelPainter myReferencePainter;
  private boolean myHasGroupWithMultipleRefs;

  public TooltipReferencesPanel(@Nonnull VcsLogDataImpl logData,
                                @Nonnull LabelPainter referencePainter,
                                @Nonnull Collection<VcsRef> refs) {
    super(new VerticalFlowLayout(JBUI.scale(ReferencesPanel.H_GAP), JBUI.scale(ReferencesPanel.V_GAP)), REFS_LIMIT);
    myReferencePainter = referencePainter;

    VirtualFile root = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(refs)).getRoot();
    setReferences(ContainerUtil.sorted(refs, logData.getLogProvider(root).getReferenceManager().getLabelsOrderComparator()));
  }

  @Override
  public void update() {
    myHasGroupWithMultipleRefs = false;
    for (Map.Entry<VcsRefType, Collection<VcsRef>> typeAndRefs : myGroupedVisibleReferences.entrySet()) {
      if (typeAndRefs.getValue().size() > 1) {
        myHasGroupWithMultipleRefs = true;
      }
    }
    super.update();
  }

  @Nonnull
  @Override
  protected Font getLabelsFont() {
    return myReferencePainter.getReferenceFont();
  }

  @Nullable
  @Override
  protected Image createIcon(@Nonnull VcsRefType type, @Nonnull Collection<VcsRef> refs, int refIndex, int height) {
    if (refIndex == 0) {
      Color color = type.getBackgroundColor();
      return new LabelIcon(height, getBackground(),
                           refs.size() > 1 ? new Color[]{color, color} : new Color[]{color}) {
        @Override
        public int getIconWidth() {
          return getWidth(myHasGroupWithMultipleRefs ? 2 : 1);
        }
      };
    }
    return createEmptyIcon(height);
  }

  @Nonnull
  private static consulo.ui.image.Image createEmptyIcon(int height) {
    return consulo.ui.image.Image.empty(LabelIcon.getWidth(height, 2), height);
  }

  @Nonnull
  @Override
  protected JBLabel createRestLabel(int restSize) {
    String gray = ColorUtil.toHex(UIManager.getColor("Button.disabledText"));
    return createLabel("<html><font color=\"#" + gray + "\">... " + restSize + " more in details pane</font></html>",
                       createEmptyIcon(getIconHeight()));
  }
}
