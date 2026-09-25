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
import org.jspecify.annotations.Nullable;

/**
 * @author cdr
 */
public class ProblemImpl implements Problem {
    private final VirtualFile virtualFile;
    private final HighlightInfoImpl highlightInfo;
    private final boolean isSyntax;

    public ProblemImpl(VirtualFile virtualFile, HighlightInfoImpl highlightInfo, boolean isSyntax) {
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProblemImpl that = (ProblemImpl) o;

        return isSyntax == that.isSyntax
            && highlightInfo.equals(that.highlightInfo)
            && virtualFile.equals(that.virtualFile);
    }

    @Override
    public int hashCode() {
        int result = 31 * virtualFile.hashCode() + highlightInfo.hashCode();
        return 31 * result + Boolean.hashCode(isSyntax);
    }

    @Override
    public String toString() {
        return "Problem: " + highlightInfo;
    }
}
