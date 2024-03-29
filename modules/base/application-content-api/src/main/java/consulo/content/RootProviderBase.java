/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.content;

import consulo.disposer.Disposable;
import consulo.proxy.EventDispatcher;

import jakarta.annotation.Nonnull;

/**
 *  @author dsl
 */
public abstract class RootProviderBase implements RootProvider {
  protected final EventDispatcher<RootSetChangedListener> myDispatcher = EventDispatcher.create(RootSetChangedListener.class);

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
    myDispatcher.removeListener(listener);
  }

  @Override
  public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener, @Nonnull Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  public void fireRootSetChanged() {
    myDispatcher.getMulticaster().rootSetChanged(this);
  }
}
