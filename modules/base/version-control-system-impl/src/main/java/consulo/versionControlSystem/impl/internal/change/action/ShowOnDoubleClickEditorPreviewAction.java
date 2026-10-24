// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.versionControlSystem.localize.VcsLocalize;

@ActionImpl(id = "CommitView.ShowOnDoubleClick.EditorPreview")
public class ShowOnDoubleClickEditorPreviewAction extends ShowOnDoubleClickToggleAction {
    public ShowOnDoubleClickEditorPreviewAction() {
        super(VcsLocalize.changesActionShowOnDoubleClickDiffText(), VcsLocalize.changesActionShowOnDoubleClickDiffDescription(), true);
    }
}
