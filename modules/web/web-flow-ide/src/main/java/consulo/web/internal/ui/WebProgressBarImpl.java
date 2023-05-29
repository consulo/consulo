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

import consulo.ui.Component;
import consulo.ui.ProgressBar;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class WebProgressBarImpl extends VaadinComponentDelegate<WebProgressBarImpl.Vaadin> implements ProgressBar {
  public  class Vaadin extends com.vaadin.flow.component.progressbar.ProgressBar implements FromVaadinComponentWrapper {

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebProgressBarImpl.this;
    }
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  public void setIndeterminate(boolean value) {
    getVaadinComponent().setIndeterminate(value);
  }

  @Override
  public boolean isIndeterminate() {
    return getVaadinComponent().isIndeterminate();
  }

  @Override
  public void setMinimum(int value) {
    getVaadinComponent().setMin(value);
  }

  @Override
  public void setMaximum(int value) {
    getVaadinComponent().setMax(value);
  }

  @Override
  public void setValue(int value) {
    getVaadinComponent().setValue(value);
  }
}
