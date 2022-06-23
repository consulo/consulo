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
package consulo.language.file;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Extension for overriding viewProviderFactory for non language files
 *
 * @author VISTALL
 * @since 23-Jun-22
 */
@Extension(ComponentScope.APPLICATION)
public interface VirtualFileViewProviderFactory extends FileViewProviderFactory {
  ExtensionPointCacheKey<VirtualFileViewProviderFactory, Map<FileType, VirtualFileViewProviderFactory>> KEY =
          ExtensionPointCacheKey.groupBy("VirtualFileViewProviderFactory", VirtualFileViewProviderFactory::getFileType);

  @Nullable
  static VirtualFileViewProviderFactory forFileType(FileType fileType) {
    ExtensionPoint<VirtualFileViewProviderFactory> extensionPoint = Application.get().getExtensionPoint(VirtualFileViewProviderFactory.class);
    Map<FileType, VirtualFileViewProviderFactory> map = extensionPoint.getOrBuildCache(KEY);
    return map.get(fileType);
  }

  @Nonnull
  FileType getFileType();
}
