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
package consulo.language.editor.internal;

import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.generation.MemberChooserBuilder;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 20-Aug-22
 */
public abstract class MemberChooserBuilderBase<T extends ClassMember> implements MemberChooserBuilder<T> {
  protected final T[] myElements;
  protected LocalizeValue myTitle = LocalizeValue.localizeTODO("Select Member");
  protected List<Pair<KeyWithDefaultValue<Boolean>, LocalizeValue>> myOptions = new ArrayList<>();
  protected boolean myAllowEmptySelection = false;
  protected boolean myAllowMultipleSelection = false;

  public MemberChooserBuilderBase(T[] elements) {
    myElements = elements;
  }

  @Nonnull
  @Override
  public MemberChooserBuilder<T> withTitle(@Nonnull LocalizeValue titleValue) {
    myTitle = titleValue;
    return this;
  }

  @Nonnull
  @Override
  public MemberChooserBuilder<T> withOption(@Nonnull KeyWithDefaultValue<Boolean> dataKey, @Nonnull LocalizeValue optionTitle) {
    myOptions.add(Pair.create(dataKey, optionTitle));
    return this;
  }

  @Nonnull
  @Override
  public MemberChooserBuilder<T> withEmptySelection() {
    myAllowEmptySelection = true;
    return this;
  }

  @Nonnull
  @Override
  public MemberChooserBuilder<T> withMultipleSelection() {
    myAllowMultipleSelection = true;
    return this;
  }
}
