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

import com.intellij.CommonBundle;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.AlertBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-13
 */
public abstract class BaseAlertBuilder<V> implements AlertBuilder<V> {
  protected enum Type {
    INFO,
    WARN,
    ERROR,
    QUESTION
  }

  protected class ButtonImpl {
    public int myButtonId;
    public String myText;
    public Supplier<V> myValue;
    public boolean myDefault;

    ButtonImpl(int buttonId, String text, Supplier<V> value) {
      myButtonId = buttonId;
      myText = text;
      myValue = value;
    }
  }

  protected String getText(ButtonImpl button) {
    if (button.myText != null) {
      return button.myText;
    }

    switch (button.myButtonId) {
      case CANCEL:
        return CommonBundle.getCancelButtonText();
      default:
        throw new UnsupportedOperationException(String.valueOf(button.myButtonId));
    }
  }

  protected String myText;
  protected String myTitle;
  protected Type myType = Type.INFO;
  protected List<ButtonImpl> myButtons = new ArrayList<>();

  @Nonnull
  @Override
  public AlertBuilder<V> asWarning() {
    myType = Type.WARN;
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> asQuestion() {
    myType = Type.QUESTION;
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> asError() {
    myType = Type.ERROR;
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> button(int buttonId, @Nonnull Supplier<V> valueGetter) {
    myButtons.add(new ButtonImpl(buttonId, null, valueGetter));
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> button(@Nonnull String text, @Nonnull Supplier<V> valueGetter) {
    myButtons.add(new ButtonImpl(-1, text, valueGetter));
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> markDefault() {
    if (myButtons.isEmpty()) {
      throw new IllegalArgumentException();
    }
    ContainerUtil.getLastItem(myButtons).myDefault = true;
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> title(@Nonnull String text) {
    myTitle = text;
    return this;
  }

  @Nonnull
  @Override
  public AlertBuilder<V> text(@Nonnull String text) {
    myText = text;
    return this;
  }
}
