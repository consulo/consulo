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
package consulo.ide.impl.ui.ex.impl.util;

import consulo.ide.impl.idea.util.concurrency.InvokerImpl;
import consulo.disposer.Disposable;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerFactory;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27-Feb-22
 */
@Singleton
public class InvokerFactoryImpl implements InvokerFactory {
  @Nonnull
  @Override
  public Invoker forEventDispatchThread(@Nonnull Disposable parent) {
    return InvokerImpl.forEventDispatchThread(parent);
  }

  @Nonnull
  @Override
  public Invoker forBackgroundPoolWithReadAction(@Nonnull Disposable parent) {
    return InvokerImpl.forBackgroundPoolWithReadAction(parent);
  }

  @Nonnull
  @Override
  public Invoker forBackgroundThreadWithReadAction(@Nonnull Disposable parent) {
    return InvokerImpl.forBackgroundThreadWithReadAction(parent);
  }

  @Nonnull
  @Override
  public Invoker forBackgroundThreadWithoutReadAction(@Nonnull Disposable parent) {
    return InvokerImpl.forBackgroundThreadWithoutReadAction(parent);
  }
}
