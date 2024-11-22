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
package consulo.ui.ex.awt;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public class JBTextField extends JTextField implements ComponentWithEmptyText {
  private TextComponentEmptyText myEmptyText;

  public JBTextField() {
    init();
  }

  public JBTextField(int columns) {
    super(columns);
    init();
  }

  public JBTextField(String text) {
    super(text);
    init();
  }

  public JBTextField(String text, int columns) {
    super(text, columns);
    init();
  }

  private void init() {
    myEmptyText = new TextComponentEmptyText(this) {
      @Override
      protected boolean isStatusVisible() {
        Object function = getClientProperty("StatusVisibleFunction");
        if (function instanceof Predicate) {
          //noinspection unchecked
          return ((Predicate<JTextComponent>)function).test(JBTextField.this);
        }
        return super.isStatusVisible();
      }

      @Override
      protected Rectangle getTextComponentBound() {
        return getEmptyTextComponentBounds(super.getTextComponentBound());
      }
    };
  }

  protected Rectangle getEmptyTextComponentBounds(Rectangle bounds) {
    return bounds;
  }

  public void setTextToTriggerEmptyTextStatus(String t) {
    myEmptyText.setTextToTriggerStatus(t);
  }

  @Override
  public void setText(String t) {
    super.setText(t);
    UIUtil.resetUndoRedoActions(this);
  }

  @Nonnull
  @Override
  public StatusText getEmptyText() {
    return myEmptyText;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (!myEmptyText.getStatusTriggerText().isEmpty() && myEmptyText.isStatusVisible()) {
      g.setColor(getBackground());

      Rectangle rect = new Rectangle(getSize());
      JBInsets.removeFrom(rect, getInsets());
      JBInsets.removeFrom(rect, getMargin());
      ((Graphics2D)g).fill(rect);

      g.setColor(getForeground());
    }
    myEmptyText.paintStatusText(g);
  }
}
