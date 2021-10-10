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
import consulo.ui.RadioButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebRadioButtonImpl extends WebBooleanValueComponentBase<WebRadioButtonImpl.Vaadin> implements RadioButton {

  public static class Vaadin extends VaadinBooleanValueComponentBase {
    public void setText(@Nonnull final LocalizeValue text) {
      if (Comparing.equal(getState().caption, text)) {
        return;
      }

      getState().caption = text.getValue();

      markAsDirty();
    }
  }

  private LocalizeValue myText = LocalizeValue.empty();

  public WebRadioButtonImpl(boolean selected, LocalizeValue text) {
    super(selected);
    setLabelText(text);
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  @Nonnull
  public LocalizeValue getLabelText() {
    return myText;
  }

  @RequiredUIAccess
  @Override
  public void setLabelText(@Nonnull final LocalizeValue text) {
    UIAccess.assertIsUIThread();

    myText = text;
    
    getVaadinComponent().setText(text);
  }
}
