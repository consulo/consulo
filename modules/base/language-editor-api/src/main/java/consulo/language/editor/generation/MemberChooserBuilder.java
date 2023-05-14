/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.generation;

import consulo.application.Application;
import consulo.language.editor.internal.MemberChooserBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.dataholder.UserDataHolder;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 18-Aug-22
 */
public interface MemberChooserBuilder<T extends ClassMember> {
  @Nonnull
  static <C extends ClassMember> MemberChooserBuilder<C> create(@Nonnull C[] elements) {
    return Application.get().getInstance(MemberChooserBuilderFactory.class).newBuilder(elements);
  }

  @Nonnull
  MemberChooserBuilder<T> withTitle(@Nonnull LocalizeValue titleValue);

  @Nonnull
  MemberChooserBuilder<T> withOption(@Nonnull KeyWithDefaultValue<Boolean> dataKey, @Nonnull LocalizeValue optionTitle);

  @Nonnull
  MemberChooserBuilder<T> withEmptySelection();

  @Nonnull
  MemberChooserBuilder<T> withMultipleSelection();

  /**
   * Return dataholder of selected data. Keys will be used from options, and ClassMember#KEY_OF_ARRAY
   */
  @RequiredUIAccess
  void showAsync(@Nonnull Project project, @Nonnull Consumer<UserDataHolder> consumer);
}
