/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application;

import consulo.annotation.DeprecationInfo;
import consulo.component.internal.RootComponentHolder;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides access to the <code>Application</code>.
 */
public class ApplicationManager {
  protected static Application ourApplication = null;

  /**
   * Gets Application.
   *
   * @return <code>Application</code>
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use Application.get()")
  public static Application getApplication() {
    return ourApplication;
  }

  public static void setApplication(@Nullable Application instance) {
    ourApplication = instance;
    RootComponentHolder.setRootComponent(instance);
  }

  public static void setApplication(@Nonnull Application instance, @Nonnull Disposable parent) {
    Disposer.register(parent, () -> {
      setApplication(null);
      RootComponentHolder.setRootComponent(null);
    });

    setApplication(instance);
  }
}
