/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.impl;

import com.intellij.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Alert;
import consulo.ui.AlertValueRemember;
import consulo.ui.NotificationType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-13
 */
public abstract class BaseAlert<V> implements Alert<V> {
  protected class ButtonImpl {
    public int myButtonId;
    public LocalizeValue myText;
    public Supplier<V> myValue;
    public boolean myDefault;

    ButtonImpl(int buttonId, LocalizeValue text, Supplier<V> value) {
      myButtonId = buttonId;
      myText = text;
      myValue = value;
    }
  }

  protected LocalizeValue getText(ButtonImpl button) {
    if (button.myText != null) {
      return button.myText;
    }

    switch (button.myButtonId) {
      case YES:
        return CommonLocalize.buttonYes();
      case NO:
        return CommonLocalize.buttonNo();
      case OK:
        return CommonLocalize.buttonOk();
      case CANCEL:
        return CommonLocalize.buttonCancel();
      case APPLY:
        return CommonLocalize.buttonApply();
      default:
        throw new UnsupportedOperationException(String.valueOf(button.myButtonId));
    }
  }

  protected LocalizeValue myText = LocalizeValue.empty();
  // FIXME [VISTALL] usage Application.getName();
  protected LocalizeValue myTitle = LocalizeValue.localizeTODO("Consulo");
  protected NotificationType myType = NotificationType.INFO;
  protected List<ButtonImpl> myButtons = new ArrayList<>();
  protected AlertValueRemember<V> myRemember;

  protected Supplier<V> myExitValue;

  @Nonnull
  @Override
  public Alert<V> remember(@Nonnull AlertValueRemember<V> remember) {
    myRemember = remember;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> asWarning() {
    myType = NotificationType.WARNING;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> asQuestion() {
    myType = NotificationType.QUESTION;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> asError() {
    myType = NotificationType.ERROR;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> button(int buttonId, @Nonnull Supplier<V> valueGetter) {
    myButtons.add(new ButtonImpl(buttonId, null, valueGetter));
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> button(@Nonnull LocalizeValue textValue, @Nonnull Supplier<V> valueGetter) {
    myButtons.add(new ButtonImpl(-1, textValue, valueGetter));
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> asDefaultButton() {
    if (myButtons.isEmpty()) {
      throw new IllegalArgumentException();
    }
    ContainerUtil.getLastItem(myButtons).myDefault = true;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> asExitButton() {
    if (myButtons.isEmpty()) {
      throw new IllegalArgumentException();
    }
    myExitValue = ContainerUtil.getLastItem(myButtons).myValue;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> exitValue(@Nonnull Supplier<V> valueGetter) {
    myExitValue = valueGetter;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> title(@Nonnull LocalizeValue text) {
    myTitle = text;
    return this;
  }

  @Nonnull
  @Override
  public Alert<V> text(@Nonnull LocalizeValue text) {
    myText = text;
    return this;
  }
}
