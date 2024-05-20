/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.components;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class JBComboBoxLabel extends JPanel {
  private final JLabel myIcon = new JLabel(TargetAWT.to(PlatformIconGroup.generalArrowdown()));
  private final JLabel myText = new JLabel();

  public JBComboBoxLabel() {
    super(new BorderLayout());
    add(myText, BorderLayout.CENTER);
    add(myIcon, BorderLayout.EAST);
  }

  public void setText(String text) {
    myText.setText(text);
  }

  public String getText() {
    return myText.getText();
  }

  public void setIcon(Icon icon) {
    myIcon.setIcon(icon);
  }

  public Icon getIcon() {
    return myIcon.getIcon();
  }

  public void setRegularIcon() {
    myIcon.setIcon(TargetAWT.to(PlatformIconGroup.generalArrowdown()));
  }

  public void setSelectionIcon() {
    myIcon.setIcon(TargetAWT.to(PlatformIconGroup.generalArrowdown()));
  }

  @Override
  public void setForeground(Color color) {
    super.setForeground(color);
    if (myText != null) {
      myText.setForeground(color);
    }
  }
}
