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

import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public interface Button extends Component {
  @NotNull
  static Button create(@NotNull String text) {
    return UIInternal.get()._Components_button(text);
  }

  @NotNull
  static Button create(@NotNull String text, @NotNull @RequiredUIAccess Button.ClickHandler clickHandler) {
    Button button = UIInternal.get()._Components_button(text);
    button.addListener(ClickHandler.class, clickHandler);
    return button;
  }

  interface ClickHandler extends EventListener {
    @RequiredUIAccess
    void onClick();
  }

  @NotNull
  String getText();

  @RequiredUIAccess
  void setText(@NotNull String text);

  default void addClickListener(@RequiredUIAccess ClickHandler listener) {
    addListener(ClickHandler.class, listener);
  }
}
