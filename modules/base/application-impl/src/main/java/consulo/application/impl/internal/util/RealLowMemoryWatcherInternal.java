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
package consulo.application.impl.internal.util;

import consulo.application.util.LowMemoryWatcher;
import consulo.index.io.internal.LowMemoryWatcherInternal;

/**
 * @author VISTALL
 * @since 21-Jul-22
 */
public class RealLowMemoryWatcherInternal extends LowMemoryWatcherInternal {
  @Override
  public Runnable registerImpl(Runnable hook) {
    LowMemoryWatcher watcher = LowMemoryWatcher.register(hook);
    return watcher::stop;
  }
}
