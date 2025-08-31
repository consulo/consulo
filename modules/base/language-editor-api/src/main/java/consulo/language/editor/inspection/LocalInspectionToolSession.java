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
package consulo.language.editor.inspection;

import consulo.util.dataholder.UserDataHolderBase;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2008-08-07
 */
public class LocalInspectionToolSession extends UserDataHolderBase {
  private final PsiFile myFile;
  private final int myStartOffset;
  private final int myEndOffset;

  public LocalInspectionToolSession(@Nonnull PsiFile file, int startOffset, int endOffset) {
    myFile = file;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }
}
