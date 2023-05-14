/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.vfs.newvfs.persistent;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.caches.CachesInvalidator;
import consulo.ide.impl.idea.openapi.vfs.newvfs.persistent.FSRecords;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20/03/2021
 */
@ExtensionImpl
public class FSCachesInvalidator extends CachesInvalidator {
  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.localizeTODO("Invalidate file system cache (also clear local history)");
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  @Override
  public void invalidateCaches() {
    FSRecords.invalidateCaches();
  }
}
