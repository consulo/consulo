/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An informational object for debugging stub-mismatch related issues. Should be as small as possible since it's stored in files's attributes.
 *
 * @author peter
 */
public class IndexingStampInfo {
  final long indexingFileStamp;
  final long indexingByteLength;
  final int indexingCharLength;

  public IndexingStampInfo(long indexingFileStamp, long indexingByteLength, int indexingCharLength) {
    this.indexingFileStamp = indexingFileStamp;
    this.indexingByteLength = indexingByteLength;
    this.indexingCharLength = indexingCharLength;
  }

  @Override
  public String toString() {
    return "indexed at " + indexingFileStamp + " with document " + dumpSize(indexingByteLength, indexingCharLength);
  }

  @Nonnull
  public static String dumpSize(long byteLength, int charLength) {
    return " byte size = " + byteLength + ", char size = " + charLength;
  }

  public boolean isUpToDate(@Nullable Document document, @Nonnull VirtualFile file, @Nonnull PsiFile psi) {
    if (document == null || FileDocumentManager.getInstance().isDocumentUnsaved(document) || !PsiDocumentManager.getInstance(psi.getProject()).isCommitted(document)) {
      return false;
    }

    return indexingFileStamp == file.getTimeStamp() && contentLengthMatches(file.getLength(), document.getTextLength());
  }

  public boolean contentLengthMatches(long byteContentLength, int charContentLength) {
    if (this.indexingCharLength >= 0 && charContentLength >= 0) {
      return this.indexingCharLength == charContentLength;
    }
    return this.indexingByteLength == byteContentLength;
  }
}
