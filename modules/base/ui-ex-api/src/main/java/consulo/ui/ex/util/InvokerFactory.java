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
package consulo.ui.ex.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24-Feb-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface InvokerFactory {
  @Nonnull
  static InvokerFactory getInstance() {
    return Application.get().getInstance(InvokerFactory.class);
  }

  @Nonnull
  Invoker forEventDispatchThread(@Nonnull UIAccess uiAccess, @Nonnull Disposable parent);

  @Nonnull
  Invoker forBackgroundPoolWithReadAction(@Nonnull Disposable parent);

  @Nonnull
  Invoker forBackgroundThreadWithReadAction(@Nonnull Disposable parent);

  @Nonnull
  Invoker forBackgroundThreadWithoutReadAction(@Nonnull Disposable parent);
}
