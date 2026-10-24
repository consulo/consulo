// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.localize.VcsLocalize;

@ActionImpl(
    id = "CommitView.ShowOnDoubleClick",
    children = {
        @ActionRef(type = ShowOnDoubleClickEditorPreviewAction.class),
        @ActionRef(type = ShowOnDoubleClickSourceAction.class)
    }
)
public class ShowOnDoubleClickGroup extends DefaultActionGroup implements DumbAware {
    public ShowOnDoubleClickGroup() {
        super(VcsLocalize.changesActionShowOnDoubleClickText(), true);
    }
}
