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
package consulo.ui.layout;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.internal.UIInternal;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.EventListener;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public interface FoldoutLayout extends Layout {
  @FunctionalInterface
  static interface StateListener extends EventListener {
    @RequiredUIAccess
    void stateChanged(boolean state);
  }

  @Nonnull
  static FoldoutLayout create(@Nonnull LocalizeValue titleValue, @Nonnull Component component) {
    return create(titleValue, component, true);
  }

  @Nonnull
  static FoldoutLayout create(@Nonnull LocalizeValue titleValue, @Nonnull Component component, boolean state) {
    return UIInternal.get()._Layouts_foldout(titleValue, component, state);
  }

  @Nonnull
  @RequiredUIAccess
  FoldoutLayout setState(boolean showing);

  @Nonnull
  @RequiredUIAccess
  FoldoutLayout setTitle(@Nonnull LocalizeValue title);

  @Nonnull
  Disposable addStateListener(@Nonnull StateListener stateListener);
}
