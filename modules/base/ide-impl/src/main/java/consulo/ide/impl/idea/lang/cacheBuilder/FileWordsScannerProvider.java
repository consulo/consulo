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
package consulo.ide.impl.idea.lang.cacheBuilder;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 24-Jun-22
 */
@Extension(ComponentScope.APPLICATION)
public interface FileWordsScannerProvider {
  ExtensionPointCacheKey<FileWordsScannerProvider, Map<FileType, WordsScanner>> SCANNERS = ExtensionPointCacheKey.create("FileWordsScannerProvider", fileWordsScannerProviders -> {
    Map<FileType, WordsScanner> scannerMap = new HashMap<>();
    for (FileWordsScannerProvider provider : fileWordsScannerProviders) {
      scannerMap.put(provider.getFileType(), provider.createWordsScanner());
    }
    return scannerMap;
  });

  @Nonnull
  FileType getFileType();

  @Nonnull
  WordsScanner createWordsScanner();
}
