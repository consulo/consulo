// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;

public final class LargeFileEditorState implements FileEditorState {
    long caretPageNumber = 0;
    int caretSymbolOffsetInPage = 0;

    @Override
    public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
        if (otherState instanceof LargeFileEditorState state) {
            return caretPageNumber == state.caretPageNumber
                && caretSymbolOffsetInPage == state.caretSymbolOffsetInPage;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[p" + caretPageNumber + ",s" + caretSymbolOffsetInPage + "]";  // 'p' - Page number, 's' - Symbol offset in Page
    }
}
