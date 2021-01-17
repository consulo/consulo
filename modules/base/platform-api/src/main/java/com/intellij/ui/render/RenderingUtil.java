// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.render;

import com.intellij.util.ui.JBUI.CurrentTheme;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.WideSelectionTreeUI;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public final class RenderingUtil {
  /**
   * This key can be set to a list or a tree to paint unfocused selection as focused.
   *
   * @see JComponent#putClientProperty
   */
  public static final Key<Boolean> ALWAYS_PAINT_SELECTION_AS_FOCUSED = Key.create("ALWAYS_PAINT_SELECTION_AS_FOCUSED");

  /**
   * This key allows to paint a background of a hovered row if it is not selected.
   */
  //@ApiStatus.Experimental
  public static final Key<Boolean> PAINT_HOVERED_BACKGROUND = Key.create("PAINT_HOVERED_BACKGROUND");

  /**
   * This key allows to paint focused selection even if a component does not have a focus.
   * Our tree table implementations use a table as a focusable sibling of a tree.
   * In such case the table colors will be used to paint the tree.
   */
  //@ApiStatus.Internal
  public static final Key<JComponent> FOCUSABLE_SIBLING = Key.create("FOCUSABLE_SIBLING");

  /**
   * This key can be set to provide a custom selection background.
   */
  //@ApiStatus.Internal
  public static final Key<Supplier<Color>> CUSTOM_SELECTION_BACKGROUND = Key.create("CUSTOM_SELECTION_BACKGROUND");


  @Nonnull
  public static Color getBackground(@Nonnull JList<?> list, boolean selected) {
    return selected ? getSelectionBackground(list) : getBackground(list);
  }

  @Nonnull
  public static Color getBackground(@Nonnull JTable table, boolean selected) {
    return selected ? getSelectionBackground(table) : getBackground(table);
  }

  @Nonnull
  public static Color getBackground(@Nonnull JTree tree, boolean selected) {
    return selected ? getSelectionBackground(tree) : getBackground(tree);
  }


  @Nonnull
  public static Color getBackground(@Nonnull JList<?> list) {
    Color background = list.getBackground();
    return background != null ? background : CurrentTheme.List.BACKGROUND;
  }

  @Nonnull
  public static Color getBackground(@Nonnull JTable table) {
    Color background = table.getBackground();
    return background != null ? background : CurrentTheme.Table.BACKGROUND;
  }

  @Nonnull
  public static Color getBackground(@Nonnull JTree tree) {
    JTable table = getTableFor(tree);
    if (table != null) return getBackground(table); // tree table
    Color background = tree.getBackground();
    return background != null ? background : CurrentTheme.Tree.BACKGROUND;
  }


  @Nonnull
  public static Color getSelectionBackground(@Nonnull JList<?> list) {
    Color background = getCustomSelectionBackground(list);
    return background != null ? background : CurrentTheme.List.Selection.background(isFocused(list));
  }

  @Nonnull
  public static Color getSelectionBackground(@Nonnull JTable table) {
    Color background = getCustomSelectionBackground(table);
    return background != null ? background : CurrentTheme.Table.Selection.background(isFocused(table));
  }

  @Nonnull
  public static Color getSelectionBackground(@Nonnull JTree tree) {
    JTable table = getTableFor(tree);
    if (table != null) return getSelectionBackground(table); // tree table
    Color background = getCustomSelectionBackground(tree);
    return background != null ? background : CurrentTheme.Tree.Selection.background(isFocused(tree));
  }


  @Nonnull
  public static Color getForeground(@Nonnull JList<?> list, boolean selected) {
    return selected ? getSelectionForeground(list) : getForeground(list);
  }

  @Nonnull
  public static Color getForeground(@Nonnull JTable table, boolean selected) {
    return selected ? getSelectionForeground(table) : getForeground(table);
  }

  @Nonnull
  public static Color getForeground(@Nonnull JTree tree, boolean selected) {
    return selected ? getSelectionForeground(tree) : getForeground(tree);
  }


  @Nonnull
  public static Color getForeground(@Nonnull JList<?> list) {
    Color foreground = list.getForeground();
    return foreground != null ? foreground : CurrentTheme.List.FOREGROUND;
  }

  @Nonnull
  public static Color getForeground(@Nonnull JTable table) {
    Color foreground = table.getForeground();
    return foreground != null ? foreground : CurrentTheme.Table.FOREGROUND;
  }

  @Nonnull
  public static Color getForeground(@Nonnull JTree tree) {
    JTable table = getTableFor(tree);
    if (table != null) return getForeground(table); // tree table
    Color foreground = tree.getForeground();
    return foreground != null ? foreground : CurrentTheme.Tree.FOREGROUND;
  }

  @Nonnull
  public static Color getSelectionForeground(@Nonnull JList<?> list) {
    return CurrentTheme.List.Selection.foreground(isFocused(list));
  }

  @Nonnull
  public static Color getSelectionForeground(@Nonnull JTable table) {
    return CurrentTheme.Table.Selection.foreground(isFocused(table));
  }

  @Nonnull
  public static Color getSelectionForeground(@Nonnull JTree tree) {
    JTable table = getTableFor(tree);
    if (table != null) return getSelectionForeground(table); // tree table
    return CurrentTheme.Tree.Selection.foreground(isFocused(tree));
  }

  public static boolean isHoverPaintingDisabled(@Nonnull JComponent component) {
    return Boolean.FALSE.equals(component.getClientProperty(PAINT_HOVERED_BACKGROUND));
  }

  public static
  @Nullable
  Color getHoverBackground(@Nonnull JList<?> list) {
    if (isHoverPaintingDisabled(list)) return null;
    return CurrentTheme.List.Hover.background(isFocused(list));
  }

  public static
  @Nullable
  Color getHoverBackground(@Nonnull JTable table) {
    if (isHoverPaintingDisabled(table)) return null;
    return CurrentTheme.Table.Hover.background(isFocused(table));
  }

  public static
  @Nullable
  Color getHoverBackground(@Nonnull JTree tree) {
    JTable table = getTableFor(tree);
    if (table != null) return getHoverBackground(table); // tree table
    if (isHoverPaintingDisabled(tree)) return null;
    return CurrentTheme.Tree.Hover.background(isFocused(tree));
  }


  public static boolean isFocused(@Nonnull JComponent component) {
    if (isFocusedImpl(component)) return true;
    JComponent sibling = UIUtil.getClientProperty(component, FOCUSABLE_SIBLING);
    return sibling != null && isFocusedImpl(sibling);
  }

  private static boolean isFocusedImpl(@Nonnull JComponent component) {
    return component.hasFocus() || UIUtil.isClientPropertyTrue(component, ALWAYS_PAINT_SELECTION_AS_FOCUSED);
  }

  private static JTable getTableFor(@Nonnull JTree tree) {
    @SuppressWarnings("deprecation") Object property = tree.getClientProperty(WideSelectionTreeUI.TREE_TABLE_TREE_KEY);
    if (property instanceof JTable) return (JTable)property;
    JComponent sibling = UIUtil.getClientProperty(tree, FOCUSABLE_SIBLING);
    return sibling instanceof JTable ? (JTable)sibling : null;
  }

  private static Color getCustomSelectionBackground(@Nonnull JComponent component) {
    Supplier<Color> supplier = UIUtil.getClientProperty(component, CUSTOM_SELECTION_BACKGROUND);
    return supplier == null ? null : supplier.get();
  }
}
