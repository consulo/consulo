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

package com.intellij.lang.cacheBuilder;

import com.intellij.openapi.fileTypes.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class CacheBuilderRegistryImpl extends CacheBuilderRegistry {
  @Override
  @Nullable
  public WordsScanner getCacheBuilder(@Nonnull FileType fileType) {
    for(CacheBuilderEP ep: CacheBuilderEP.EP_NAME.getExtensionList()) {
      if (ep.getFileType().equals(fileType.getId())) {
        return ep.getWordsScanner();
      }
    }
    return null;
  }
}
