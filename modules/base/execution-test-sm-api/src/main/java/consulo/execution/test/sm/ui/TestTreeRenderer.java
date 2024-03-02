// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.test.sm.ui;

import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.RelativeFont;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.util.UISettingsUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

/**
 * @author Roman Chernyatchik
 */
public class TestTreeRenderer extends ColoredTreeCellRenderer {
  private static final String SPACE_STRING = " ";

  private final TestConsoleProperties myConsoleProperties;
  private SMRootTestProxyFormatter myAdditionalRootFormatter;
  private String myDurationText;
  private Color myDurationColor;
  private int myDurationWidth;
  private int myDurationLeftInset;
  private int myDurationRightInset;

  public TestTreeRenderer(final TestConsoleProperties consoleProperties) {
    myConsoleProperties = consoleProperties;
  }

  @Override
  public void customizeCellRenderer(@Nonnull final JTree tree,
                                    final Object value,
                                    final boolean selected,
                                    final boolean expanded,
                                    final boolean leaf,
                                    final int row,
                                    final boolean hasFocus) {
    myDurationText = null;
    myDurationColor = null;
    myDurationWidth = 0;
    myDurationLeftInset = 0;
    myDurationRightInset = 0;

    final DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
    final Object userObj = node.getUserObject();
    if (userObj instanceof SMTRunnerNodeDescriptor) {
      final SMTRunnerNodeDescriptor desc = (SMTRunnerNodeDescriptor)userObj;
      final SMTestProxy testProxy = desc.getElement();

      if (testProxy instanceof SMTestProxy.SMRootTestProxy) {
        SMTestProxy.SMRootTestProxy rootTestProxy = (SMTestProxy.SMRootTestProxy)testProxy;
        if (rootTestProxy.isLeaf()) {
          TestsPresentationUtil.formatRootNodeWithoutChildren(rootTestProxy, this);
        }
        else {
          TestsPresentationUtil.formatRootNodeWithChildren(rootTestProxy, this);
        }
        if (myAdditionalRootFormatter != null) {
          myAdditionalRootFormatter.format(rootTestProxy, this);
        }
      }
      else {
        TestsPresentationUtil.formatTestProxy(testProxy, this);
      }

      if (TestConsoleProperties.SHOW_INLINE_STATISTICS.value(myConsoleProperties)) {
        myDurationText = testProxy.getDurationString(myConsoleProperties);
        if (myDurationText != null) {
          FontMetrics metrics = getFontMetrics(RelativeFont.SMALL.derive(getFont()));
          myDurationWidth = metrics.stringWidth(myDurationText);
          myDurationLeftInset = metrics.getHeight() / 4;
          myDurationRightInset = myDurationLeftInset;
          myDurationColor = selected ? UIUtil.getTreeSelectionForeground(hasFocus) : SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor();
        }
      }
      //Done
      return;
    }

    //strange node
    final String text = node.toString();
    //no icon
    append(text != null ? text : SPACE_STRING, SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Nonnull
  @Override
  public Dimension getPreferredSize() {
    Dimension preferredSize = super.getPreferredSize();
    if (myDurationWidth > 0) {
      preferredSize.width += myDurationWidth + myDurationLeftInset + myDurationRightInset;
    }
    return preferredSize;
  }

  public TestConsoleProperties getConsoleProperties() {
    return myConsoleProperties;
  }

  public void setAdditionalRootFormatter(@Nonnull SMRootTestProxyFormatter formatter) {
    myAdditionalRootFormatter = formatter;
  }

  public void removeAdditionalRootFormatter() {
    myAdditionalRootFormatter = null;
  }

  @Override
  protected void paintComponent(Graphics g) {
    UISettingsUtil.setupAntialiasing(g);
    Shape clip = null;
    int width = getWidth();
    int height = getHeight();
    if (isOpaque()) {
      // paint background for expanded row
      g.setColor(getBackground());
      g.fillRect(0, 0, width, height);
    }
    if (myDurationWidth > 0) {
      width -= myDurationWidth + myDurationLeftInset + myDurationRightInset;
      if (width > 0 && height > 0) {
        g.setColor(myDurationColor);
        Font oldFont = g.getFont();
        try {
          g.setFont(RelativeFont.SMALL.derive(oldFont));
          g.drawString(myDurationText, width + myDurationLeftInset, getTextBaseLine(g.getFontMetrics(), height));
          clip = g.getClip();
          g.clipRect(0, 0, width, height);
        } finally {
          g.setFont(oldFont);
        }
      }
    }
    super.paintComponent(g);
    // restore clip area if needed
    if (clip != null) g.setClip(clip);
  }
}