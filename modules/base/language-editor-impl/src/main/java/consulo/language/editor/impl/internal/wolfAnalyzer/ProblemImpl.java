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

package consulo.language.editor.impl.internal.wolfAnalyzer;

import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.wolfAnalyzer.Problem;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
public class ProblemImpl implements Problem {
  private final VirtualFile virtualFile;
  private final HighlightInfoImpl highlightInfo;
  private final boolean isSyntax;

  public ProblemImpl(@Nonnull VirtualFile virtualFile, @Nonnull HighlightInfoImpl highlightInfo, boolean isSyntax) {
    this.isSyntax = isSyntax;
    this.virtualFile = virtualFile;
    this.highlightInfo = highlightInfo;
  }

  @Override
  public VirtualFile getVirtualFile() {
    return virtualFile;
  }

  public boolean isSyntaxOnly() {
    return isSyntax;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProblemImpl problem = (ProblemImpl)o;

    if (isSyntax != problem.isSyntax) return false;
    if (!highlightInfo.equals(problem.highlightInfo)) return false;
    if (!virtualFile.equals(problem.virtualFile)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = virtualFile.hashCode();
    result = 31 * result + highlightInfo.hashCode();
    result = 31 * result + (isSyntax ? 1 : 0);
    return result;
  }

  @NonNls
  public String toString() {
    return "Problem: " + highlightInfo;
  }
}
