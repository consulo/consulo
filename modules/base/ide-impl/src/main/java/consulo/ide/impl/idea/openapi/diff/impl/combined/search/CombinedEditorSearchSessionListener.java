// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.diff.impl.combined.search;

import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.EventListener;

public interface CombinedEditorSearchSessionListener extends EventListener {
    @RequiredUIAccess
    void onSearch(boolean forward, Editor editor);

    @RequiredUIAccess
    void statusTextChanged(int matches, int files);
}
