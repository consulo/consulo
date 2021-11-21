/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class GotItPanel {
  JButton myButton;
  JPanel myRoot;
  JLabel myTitle;
  JEditorPane myMessage;


  private void createUIComponents() {
    myButton = new JButton() {
      @Override
      public boolean isDefaultButton() {
        return true;
      }
    };
    myTitle = new JLabel();
    Font font = myTitle.getFont();
    myTitle.setFont(font.deriveFont((float)JBUI.scaleFontSize(20)));
    myMessage = new JEditorPane("text/html", "<html></html>");
    myMessage.setEditorKit(JBHtmlEditorKit.create());
    myMessage.setEditable(false);
    myMessage.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    myMessage.setFont(JBUI.Fonts.biggerFont());
  }
}
