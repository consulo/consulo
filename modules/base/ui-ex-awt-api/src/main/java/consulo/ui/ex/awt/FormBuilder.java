/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static consulo.ui.ex.awt.UIUtil.DEFAULT_HGAP;
import static consulo.ui.ex.awt.UIUtil.DEFAULT_VGAP;
import static java.awt.GridBagConstraints.*;

/**
 * @author max
 */
public class FormBuilder {
  private boolean myAlignLabelOnRight;

  private int myLineCount = 0;
  private int myIndent;
  private final JPanel myPanel;
  private boolean myVertical;

  private int myVerticalGap;
  private int myHorizontalGap;

  private FormBuilder() {
    myPanel = new JPanel(new GridBagLayout());
    myVertical = false;
    myIndent = 0;
    myAlignLabelOnRight = false;
    myVerticalGap = DEFAULT_VGAP;
    myHorizontalGap = DEFAULT_HGAP;
  }

  public static FormBuilder createFormBuilder() {
    return new FormBuilder();
  }

  public FormBuilder addLabeledComponent(@Nullable Component label, @Nonnull Component component) {
    return addLabeledComponent(label, component, myVerticalGap, false);
  }

  public FormBuilder addLabeledComponent(@Nullable Component label, @Nonnull Component component, final int topInset) {
    return addLabeledComponent(label, component, topInset, false);
  }

  public FormBuilder addLabeledComponent(@Nullable JComponent label, @Nonnull Component component, boolean labelOnTop) {
    return addLabeledComponent(label, component, myVerticalGap, labelOnTop);
  }

  public FormBuilder addLabeledComponent(@Nonnull String labelText, @Nonnull Component component) {
    return addLabeledComponent(labelText, component, myVerticalGap, false);
  }

  public FormBuilder addLabeledComponent(@Nonnull String labelText, @Nonnull Component component, final int topInset) {
    return addLabeledComponent(labelText, component, topInset, false);
  }

  public FormBuilder addLabeledComponent(@Nonnull String labelText, @Nonnull Component component, boolean labelOnTop) {
    return addLabeledComponent(labelText, component, myVerticalGap, labelOnTop);
  }

  public FormBuilder addLabeledComponent(@Nonnull String labelText, @Nonnull Component component, final int topInset, boolean labelOnTop) {
    JLabel label = new JLabel(UIUtil.removeMnemonic(labelText));
    final int index = UIUtil.getDisplayMnemonicIndex(labelText);
    if (index != -1) {
      label.setDisplayedMnemonic(labelText.charAt(index + 1));
    }
    label.setLabelFor(component);

    return addLabeledComponent(label, component, topInset, labelOnTop);
  }

  public FormBuilder addComponent(@Nonnull Component component) {
    return addLabeledComponent((JLabel)null, component, myVerticalGap, false);
  }

  public FormBuilder addComponent(@Nonnull Component component, final int topInset) {
    return addLabeledComponent((JLabel)null, component, topInset, false);
  }

  public FormBuilder addSeparator(final int topInset) {
    return addComponent(new JSeparator(), topInset);
  }

  public FormBuilder addSeparator() {
    return addSeparator(myVerticalGap);
  }

  public FormBuilder addVerticalGap(final int height) {
    return addLabeledComponent((JLabel)null,
                               new Box.Filler(new Dimension(0, height), new Dimension(0, height), new Dimension(Short.MAX_VALUE, height)));
  }

  public FormBuilder addTooltip(final String text) {
    final JBLabel label = new JBLabel(text, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    label.setBorder(new EmptyBorder(0, 10, 0, 0));
    return addComponentToRightColumn(label, 1);
  }

  public FormBuilder addComponentToRightColumn(@Nonnull final Component component) {
    return addComponentToRightColumn(component, myVerticalGap);
  }

  public FormBuilder addComponentToRightColumn(@Nonnull final Component component, final int topInset) {
    return addLabeledComponent(new JLabel(), component, topInset);
  }

  @Nonnull
  public FormBuilder addComponentFillVertically(@Nonnull JComponent component, int topInset) {
    return addComponent(component); // TODO not supported !
  }

  public FormBuilder addLabeledComponent(@Nullable Component label, @Nonnull Component component, int topInset, boolean labelOnTop) {
    GridBagConstraints c = new GridBagConstraints();
    topInset = myLineCount > 0 ? topInset : 0;

    if (myVertical || labelOnTop || label == null) {
      c.gridwidth = 2;
      c.gridx = 0;
      c.gridy = myLineCount;
      c.weightx = 0;
      c.weighty = 0;
      c.fill = NONE;
      c.anchor = getLabelAnchor(component, false);
      c.insets = new Insets(topInset, myIndent, DEFAULT_VGAP, 0);

      if (label != null) myPanel.add(label, c);

      c.gridx = 0;
      c.gridy = myLineCount + 1;
      c.weightx = 1.0;
      c.weighty = getWeightY(component);
      c.fill = getFill(component);
      c.anchor = WEST;
      c.insets = new Insets(label == null ? topInset : 0, myIndent, 0, 0);

      myPanel.add(component, c);

      myLineCount += 2;
    }
    else {
      c.gridwidth = 1;
      c.gridx = 0;
      c.gridy = myLineCount;
      c.weightx = 0;
      c.weighty = 0;
      c.fill = NONE;
      c.anchor = getLabelAnchor(component, true);
      c.insets = new Insets(topInset, myIndent, 0, myHorizontalGap);

      myPanel.add(label, c);

      c.gridx = 1;
      c.weightx = 1;
      c.weighty = getWeightY(component);
      c.fill = getFill(component);
      c.anchor = WEST;
      c.insets = new Insets(topInset, myIndent, 0, 0);

      myPanel.add(component, c);

      myLineCount++;
    }

    return this;
  }

  private  int getLabelAnchor(Component component, boolean honorAlignment) {
    if (component instanceof JScrollPane) return honorAlignment && myAlignLabelOnRight ? NORTHEAST : NORTHWEST;
    return honorAlignment && myAlignLabelOnRight ? EAST : WEST;
  }

  private static int getFill(Component component) {
    if (component instanceof JComboBox) {
      return NONE;
    }
    else if (component instanceof JScrollPane) {
      return BOTH;
    }
    else if (component instanceof JTextField && ((JTextField)component).getColumns() != 0) {
      return NONE;
    }
    return HORIZONTAL;
  }

  private static int getWeightY(Component component) {
    if (component instanceof JScrollPane) return 1;
    return 0;
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public int getLineCount() {
    return myLineCount;
  }

  public FormBuilder setAlignLabelOnRight(boolean alignLabelOnRight) {
    myAlignLabelOnRight = alignLabelOnRight;
    return this;
  }

  public FormBuilder setVertical(boolean vertical) {
    myVertical = vertical;
    return this;
  }

  public FormBuilder setVerticalGap(int verticalGap) {
    myVerticalGap = verticalGap;
    return this;
  }

  public FormBuilder setHorizontalGap(int horizontalGap) {
    myHorizontalGap = horizontalGap;
    return this;
  }

  public FormBuilder setIndent(int indent) {
    myIndent = indent;
    return this;
  }
}
