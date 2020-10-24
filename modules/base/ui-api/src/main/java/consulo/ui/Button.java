/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public interface Button extends Clickable {
  @Nonnull
  static Button create(@Nonnull String text) {
    return UIInternal.get()._Components_button(text);
  }

  @Nonnull
  static Button create(@Nonnull String text, @Nonnull @RequiredUIAccess ClickListener clickListener) {
    Button button = UIInternal.get()._Components_button(text);
    button.addClickListener(clickListener);
    return button;
  }

  @Nonnull
  String getText();

  @RequiredUIAccess
  void setText(@Nonnull String text);
}
