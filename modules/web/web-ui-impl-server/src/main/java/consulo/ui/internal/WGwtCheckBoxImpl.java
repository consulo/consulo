/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import com.intellij.openapi.util.Comparing;
import consulo.ui.CheckBox;
import consulo.ui.KeyCode;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtCheckBoxImpl extends WGwtBooleanValueComponentImpl implements CheckBox {
  public WGwtCheckBoxImpl() {
    super(false);
  }

  @Override
  @Nonnull
  public String getText() {
    return getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull final String text) {
    UIAccess.assertIsUIThread();

    if (Comparing.equal(getState().caption, text)) {
      return;
    }

    getState().caption = text;

    getState().caption = text;

    markAsDirty();
  }

  @Override
  public void setMnemonicKey(@Nullable KeyCode key) {

  }

  @Override
  public void setMnemonicTextIndex(int index) {

  }
}
