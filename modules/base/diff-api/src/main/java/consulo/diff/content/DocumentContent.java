/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.content;

import consulo.diff.util.LineCol;
import consulo.document.Document;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.Navigatable;
import consulo.platform.LineSeparator;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.Charset;

public interface DocumentContent extends DiffContent {
  /**
   * Represents this content as Document
   */
  @Nonnull
  Document getDocument();

  /**
   * This file could be used for better syntax highlighting.
   * Some file types can't be highlighted properly depending only on their FileType (ex: SQL dialects, PHP templates).
   */
  @Nullable
  default VirtualFile getHighlightFile() {
    return null;
  }

  /**
   * Provides a way to open given text place in editor
   */
  @Nullable
  default Navigatable getNavigatable(@Nonnull LineCol position) {
    return null;
  }

  /**
   * @return original file line separator
   */
  @Nullable
  default LineSeparator getLineSeparator() {
    return null;
  }

  /**
   * @return original file charset
   */
  @Nullable
  default Charset getCharset() {
    return null;
  }

  /**
   * @return original file byte order mark
   */
  @Nullable
  default Boolean hasBom() {
    return null;
  }

  @Nullable
  @Deprecated
  default OpenFileDescriptor getOpenFileDescriptor(int offset) {
    LineCol position = LineCol.fromOffset(getDocument(), offset);
    return ObjectUtil.tryCast(getNavigatable(position), OpenFileDescriptor.class);
  }
}
