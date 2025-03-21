/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.application.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.progress.ProgressIndicator;
import jakarta.annotation.Nonnull;

/**
 * An activity to be executed in background on IDE startup. It may load some classes or other configuration
 * so that when something (e.g. an action) is invoked the first time in the UI, there's no visible pause
 * while required stuff is being lazily loaded.
 *
 * @author peter
 * @since 144
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PreloadingActivity {
  /**
   * Perform the preloading
   * @param indicator a progress indicator for the background preloading process.
   *                  Canceled if the application has exited.
   *                  Long actions should periodically perform <code>indicator.checkCanceled()</code>.
   */
  public abstract void preload(@Nonnull ProgressIndicator indicator);
}
