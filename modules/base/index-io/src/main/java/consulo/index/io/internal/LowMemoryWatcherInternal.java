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
package consulo.index.io.internal;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 21-Jul-22
 */
public abstract class LowMemoryWatcherInternal {
  private static LowMemoryWatcherInternal ourInstance;

  static {
    ServiceLoader<LowMemoryWatcherInternal> loader = ServiceLoader.load(LowMemoryWatcherInternal.class, LowMemoryWatcherInternal.class.getClassLoader());
    Optional<LowMemoryWatcherInternal> first = loader.findFirst();
    ourInstance = first.orElse(null);
  }

  public static Runnable register(Runnable hook) {
    if (ourInstance != null) {
      return ourInstance.registerImpl(hook);
    }
    else {
      return () -> {
      };
    }
  }

  /**
   * Return action for stop watching
   */
  public abstract Runnable registerImpl(Runnable hook);
}
