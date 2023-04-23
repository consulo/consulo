/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ui.layout;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 23/04/2023
 */
public interface LoadingLayout<InnerLayout> extends Layout {
  @Nonnull
  static <L extends Layout> LoadingLayout<L> create(@Nonnull L innerLayout, @Nonnull Disposable parent) {
    return UIInternal.get()._Layouts_LoadingLayout(innerLayout, parent);
  }

  /**
   * Execute valueGetter in another thread, and when value is got - uiSetter will be invoked
   */
  @RequiredUIAccess
  <Value> Future<Value> startLoading(@Nonnull Supplier<Value> valueGetter,
                                     @RequiredUIAccess @Nonnull BiConsumer<InnerLayout, Value> uiSetter);

  @RequiredUIAccess
  void startLoading();

  @RequiredUIAccess
  void startLoading(@Nonnull LocalizeValue loadingText);

  @RequiredUIAccess
  void stopLoading(@RequiredUIAccess @Nonnull Consumer<InnerLayout> consumer);

  @RequiredUIAccess
  void setLoadingText(@Nonnull LocalizeValue loadingText);
}
