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
package consulo.ui.web.internal.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public abstract class WebTwoComponentSplitLayoutBase<V extends WebSplitLayoutVaadinBase<?>> extends VaadinComponentDelegate<V> implements TwoComponentSplitLayout {
  @Override
  public void setProportion(int percent) {
    getVaadinComponent().setProportion(percent);
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public TwoComponentSplitLayout setFirstComponent(@Nonnull Component component) {
    getVaadinComponent().setFirstComponent(TargetVaddin.to(component));
    return this;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public TwoComponentSplitLayout setSecondComponent(@Nonnull Component component) {
    getVaadinComponent().setSecondComponent(TargetVaddin.to(component));
    return this;
  }
}
