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
package consulo.ide.impl.idea.application.options.codeStyle.arrangement.component;

import consulo.language.codeStyle.ui.internal.arrangement.ArrangementConstants;
import consulo.util.lang.StringUtil;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author Denis Zhdanov
 * @since 3/12/13 11:18 AM
 */
public class ArrangementTextFieldUiComponent extends AbstractArrangementUiComponent {

  @Nonnull
  private final JBTextField myTextField = new JBTextField(20);
  @Nonnull
  private final Alarm       myAlarm     = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  @Nonnull
  private final ArrangementSettingsToken myToken;

  public ArrangementTextFieldUiComponent(@Nonnull ArrangementSettingsToken token) {
    super(token);
    myToken = token;
    myTextField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        scheduleUpdate();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        scheduleUpdate();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        scheduleUpdate();
      }
    });
  }

  private void scheduleUpdate() {
    myAlarm.cancelAllRequests();
    myAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        fireStateChanged();
      }
    }, ArrangementConstants.TEXT_UPDATE_DELAY_MILLIS);
  }

  @Nonnull
  @Override
  public ArrangementSettingsToken getToken() {
    return myToken;
  }

  @Override
  public void chooseToken(@Nonnull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    String text = myTextField.getText();
    return new ArrangementAtomMatchCondition(myToken, StringUtil.isEmpty(text) ? "" : text.trim());
  }

  @Override
  protected JComponent doGetUiComponent() {
    return myTextField;
  }

  @Override
  public boolean isSelected() {
    return !StringUtil.isEmpty(myTextField.getText());
  }

  @Override
  public void setSelected(boolean selected) {
  }

  @Override
  public boolean isEnabled() {
    return myTextField.isEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
    myTextField.setEnabled(enabled);
  }

  @Override
  public void setData(@Nonnull Object data) {
    if (data instanceof String) {
      myTextField.setText(data.toString());
    }
  }

  @Override
  public void doReset() {
    myTextField.setText("");
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return myTextField.getBaseline(width, height);
  }

  @Override
  public void handleMouseClickOnSelected() {
    setSelected(false);
  }
}
