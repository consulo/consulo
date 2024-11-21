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
package consulo.ide.impl.idea.openapi.vcs.ui;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.SearchTextField;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.style.StyleManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.event.KeyEvent;

/**
 * @author irengrig
 */
public abstract class SearchFieldAction extends AnAction implements CustomComponentAction {
  private final JPanel myComponent;
  private final SearchTextField myField;

  public SearchFieldAction(String text) {
    super("Find: ");
    myField = new SearchTextField(true) {
      @Override
      protected boolean preprocessEventForTextField(KeyEvent e) {
        if ((KeyEvent.VK_ENTER == e.getKeyCode()) || ('\n' == e.getKeyChar())) {
          e.consume();
          addCurrentTextToHistory();
          actionPerformed(null);
        }
        return super.preprocessEventForTextField(e);
      }

      @Override
      protected void onFocusLost() {
        myField.addCurrentTextToHistory();
        actionPerformed(null);
      }

      @Override
      protected void onFieldCleared() {
        actionPerformed(null);
      }
    };
    Border border = myField.getBorder();
    Border emptyBorder = IdeBorderFactory.createEmptyBorder(3, 0, 2, 0);
    if (border instanceof CompoundBorder) {
      if (!StyleManager.get().getCurrentStyle().isDark()) {
        myField.setBorder(new CompoundBorder(emptyBorder, ((CompoundBorder)border).getInsideBorder()));
      }
    }
    else {
      myField.setBorder(emptyBorder);
    }

    myComponent = new JPanel();
    final BoxLayout layout = new BoxLayout(myComponent, BoxLayout.X_AXIS);
    myComponent.setLayout(layout);
    if (text.length() > 0) {
      final JLabel label = new JLabel(text);
      //label.setFont(label.getFont().deriveFont(Font.ITALIC));
      label.setForeground(
        StyleManager.get().getCurrentStyle().isDark()
          ? UIUtil.getLabelForeground()
          : UIUtil.getInactiveTextColor()
      );
      label.setBorder(BorderFactory.createEmptyBorder(0,3,0,0));
      myComponent.add(label);
    }
    myComponent.add(myField);
  }

  public String getText() {
    return myField.getText();
  }

  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    return myComponent;
  }

  public void setTextFieldFg(boolean inactive) {
    myField.getTextEditor().setForeground(inactive ? UIUtil.getInactiveTextColor() : UIUtil.getActiveTextColor());
  }
}
