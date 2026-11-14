// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.GutterMarkPreprocessor;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class SuggestedRefactoringGutterMarkPreprocessor implements GutterMarkPreprocessor {
    @Override
    public List<GutterMark> processMarkers(List<GutterMark> list) {
        GutterMark mark = null;
        for (GutterMark it : list) {
            if (it instanceof RefactoringAvailableGutterIconRenderer || it instanceof RefactoringDisabledGutterIconRenderer) {
                mark = it;
                break;
            }
        }
        if (mark == null) {
            return list;
        }
        // in order our gutter icon to be the most right, we must put it the first among ones with Alignment.RIGHT
        List<GutterMark> result = new ArrayList<>(list);
        result.remove(mark);
        result.add(0, mark);
        return result;
    }
}
