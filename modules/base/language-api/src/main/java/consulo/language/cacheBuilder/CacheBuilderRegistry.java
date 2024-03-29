/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.cacheBuilder;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The cache builder registry allows to register custom cache builders for file types which
 * are not based on a language. For language file types, the words scanner should be returned
 * from {@link consulo.language.findUsage.FindUsagesProvider#getWordsScanner()}.
 *
 * @author yole
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CacheBuilderRegistry {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use injecting provider")
  public static CacheBuilderRegistry getInstance() {
    return Application.get().getInstance(CacheBuilderRegistry.class);
  }

  /**
   * Returns the cache builder registered for the specified file type.
   *
   * @param fileType the file type for which the cache builder is registered.
   * @return the cache builder, or null if none was registered.
   */
  @Nullable
  public abstract WordsScanner getCacheBuilder(@Nonnull FileType fileType);
}
