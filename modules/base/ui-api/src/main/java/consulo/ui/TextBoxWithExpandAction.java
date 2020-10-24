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
package consulo.ui;

import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Text box with two states. Short - when text box looks like default {@link TextBox}, and expanded - when text are splitted and showed in large view
 *
 * Some themes can not support inline actions, in this case it will show button with 'editButtonImage' icon, and will show text dialog on click
 *
 * @author VISTALL
 * @since 2019-04-26
 */
public interface TextBoxWithExpandAction extends TextBox {
  @Nonnull
  static TextBoxWithExpandAction create(@Nullable Image editButtonImage, @Nonnull String dialogTitle, @Nonnull Function<String, List<String>> parser, @Nonnull Function<List<String>, String> joiner) {
    return UIInternal.get()._Components_textBoxWithExpandAction(editButtonImage, dialogTitle, parser, joiner);
  }

  @Nonnull
  TextBoxWithExpandAction setDialogTitle(@Nonnull String text);
}
