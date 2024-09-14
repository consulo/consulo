/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.textfield.TextField;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-05-10
 */
public class WebIntBoxImpl extends VaadinComponentDelegate<WebIntBoxImpl.Vaadin> implements IntBox {
  public class Vaadin extends TextField implements FromVaadinComponentWrapper {
    @Nullable
    @Override
    public Component toUIComponent() {
      return WebIntBoxImpl.this;
    }
  }

  public WebIntBoxImpl(int value) {
    setValue(value, false);
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Nullable
  @Override
  public Integer getValue() {
    return Integer.parseInt(getVaadinComponent().getValue());
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {
    getVaadinComponent().setValue(String.valueOf(value));
  }

  @Override
  public void setPlaceholder(@Nullable String text) {
  }

  @Override
  public void setRange(int min, int max) {
  }

  @Nonnull
  @Override
  public Disposable addValidator(@Nonnull Validator<Integer> validator) {
    return () -> {
    };
  }

  @RequiredUIAccess
  @Override
  public boolean validate() {
    return true;
  }
}