/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author cdr
 */
public class TitledSeparator extends JPanel {
  public static final int TOP_INSET = 7;
  public static final int BOTTOM_INSET = 5;
  public static final int SEPARATOR_LEFT_INSET = 6;
  public static final int SEPARATOR_RIGHT_INSET = 3;

  public static final Border EMPTY_BORDER = IdeBorderFactory.createEmptyBorder(TOP_INSET, 0, BOTTOM_INSET, 0);

  protected final JBLabel myLabel = new JBLabel() {
    @Override
    public Font getFont() {
      return UIUtil.getTitledBorderFont();
    }
  };
  protected final JSeparator mySeparator = new JSeparator(SwingConstants.HORIZONTAL);
  private String myOriginalText;

  @Nonnull
  public static Border createEmptyBorder() {
    return JBUI.Borders.empty(TOP_INSET, 0, BOTTOM_INSET, 0);
  }

  public TitledSeparator() {
    this("");
  }

  public TitledSeparator(String text) {
    this(text, null);
  }

  public TitledSeparator(String text, @Nullable JComponent labelFor) {
    super();
    setLayout(new GridBagLayout());
    add(myLabel, new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    add(mySeparator, new GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                            new Insets(2, SEPARATOR_LEFT_INSET, 0, SEPARATOR_RIGHT_INSET), 0, 0));
    setBorder(EMPTY_BORDER);

    setText(text);
    setLabelFor(labelFor);
  }

  public String getText() {
    return myOriginalText;
  }

  public void setText(String text) {
    myOriginalText = text;
    myLabel.setText(text != null && text.startsWith("<html>") ? text : UIUtil.replaceMnemonicAmpersand(myOriginalText));
  }
  public void setTitleFont(Font font) {
    myLabel.setFont(font);
  }

  public Font getTitleFont() {
    return myLabel.getFont();
  }

  public JLabel getLabel() {
    return myLabel;
  }

  public JSeparator getSeparator() {
    return mySeparator;
  }

  public Component getLabelFor() {
    return myLabel.getLabelFor();
  }

  public void setLabelFor(Component labelFor) {
    myLabel.setLabelFor(labelFor);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myLabel.setEnabled(enabled);
    mySeparator.setEnabled(enabled);
  }
}
