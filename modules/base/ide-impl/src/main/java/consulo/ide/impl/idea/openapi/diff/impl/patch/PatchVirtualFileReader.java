/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.patch;

import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;

public class PatchVirtualFileReader {
  private PatchVirtualFileReader() {
  }

  public static PatchReader create(final VirtualFile virtualFile) throws IOException {
    final byte[] patchContents = virtualFile.contentsToByteArray();
    final CharSequence patchText = LoadTextUtil.getTextByBinaryPresentation(patchContents, virtualFile);
    return new PatchReader(patchText);
  }
}
