/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tree;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.navigation.ItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.SpeedSearchUtilBase;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class NodeRenderer extends ColoredTreeCellRenderer {
  @RequiredUIAccess
  @Override
  public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    ColorValue color = null;
    NodeDescriptor descriptor = null;
    if (value instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      Object userObject = node.getUserObject();
      if (userObject instanceof NodeDescriptor) {
        descriptor = (NodeDescriptor)userObject;
        color = descriptor.getColor();
        setIcon(descriptor.getIcon());
      }
    }

    if (descriptor instanceof PresentableNodeDescriptor) {
      final PresentableNodeDescriptor node = (PresentableNodeDescriptor)descriptor;
      final PresentationData presentation = node.getPresentation();

      final List<PresentableNodeDescriptor.ColoredFragment> coloredText = presentation.getColoredText();
      if (coloredText.isEmpty()) {
        String text = tree.convertValueToText(value.toString(), selected, expanded, leaf, row, hasFocus);
        SimpleTextAttributes simpleTextAttributes = getSimpleTextAttributes(node, presentation.getForcedTextForeground() != null ? presentation.getForcedTextForeground() : color);
        append(text, simpleTextAttributes);
      }
      else {
        boolean first = true;
        for (PresentableNodeDescriptor.ColoredFragment each : coloredText) {
          SimpleTextAttributes simpleTextAttributes = each.getAttributes();
          if (each.getAttributes().getFgColor() == null && presentation.getForcedTextForeground() != null) {
            simpleTextAttributes = addColorToSimpleTextAttributes(each.getAttributes(),
                                                                  presentation.getForcedTextForeground() != null ? presentation.getForcedTextForeground() : color);
          }
          if (first) {
            final TextAttributesKey textAttributesKey = presentation.getTextAttributesKey();
            if (textAttributesKey != null) {
              final TextAttributes forcedAttributes = getColorsScheme().getAttributes(textAttributesKey);
              if (forcedAttributes != null) {
                simpleTextAttributes = SimpleTextAttributes.merge(TextAttributesUtil.fromTextAttributes(forcedAttributes), simpleTextAttributes);
              }
            }
            first = false;
          }
          // treat grayed text as non-main
          boolean isMain = simpleTextAttributes != SimpleTextAttributes.GRAYED_ATTRIBUTES;
          append(each.getText(), simpleTextAttributes, isMain);
        }
      }

      final String location = presentation.getLocationString();
      if (!StringUtil.isEmpty(location)) {
        append(presentation.getLocationPrefix() + location + presentation.getLocationSuffix(), SimpleTextAttributes.GRAY_ATTRIBUTES, false);
      }

      setToolTipText(presentation.getTooltip());
    }
    else if (value != null) {
      String text = value.toString();
      if (descriptor != null) {
        text = descriptor.getName();
      }
      text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus);
      if (text == null) {
        text = "";
      }
      append(text);
      setToolTipText(null);
    }
    if (!AbstractTreeUi.isLoadingNode(value)) {
      SpeedSearchUtilBase.applySpeedSearchHighlighting(tree, this, true, selected);
    }
  }

  @Nonnull
  protected EditorColorsScheme getColorsScheme() {
    return EditorColorsManager.getInstance().getGlobalScheme();
  }

  protected SimpleTextAttributes getSimpleTextAttributes(final PresentableNodeDescriptor node, final ColorValue color) {
    SimpleTextAttributes simpleTextAttributes = getSimpleTextAttributes(node.getPresentation(), getColorsScheme());

    return addColorToSimpleTextAttributes(simpleTextAttributes, color);
  }

  private SimpleTextAttributes addColorToSimpleTextAttributes(SimpleTextAttributes simpleTextAttributes, ColorValue color) {
    if (color != null) {
      final TextAttributes textAttributes = TextAttributesUtil.toTextAttributes(simpleTextAttributes);
      textAttributes.setForegroundColor(color);
      simpleTextAttributes = TextAttributesUtil.fromTextAttributes(textAttributes);
    }
    return simpleTextAttributes;
  }

  public static SimpleTextAttributes getSimpleTextAttributes(@Nullable final ItemPresentation presentation) {
    return getSimpleTextAttributes(presentation, EditorColorsManager.getInstance().getGlobalScheme());
  }

  public static SimpleTextAttributes getSimpleTextAttributes(@Nullable final ItemPresentation presentation,
                                                             @Nonnull EditorColorsScheme colorsScheme)
  {
    if (presentation instanceof ColoredItemPresentation) {
      final TextAttributesKey textAttributesKey = ((ColoredItemPresentation) presentation).getTextAttributesKey();
      if (textAttributesKey == null) return SimpleTextAttributes.REGULAR_ATTRIBUTES;
      final TextAttributes textAttributes = colorsScheme.getAttributes(textAttributesKey);
      return textAttributes == null ? SimpleTextAttributes.REGULAR_ATTRIBUTES : TextAttributesUtil.fromTextAttributes(textAttributes);
    }
    return SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

}