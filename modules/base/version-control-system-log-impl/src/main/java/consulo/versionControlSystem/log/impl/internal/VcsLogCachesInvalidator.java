/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CachesInvalidator;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.log.impl.internal.util.PersistentUtil;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class VcsLogCachesInvalidator extends CachesInvalidator {
  private static final Logger LOG = Logger.getInstance(VcsLogCachesInvalidator.class);

  public synchronized boolean isValid() {
    if (PersistentUtil.getCorruptionMarkerFile().exists()) {
      boolean deleted = FileUtil.deleteWithRenaming(PersistentUtil.LOG_CACHE);
      if (!deleted) {
        // if could not delete caches, ensure that corruption marker is still there
        FileUtil.createIfDoesntExist(PersistentUtil.getCorruptionMarkerFile());
      }
      else {
        LOG.debug("Deleted VCS Log caches at " + PersistentUtil.LOG_CACHE);
      }
      return deleted;
    }
    return true;
  }

  @Nonnull
  @Override
  public LocalizeValue getDescription() {
    return LocalizeValue.localizeTODO("Invalidate VCS log");
  }

  @Override
  public void invalidateCaches() {
    if (PersistentUtil.LOG_CACHE.exists()) {
      String[] children = PersistentUtil.LOG_CACHE.list();
      if (!ArrayUtil.isEmpty(children)) {
        FileUtil.createIfDoesntExist(PersistentUtil.getCorruptionMarkerFile());
      }
    }
  }
}
