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
package consulo.ide.impl.idea.compiler.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.compiler.TranslatingCompilerFilesMonitor;
import consulo.ide.impl.idea.ide.caches.CachesInvalidator;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
* @author VISTALL
* @since 18-Jun-22
*/
@ExtensionImpl
class TranslactionCompilerMonitorCachesInvalidator extends CachesInvalidator {
  private final Provider<TranslatingCompilerFilesMonitor> myTranslatingCompilerFilesMonitorProvider;

  @Inject
  TranslactionCompilerMonitorCachesInvalidator(Provider<TranslatingCompilerFilesMonitor> translatingCompilerFilesMonitorProvider) {
    myTranslatingCompilerFilesMonitorProvider = translatingCompilerFilesMonitorProvider;
  }

  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.localizeTODO("Invalidate compiler cache");
  }

  @Override
  public void invalidateCaches() {
    TranslatingCompilerFilesMonitorImpl monitor = (TranslatingCompilerFilesMonitorImpl)myTranslatingCompilerFilesMonitorProvider.get();

    monitor.invalidate();
  }
}
