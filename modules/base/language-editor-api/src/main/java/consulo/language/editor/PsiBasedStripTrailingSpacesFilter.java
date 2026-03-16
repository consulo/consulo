// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor;

import consulo.document.Document;
import consulo.document.StripTrailingSpacesFilter;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;

import java.util.BitSet;

public abstract class PsiBasedStripTrailingSpacesFilter implements StripTrailingSpacesFilter {
    
    private final BitSet myDisabledLinesBitSet;
    
    private final Document myDocument;

    protected PsiBasedStripTrailingSpacesFilter(Document document) {
        myDocument = document;
        myDisabledLinesBitSet = new BitSet(document.getLineCount());
    }

    @Override
    public boolean isStripSpacesAllowedForLine(int line) {
        return !myDisabledLinesBitSet.get(line);
    }

    protected abstract void process(PsiFile psiFile);

    protected final void disableRange(TextRange range, boolean includeEndLine) {
        int startLine = myDocument.getLineNumber(range.getStartOffset());
        int endLine = myDocument.getLineNumber(range.getEndOffset());
        if (includeEndLine) {
            endLine++;
        }
        myDisabledLinesBitSet.set(startLine, endLine);
    }
}
