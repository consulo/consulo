/*
 * Copyright 2013-2023 consulo.io
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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewAcceptor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author VISTALL
 * @since 25/05/2023
 */
@ExtensionImpl
public class FileTypeExtensionPreviewAcceptor implements ExtensionPreviewAcceptor<FileTypeFactory> {
  private final FileNameMatcherFactory myFileNameMatcherFactory;

  @Inject
  public FileTypeExtensionPreviewAcceptor(FileNameMatcherFactory fileNameMatcherFactory) {
    myFileNameMatcherFactory = fileNameMatcherFactory;
  }

  @Override
  public boolean accept(ExtensionPreview pluginPreview, ExtensionPreview featurePreview) {
    FileNameMatcher matcher = createMatcher(pluginPreview.implId());
    return matcher != null && matcher.acceptsCharSequence(featurePreview.implId());
  }

  /**
   * for correct specification - see hub impl
   */
  @Nullable
  public FileNameMatcher createMatcher(@Nonnull String extensionValue) {
    if (extensionValue.length() < 2) {
      return null;
    }

    List<String> values = StringUtil.split(extensionValue, "|");

    String id = values.get(0);
    if (id.length() != 1) {
      return null;
    }


    FileNameMatcherFactory factory = myFileNameMatcherFactory;
    String value = values.get(1);

    char idChar = id.charAt(0);

    switch (idChar) {
      case '?':
        return factory.createWildcardFileNameMatcher(value);
      case '*':
        return factory.createExtensionFileNameMatcher(value);
      case '!':
        return factory.createExactFileNameMatcher(value, true);
      case '¡':
        return factory.createExactFileNameMatcher(value, false);
      default:
        return null;
    }
  }

  @Override
  public Class<FileTypeFactory> getApiClass() {
    return FileTypeFactory.class;
  }
}
