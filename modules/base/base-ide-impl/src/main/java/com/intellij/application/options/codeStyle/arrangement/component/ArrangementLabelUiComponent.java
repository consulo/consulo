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
package com.intellij.application.options.codeStyle.arrangement.component;

import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 * @since 3/12/13 11:09 AM
 */
public class ArrangementLabelUiComponent extends AbstractArrangementUiComponent {

  @Nonnull
  private final ArrangementAtomMatchCondition myCondition;
  @Nonnull
  private final JLabel                        myLabel;

  public ArrangementLabelUiComponent(@Nonnull ArrangementSettingsToken token) {
    super(token);
    myCondition = new ArrangementAtomMatchCondition(token);
    myLabel = new JLabel(token.getRepresentationValue());
  }

  @Nonnull
  @Override
  public ArrangementSettingsToken getToken() {
    return myCondition.getType();
  }

  @Override
  public void chooseToken(@Nonnull ArrangementSettingsToken data) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    return myCondition;
  }

  @Override
  protected JComponent doGetUiComponent() {
    return myLabel;
  }

  @Override
  public boolean isSelected() {
    return true;
  }

  @Override
  public void setSelected(boolean selected) {
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void setEnabled(boolean enabled) {
  }

  @Override
  protected void doReset() {
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return myLabel.getBaseline(width, height);
  }

  @Override
  public void handleMouseClickOnSelected() {
    setSelected(false);
  }
}
