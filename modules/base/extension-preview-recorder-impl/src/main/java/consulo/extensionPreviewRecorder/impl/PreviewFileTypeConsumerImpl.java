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
package consulo.extensionPreviewRecorder.impl;

import consulo.component.extension.preview.ExtensionPreview;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.internal.matcher.ExactFileNameMatcher;
import consulo.virtualFileSystem.internal.matcher.ExtensionFileNameMatcher;
import consulo.virtualFileSystem.internal.matcher.WildcardFileNameMatcher;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * This code based on hub analyzer code
 *
 * @author VISTALL
 * @since 22/01/2023
 */
public class PreviewFileTypeConsumerImpl implements FileTypeConsumer {
  private final FileTypeFactory myFileTypeFactory;
  private final Consumer<ExtensionPreview<FileTypeFactory>> myRecorder;

  public PreviewFileTypeConsumerImpl(FileTypeFactory fileTypeFactory, Consumer<ExtensionPreview<FileTypeFactory>> recorder) {
    myFileTypeFactory = fileTypeFactory;
    myRecorder = recorder;
  }

  @Override
  public void consume(@Nonnull FileType fileType) {
    consume(fileType, fileType.getDefaultExtension());
  }

  @Override
  public void consume(@Nonnull FileType fileType, String extensions) {
    if (extensions.isEmpty()) {
      return;
    }

    List<String> split = StringUtil.split(extensions, EXTENSION_DELIMITER);
    for (String ext : split) {
      consume(fileType, new ExtensionFileNameMatcher(ext.toLowerCase(Locale.US)));
    }
  }

  @Override
  public void consume(@Nonnull FileType fileType, FileNameMatcher... fileNameMatchers) {
    for (FileNameMatcher fileNameMatcher : fileNameMatchers) {
      record(fileNameMatcher);
    }
  }

  private void record(@Nonnull FileNameMatcher fileNameMatcher) {
    String id = buildMatcherIdentificator(fileNameMatcher);
    if (id == null) {
      return;
    }

    myRecorder.accept(new ExtensionPreview<>(FileTypeFactory.class, id, myFileTypeFactory));
  }

  @Nullable
  private String buildMatcherIdentificator(FileNameMatcher fileNameMatcher) {
    if (fileNameMatcher instanceof ExactFileNameMatcher) {
      if (((ExactFileNameMatcher)fileNameMatcher).isIgnoreCase()) {
        return "!|" + ((ExactFileNameMatcher)fileNameMatcher).getFileName();
      }
      else {
        return "¡|" + ((ExactFileNameMatcher)fileNameMatcher).getFileName();
      }
    }
    else if (fileNameMatcher instanceof ExtensionFileNameMatcher) {
      return "*|" + ((ExtensionFileNameMatcher)fileNameMatcher).getExtension();
    }
    else if (fileNameMatcher instanceof WildcardFileNameMatcher) {
      return "?|" + ((WildcardFileNameMatcher)fileNameMatcher).getPattern();
    }
    return null;
  }
}
