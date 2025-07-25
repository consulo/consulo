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
package consulo.ide.impl.idea.usages.impl;

import consulo.colorScheme.EditorColorsScheme;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.ui.ex.DarculaColors;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.FontUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.style.StyleManager;
import consulo.usage.*;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * @author max
 */
class UsageViewTreeCellRenderer extends ColoredTreeCellRenderer {
  private static final Logger LOG = Logger.getInstance(UsageViewTreeCellRenderer.class);
  private static final EditorColorsScheme ourColorsScheme = UsageTreeColorsScheme.getInstance().getScheme();
  private static final SimpleTextAttributes ourInvalidAttributes = TextAttributesUtil.fromTextAttributes(ourColorsScheme.getAttributes(UsageTreeColors.INVALID_PREFIX));
  private static final SimpleTextAttributes ourReadOnlyAttributes = TextAttributesUtil.fromTextAttributes(ourColorsScheme.getAttributes(UsageTreeColors.READONLY_PREFIX));
  private static final SimpleTextAttributes ourNumberOfUsagesAttribute = TextAttributesUtil.fromTextAttributes(ourColorsScheme.getAttributes(UsageTreeColors.NUMBER_OF_USAGES));
  private static final SimpleTextAttributes ourInvalidAttributesDarcula =
          new SimpleTextAttributes(null, DarculaColors.RED, null, ourInvalidAttributes.getStyle());
  private static final Insets STANDARD_IPAD_NOWIFI = JBUI.insets(1, 2);

  private final UsageViewPresentation myPresentation;
  private final UsageView myView;

  UsageViewTreeCellRenderer(@Nonnull UsageView view) {
    myView = view;
    myPresentation = view.getPresentation();
    setIpad(STANDARD_IPAD_NOWIFI);
  }

  private Dimension cachedPreferredSize;

  @Override
  public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    boolean showAsReadOnly = false;
    if (value instanceof Node && value != tree.getModel().getRoot()) {
      Node node = (Node)value;
      if (!node.isValid()) {
        append(
          UsageViewBundle.message("node.invalid") + " ",
          StyleManager.get().getCurrentStyle().isDark() ? ourInvalidAttributesDarcula : ourInvalidAttributes
        );
      }
      if (myPresentation.isShowReadOnlyStatusAsRed() && node.isReadOnly()) {
        showAsReadOnly = true;
      }
    }

