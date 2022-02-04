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
package consulo.ui.dialog;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 13/12/2021
 */
public interface Dialog<V> {
  Key<Dialog<Object>> KEY = Key.create(Dialog.class.getName());

  @Nonnull
  @RequiredUIAccess
  AsyncResult<V> showAsync();

  /**
   * Will done showAsync result with value
   */
  void doOkAction(@Nullable V value);

  /**
   * Will reject showAsync result
   */
  void doCancelAction();

  @Nonnull
  DialogDescriptor<V> getDescriptor();
}
