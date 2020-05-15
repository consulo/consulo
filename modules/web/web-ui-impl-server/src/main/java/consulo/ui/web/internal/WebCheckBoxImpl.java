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
package consulo.ui.web.internal;

import com.intellij.openapi.util.Comparing;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.KeyCode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebCheckBoxImpl extends WebBooleanValueComponentBase<WebCheckBoxImpl.Vaadin> implements CheckBox {

  public static class Vaadin extends VaadinBooleanValueComponentBase {
    public void setText(@Nonnull final String text) {
      if (Comparing.equal(getState().caption, text)) {
        return;
      }

      getState().caption = text;

      markAsDirty();
    }
  }

  public WebCheckBoxImpl() {
    super(false);
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  @Nonnull
  public String getText() {
    return getVaadinComponent().getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull LocalizeValue textValue) {
    UIAccess.assertIsUIThread();

    getVaadinComponent().setText(textValue.getValue());
  }

  @Override
  public void setMnemonicKey(@Nullable KeyCode key) {

  }

  @Override
  public void setMnemonicTextIndex(int index) {

  }
}