    if (value instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
      Object userObject = treeNode.getUserObject();

      if (userObject instanceof UsageTarget) {
        UsageTarget usageTarget = (UsageTarget)userObject;
        if (!usageTarget.isValid()) {
          if (!getCharSequence(false).toString().contains(UsageViewBundle.message("node.invalid"))) {
            append(UsageViewBundle.message("node.invalid"), ourInvalidAttributes);
          }
          return;
        }

        ItemPresentation presentation = usageTarget.getPresentation();
        LOG.assertTrue(presentation != null);
        if (showAsReadOnly) {
          append(UsageViewBundle.message("node.readonly") + " ", ourReadOnlyAttributes);
        }
        String text = presentation.getPresentableText();
        append(text == null ? "" : text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        setIcon(presentation.getIcon());
      }
      else if (treeNode instanceof GroupNode) {
        GroupNode node = (GroupNode)treeNode;

        if (node.isRoot()) {
          append(StringUtil.capitalize(myPresentation.getUsagesWord()), patchAttrs(node, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES));
        }
        else {
          append(node.getGroup().getText(myView), patchAttrs(node, showAsReadOnly ? ourReadOnlyAttributes : SimpleTextAttributes.REGULAR_ATTRIBUTES));
          setIcon(node.getGroup().getIcon());
        }

        int count = node.getRecursiveUsageCount();
        SimpleTextAttributes attributes = patchAttrs(node, ourNumberOfUsagesAttribute);
        append(FontUtil.spaceAndThinSpace() + StringUtil.pluralize(count + " " + myPresentation.getUsagesWord(), count),
               SimpleTextAttributes.GRAYED_ATTRIBUTES.derive(attributes.getStyle(), null, null, null));
      }
      else if (treeNode instanceof UsageNode) {
        UsageNode node = (UsageNode)treeNode;
        setIcon(node.getUsage().getPresentation().getIcon());
        if (showAsReadOnly) {
          append(UsageViewBundle.message("node.readonly") + " ", patchAttrs(node, ourReadOnlyAttributes));
        }

        if (node.isValid()) {
          TextChunk[] text = node.getUsage().getPresentation().getText();
          for (int i = 0; i < text.length; i++) {
            TextChunk textChunk = text[i];
            SimpleTextAttributes simples = textChunk.getSimpleAttributesIgnoreBackground();
            append(textChunk.getText() + (i == 0 ? " " : ""), patchAttrs(node, simples), true);
          }
        }
      }
      else if (userObject instanceof String) {
        append((String)userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
      else {
        append(userObject == null ? "" : userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    }
    else {
      append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, mySelected);
  }

  // computes the node text regardless of the node visibility
  @Nonnull
  String getPlainTextForNode(Object value) {
    boolean showAsReadOnly = false;
    StringBuilder result = new StringBuilder();
    if (value instanceof Node) {
      Node node = (Node)value;
      if (!node.isValid()) {
        result.append(UsageViewBundle.message("node.invalid")).append(" ");
      }
      if (myPresentation.isShowReadOnlyStatusAsRed() && node.isReadOnly()) {
        showAsReadOnly = true;
      }
    }

    if (value instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
      Object userObject = treeNode.getUserObject();

      if (userObject instanceof UsageTarget) {
        UsageTarget usageTarget = (UsageTarget)userObject;
        if (usageTarget.isValid()) {
          ItemPresentation presentation = usageTarget.getPresentation();
          LOG.assertTrue(presentation != null);
          if (showAsReadOnly) {
            result.append(UsageViewBundle.message("node.readonly")).append(" ");
          }
          String text = presentation.getPresentableText();
          result.append(text == null ? "" : text);
        }
        else {
          result.append(UsageViewBundle.message("node.invalid"));
        }
      }
      else if (treeNode instanceof GroupNode) {
        GroupNode node = (GroupNode)treeNode;

        if (node.isRoot()) {
          result.append(StringUtil.capitalize(myPresentation.getUsagesWord()));
        }
        else {
          result.append(node.getGroup().getText(myView));
        }

        int count = node.getRecursiveUsageCount();
        result.append(" (").append(StringUtil.pluralize(count + " " + myPresentation.getUsagesWord(), count)).append(")");
      }
      else if (treeNode instanceof UsageNode) {
        UsageNode node = (UsageNode)treeNode;

        if (showAsReadOnly) {
          result.append(UsageViewBundle.message("node.readonly")).append(" ");
        }

        if (node.isValid()) {
          TextChunk[] text = node.getUsage().getPresentation().getText();
          for (TextChunk textChunk : text) {
            result.append(textChunk.getText());
          }
        }
      }
      else if (userObject instanceof String) {
        result.append((String)userObject);
      }
      else {
        result.append(userObject == null ? "" : userObject.toString());
      }
    }
    else {
      result.append(value);
    }
    return result.toString();
  }

  enum RowLocation {
    BEFORE_VISIBLE_RECT,
    INSIDE_VISIBLE_RECT,
    AFTER_VISIBLE_RECT
  }

  @Nonnull
  RowLocation isRowVisible(int row, @Nonnull Rectangle visibleRect) {
    Dimension pref;
    if (cachedPreferredSize == null) {
      cachedPreferredSize = pref = getPreferredSize();
    }
    else {
      pref = cachedPreferredSize;
    }
    pref.width = Math.max(visibleRect.width, pref.width);
    JTree tree = getTree();
    Rectangle bounds = tree == null ? null : tree.getRowBounds(row);

    int y = bounds == null ? 0 : bounds.y;
    TextRange vis = TextRange.from(Math.max(0, visibleRect.y - pref.height), visibleRect.height + pref.height * 2);
    boolean inside = vis.contains(y);
    if (inside) {
      return RowLocation.INSIDE_VISIBLE_RECT;
    }
    return y < vis.getStartOffset() ? RowLocation.BEFORE_VISIBLE_RECT : RowLocation.AFTER_VISIBLE_RECT;
  }

  private static SimpleTextAttributes patchAttrs(@Nonnull Node node, @Nonnull SimpleTextAttributes original) {
    if (node.isExcluded()) {
      original = new SimpleTextAttributes(original.getStyle() | SimpleTextAttributes.STYLE_STRIKEOUT, original.getFgColor(), original.getWaveColor());
    }
    if (node instanceof GroupNode) {
      UsageGroup group = ((GroupNode)node).getGroup();
      FileStatus fileStatus = group != null ? group.getFileStatus() : null;
      if (fileStatus != null && fileStatus != FileStatus.NOT_CHANGED) {
        original = new SimpleTextAttributes(original.getStyle(), TargetAWT.to(fileStatus.getColor()), original.getWaveColor());
      }

      DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
      if (parent != null && parent.isRoot()) {
        original = new SimpleTextAttributes(original.getStyle() | SimpleTextAttributes.STYLE_BOLD, original.getFgColor(), original.getWaveColor());
      }
    }
    return original;
  }

  static String getTooltipFromPresentation(Object value) {
    if (value instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
      if (treeNode instanceof UsageNode) {
        UsageNode node = (UsageNode)treeNode;
        return node.getUsage().getPresentation().getTooltipText();
      }
    }
    return null;
  }
}
