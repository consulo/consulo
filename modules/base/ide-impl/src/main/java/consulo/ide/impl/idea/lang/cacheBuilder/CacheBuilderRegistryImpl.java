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

package consulo.ide.impl.idea.lang.cacheBuilder;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.language.cacheBuilder.CacheBuilderRegistry;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class CacheBuilderRegistryImpl extends CacheBuilderRegistry {
  private final Application myApplication;

  @Inject
  public CacheBuilderRegistryImpl(Application application) {
    myApplication = application;
  }

  @Override
  @Nullable
  public WordsScanner getCacheBuilder(@Nonnull FileType fileType) {
    ExtensionPoint<FileWordsScannerProvider> extensionPoint = myApplication.getExtensionPoint(FileWordsScannerProvider.class);
    Map<FileType, WordsScanner> map = extensionPoint.getOrBuildCache(FileWordsScannerProvider.SCANNERS);
    return map.get(fileType);
  }
}
