/*
 * Copyright 2013-2021 consulo.io
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

import consulo.ui.IntSlider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.web.internal.base.WebUnsupportedComponent;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 01/08/2021
 */
public class WebIntSliderImpl extends WebUnsupportedComponent implements IntSlider {
  public WebIntSliderImpl(int min, int max, int value) {
  }

  @Override
  public void setRange(int min, int max) {

  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Nullable
  @Override
  public Integer getValue() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {

  }
}
