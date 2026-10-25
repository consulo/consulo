// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff;

import consulo.codeEditor.Editor;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface EditorDiffViewer extends FrameDiffTool.DiffViewer {
    default @Nullable Editor getCurrentEditor() {
        List<? extends Editor> editors = getEditors();
        return editors.isEmpty() ? null : editors.get(0);
    }

    default List<? extends Editor> getHighlightEditors() {
        return getEditors();
    }

    List<? extends Editor> getEditors();
}
