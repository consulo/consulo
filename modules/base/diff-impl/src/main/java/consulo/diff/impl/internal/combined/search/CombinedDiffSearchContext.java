// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined.search;

import consulo.codeEditor.Editor;

import java.util.List;

public class CombinedDiffSearchContext {
    private final List<EditorHolder> myHolders;

    public CombinedDiffSearchContext(List<EditorHolder> holders) {
        myHolders = holders;
    }

    public List<EditorHolder> getHolders() {
        return myHolders;
    }

    /**
     * Holder, which contains ordered editors
     *
     * @see consulo.diff.EditorDiffViewer
     */
    public static class EditorHolder {
        private final List<? extends Editor> myEditors;

        public EditorHolder(List<? extends Editor> editors) {
            myEditors = editors;
        }

        public List<? extends Editor> getEditors() {
            return myEditors;
        }
    }
}
