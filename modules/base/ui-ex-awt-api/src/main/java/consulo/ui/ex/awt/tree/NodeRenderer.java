// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt.tree;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

public class NodeRenderer extends ColoredTreeCellRenderer {
  private boolean myLightTheme;

  protected Image fixIconIfNeeded(Image icon, boolean selected, boolean hasFocus) {
//    if (icon != null && myLightTheme && selected && hasFocus) {
//      return IconLibraryManager.get().inverseIcon(icon);
//    }

    return icon;
  }

  @Override
  public void customizeCellRenderer(@Nonnull JTree tree,
                                    Object value,
                                    boolean selected,
                                    boolean expanded,
                                    boolean leaf,
                                    int row,
                                    boolean hasFocus) {
    myLightTheme = !StyleManager.get().getCurrentStyle().isDark();

    Object node = TreeUtil.getUserObject(value);

    if (node instanceof NodeDescriptor<?> descriptor) {
      // TODO: use this color somewhere
      ColorValue color = descriptor.getColor();
      setIcon(fixIconIfNeeded(descriptor.getIcon(), selected, hasFocus));
    }

    ItemPresentation p0 = getPresentation(node);

    if (p0 instanceof PresentationData presentation) {
      ColorValue color = node instanceof NodeDescriptor ? ((NodeDescriptor<?>)node).getColor() : null;
      setIcon(fixIconIfNeeded(presentation.getIcon(), selected, hasFocus));

      final List<PresentableNodeDescriptor.ColoredFragment> coloredText = presentation.getColoredText();
      ColorValue forcedForeground = presentation.getForcedTextForeground();
      if (coloredText.isEmpty()) {
        String text = presentation.getPresentableText();
        if (StringUtil.isEmpty(text)) {
          String valueSting = value.toString();
          text = valueSting;
        }
        text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus);
        SimpleTextAttributes simpleTextAttributes = getSimpleTextAttributes(
          presentation, forcedForeground != null ? forcedForeground : color, node);
        append(text, simpleTextAttributes);
        String location = presentation.getLocationString();
        if (!StringUtil.isEmpty(location)) {
          SimpleTextAttributes attributes = SimpleTextAttributes.merge(simpleTextAttributes, SimpleTextAttributes.GRAYED_ATTRIBUTES);
          append(presentation.getLocationPrefix() + location + presentation.getLocationSuffix(), attributes, false);
        }
      }
      else {
        boolean first = true;
        boolean isMain = true;
        for (PresentableNodeDescriptor.ColoredFragment each : coloredText) {
          SimpleTextAttributes simpleTextAttributes = each.getAttributes();
          if (each.getAttributes().getFgColor() == null && forcedForeground != null) {
            simpleTextAttributes = addColorToSimpleTextAttributes(each.getAttributes(), forcedForeground);
          }
          if (first) {
            final TextAttributesKey textAttributesKey = presentation.getTextAttributesKey();
            if (textAttributesKey != null) {
              TextAttributes forcedAttributes = getScheme().getAttributes(textAttributesKey);
              if (forcedAttributes != null) {
                simpleTextAttributes =
                  SimpleTextAttributes.merge(simpleTextAttributes, TextAttributesUtil.fromTextAttributes(forcedAttributes));
              }
            }
            first = false;
          }
          // the first grayed text (inactive foreground, regular or small) ends main speed-searchable text
          isMain = isMain && !Comparing.equal(simpleTextAttributes.getFgColor(), SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
          append(each.getText(), simpleTextAttributes, isMain);
        }
        String location = presentation.getLocationString();
        if (!StringUtil.isEmpty(location)) {
          append(presentation.getLocationPrefix() + location + presentation.getLocationSuffix(),
                 SimpleTextAttributes.GRAYED_ATTRIBUTES,
                 false);
        }
      }

      setToolTipText(presentation.getTooltip());
    }
    else if (value != null) {
      String text = value.toString();
      if (node instanceof NodeDescriptor) {
        text = node.toString();
      }
      text = tree.convertValueToText(text, selected, expanded, leaf, row, hasFocus);
      if (text == null) {
        text = "";
      }
      append(text);
      setToolTipText(null);
    }
  }

  protected @Nullable ItemPresentation getPresentation(Object node) {
    return node instanceof PresentableNodeDescriptor ? ((PresentableNodeDescriptor<?>)node).getPresentation() :
      node instanceof NavigationItem ? ((NavigationItem)node).getPresentation() :
        null;
  }

  @Nonnull
  private static EditorColorsScheme getScheme() {
    return EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
  }

  @Nonnull
  protected SimpleTextAttributes getSimpleTextAttributes(@Nonnull PresentationData presentation,
                                                         ColorValue color,
                                                         @Nonnull Object node) {
    SimpleTextAttributes simpleTextAttributes = getSimpleTextAttributes(presentation, getScheme());

    return addColorToSimpleTextAttributes(simpleTextAttributes, color);
  }

  private static SimpleTextAttributes addColorToSimpleTextAttributes(SimpleTextAttributes simpleTextAttributes, ColorValue color) {
    if (color != null) {
      final TextAttributes textAttributes = TextAttributesUtil.toTextAttributes(simpleTextAttributes);
      textAttributes.setForegroundColor(color);
      simpleTextAttributes = TextAttributesUtil.fromTextAttributes(textAttributes);
    }
    return simpleTextAttributes;
  }

  public static SimpleTextAttributes getSimpleTextAttributes(final @Nullable ItemPresentation presentation) {
    return getSimpleTextAttributes(presentation, getScheme());
  }

  private static SimpleTextAttributes getSimpleTextAttributes(final @Nullable ItemPresentation presentation,
                                                              @Nonnull EditorColorsScheme colorsScheme) {
    if (presentation instanceof ColoredItemPresentation) {
      final TextAttributesKey textAttributesKey = ((ColoredItemPresentation)presentation).getTextAttributesKey();
      if (textAttributesKey == null) return SimpleTextAttributes.REGULAR_ATTRIBUTES;
      final TextAttributes textAttributes = colorsScheme.getAttributes(textAttributesKey);
      return textAttributes == null ? SimpleTextAttributes.REGULAR_ATTRIBUTES : TextAttributesUtil.fromTextAttributes(textAttributes);
    }
    return SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

}