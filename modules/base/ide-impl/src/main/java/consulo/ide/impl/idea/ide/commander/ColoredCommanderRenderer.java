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

package consulo.ide.impl.idea.ide.commander;

import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.UIUtil;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

final class ColoredCommanderRenderer extends ColoredListCellRenderer {
  private final CommanderPanel myCommanderPanel;

  ColoredCommanderRenderer(@Nonnull final CommanderPanel commanderPanel) {
    myCommanderPanel = commanderPanel;
  }

  @Override
  public Component getListCellRendererComponent(final JList list, final Object value, final int index, boolean selected, boolean hasFocus) {
    hasFocus = selected; // border around inactive items

    if (!myCommanderPanel.isActive()) {
      selected = false;
    }

    return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
  }

  @Override
  protected void customizeCellRenderer(@Nonnull final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus) {
    Color color = UIUtil.getListForeground();
    SimpleTextAttributes attributes = null;
    String locationString = null;

    setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0)); // for separator, see below

    if (value instanceof NodeDescriptor) {
      final NodeDescriptor descriptor = (NodeDescriptor)value;
      setIcon(descriptor.getIcon());
      final ColorValue elementColor = descriptor.getColor();

      if (elementColor != null) {
        color = TargetAWT.to(elementColor);
      }

      if (descriptor instanceof AbstractTreeNode) {
        final AbstractTreeNode treeNode = (AbstractTreeNode)descriptor;
        final TextAttributesKey attributesKey = treeNode.getPresentation().getTextAttributesKey();

        if (attributesKey != null) {
          final TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);

          if (textAttributes != null) attributes = TextAttributesUtil.fromTextAttributes(textAttributes);
        }
        locationString = treeNode.getPresentation().getLocationString();

        final PresentationData presentation = treeNode.getPresentation();
        if (presentation.hasSeparatorAbove() && !selected) {
          setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JBCurrentTheme.CustomFrameDecorations.separatorForeground()),
                                                       BorderFactory.createEmptyBorder(0, 0, 1, 0)));
        }
      }
    }

    if (attributes == null) attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
    final String text = value.toString();

    if (myCommanderPanel.isEnableSearchHighlighting()) {
      JList list1 = myCommanderPanel.getList();
      if (list1 != null) {
        SpeedSearchUtil.appendFragmentsForSpeedSearch(list1, text, attributes, selected, this);
      }
    }
    else {
      append(text != null ? text : "", attributes);
    }

    if (locationString != null && locationString.length() > 0) {
      append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }
}
