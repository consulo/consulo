// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.tree;

import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.SpeedSearchUtilBase;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.awt.util.RenderingUtil;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;

import javax.accessibility.AccessibleContext;
import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public abstract class ColoredTreeCellRenderer extends SimpleColoredComponent implements TreeCellRenderer {
  private static final Logger LOG = Logger.getInstance(ColoredTreeCellRenderer.class);

  private static final Image LOADING_NODE_ICON = Image.empty(8, 16);

  /**
   * Defines whether the tree is selected or not
   */
  protected boolean mySelected;
  /**
   * Defines whether the tree has focus or not
   */
  private boolean myFocused;
  private boolean myFocusedCalculated;

  protected boolean myUsedCustomSpeedSearchHighlighting = false;

  protected JTree myTree;

  private boolean myOpaque = true;

  @Override
  public final Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    try {
      rendererComponentInner(tree, value, selected, expanded, leaf, row, hasFocus);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      try {
        LOG.error(e);
      }
      catch (Exception ignore) {
      }
    }
    return this;
  }

  private void rendererComponentInner(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    myTree = tree;

    clear();

    mySelected = selected;
    myFocusedCalculated = false;

    // We paint background if and only if tree path is selected and tree has focus.
    // If path is selected and tree is not focused then we just paint focused border.
    if (UIUtil.isFullRowSelectionLAF()) {
      setBackground(selected ? UIUtil.getTreeSelectionBackground() : null);
    }
    else if (WideSelectionTreeUI.isWideSelection(tree)) {
      setPaintFocusBorder(false);
      if (selected) {
        setBackground(UIUtil.getTreeSelectionBackground(hasFocus));
      }
      else {
        setBackground(null);
      }
    }
    else if (selected) {
      setPaintFocusBorder(true);
      if (isFocused()) {
        setBackground(UIUtil.getTreeSelectionBackground());
      }
      else {
        setBackground(null);
      }
    }
    else {
      setBackground(null);
    }

    if (value instanceof LoadingNode) {
      setForeground(JBColor.GRAY);
      setIcon(LOADING_NODE_ICON);
    }
    else {
      setForeground(RenderingUtil.getForeground(tree, selected));
      setIcon(null);
    }

    if (WideSelectionTreeUI.isWideSelection(tree)) {
      super.setOpaque(false);  // avoid erasing Nimbus focus frame
      super.setIconOpaque(false);
    }
    else {
      super.setOpaque(myOpaque || selected && hasFocus || selected && isFocused()); // draw selection background even for non-opaque tree
    }
    customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

    if (!myUsedCustomSpeedSearchHighlighting && !AbstractTreeUi.isLoadingNode(value)) {
      SpeedSearchUtilBase.applySpeedSearchHighlightingFiltered(tree, value, this, true, selected);
    }
  }

  public JTree getTree() {
    return myTree;
  }

  protected final boolean isFocused() {
    if (!myFocusedCalculated) {
      myFocused = calcFocusedState();
      myFocusedCalculated = true;
    }
    return myFocused;
  }

  protected boolean calcFocusedState() {
    return myTree.hasFocus();
  }

  @Override
  public void setOpaque(boolean isOpaque) {
    myOpaque = isOpaque;
    super.setOpaque(isOpaque);
  }

  @Override
  public Font getFont() {
    Font font = super.getFont();

    // Cell renderers could have no parent and no explicit set font.
    // Take tree font in this case.
    if (font != null) return font;
    JTree tree = getTree();
    return tree != null ? tree.getFont() : null;
  }

  /**
   * When the item is selected then we use default tree's selection foreground.
   * It guaranties readability of selected text in any LAF.
   */
  @Override
  public void append(@Nonnull @Nls String fragment, @Nonnull SimpleTextAttributes attributes, boolean isMainText) {
    if (mySelected && isFocused()) {
      super.append(fragment, new SimpleTextAttributes(attributes.getStyle(), UIUtil.getTreeSelectionForeground(true)), isMainText);
    }
    else {
      super.append(fragment, attributes, isMainText);
    }
  }

  @Override
  protected void revalidateAndRepaint() {
    // no need for this in a renderer
  }

  /**
   * This method is invoked only for customization of component.
   * All component attributes are cleared when this method is being invoked.
   */
  public abstract void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus);

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleColoredTreeCellRenderer();
    }
    return accessibleContext;
  }

  protected class AccessibleColoredTreeCellRenderer extends AccessibleSimpleColoredComponent {
  }

  // The following method are overridden for performance reasons.
  // See the Implementation Note for more information.
  // javax.swing.tree.DefaultTreeCellRenderer
  // javax.swing.DefaultListCellRenderer

  @Override
  public void validate() {
  }

  @Override
  public void invalidate() {
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void repaint(long tm, int x, int y, int width, int height) {
  }

  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, char oldValue, char newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, short oldValue, short newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, int oldValue, int newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, long oldValue, long newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, float oldValue, float newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, double oldValue, double newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
  }
}
