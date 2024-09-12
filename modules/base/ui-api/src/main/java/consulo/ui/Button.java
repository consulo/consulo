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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public interface Button extends Component {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #create(LocalizeValue)")
  static Button create(@Nonnull String text) {
    return create(LocalizeValue.of(text));
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #create(LocalizeValue, ComponentEventListener<Component, ClickEvent>)")
  static Button create(@Nonnull String text, @Nonnull ComponentEventListener<Component, ClickEvent> clickListener) {
    return create(LocalizeValue.of(text), clickListener);
  }

  @Nonnull
  static Button create(@Nonnull LocalizeValue text) {
    return UIInternal.get()._Components_button(text);
  }

  @Nonnull
  static Button create(@Nonnull LocalizeValue text, @Nonnull ComponentEventListener<Component, ClickEvent> clickListener) {
    Button button = create(text);
    button.addClickListener(clickListener);
    return button;
  }

  @Nonnull
  LocalizeValue getText();

  @RequiredUIAccess
  void setText(@Nonnull LocalizeValue text);

  @Nullable
  Image getIcon();

  @RequiredUIAccess
  void setIcon(@Nullable Image image);
}
