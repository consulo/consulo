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
package com.intellij.openapi.options.ex;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import consulo.annotation.DeprecationInfo;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 02/12/2020
 * <p>
 * Interface for accessing settings inside already created dialog
 */
public interface Settings {
  Key<Settings> KEY = Key.create("options.editor");

  @Nullable
  SearchableConfigurable findConfigurableById(@Nonnull String configurableId);

  <T extends Configurable> T findConfigurable(Class<T> configurableClass);

  @Deprecated
  @DeprecationInfo("Use #select(Configurable)")
  AsyncResult<Void> select(Configurable configurable);

  @Deprecated
  @DeprecationInfo("Use #select(Configurable)")
  AsyncResult<Void> select(Configurable configurable, final String text);

  @Deprecated
  @DeprecationInfo("Use #select(Configurable)")
  AsyncResult<Void> clearSearchAndSelect(Configurable configurable);

  @Nonnull
  <T extends UnnamedConfigurable> AsyncResult<T> select(@Nonnull Class<T> clazz);
}
