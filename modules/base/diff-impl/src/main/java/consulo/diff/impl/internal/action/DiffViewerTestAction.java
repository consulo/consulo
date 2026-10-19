/*
 * Copyright 2013-2026 consulo.io
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
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.request.SimpleDiffRequest;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "DiffViewerTestAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI")))
public class DiffViewerTestAction extends AnAction implements DumbAware {
    private final DiffManager myDiffManager;
    private final DiffContentFactory myDiffContentFactory;

    @Inject
    public DiffViewerTestAction(DiffManager diffManager, DiffContentFactory diffContentFactory) {
        super(LocalizeValue.localizeTODO("Diff Viewer Test"));
        myDiffManager = diffManager;
        myDiffContentFactory = diffContentFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        DiffContent left = myDiffContentFactory.create(project, DiffPaintingTestAction.leftText());
        DiffContent right = myDiffContentFactory.create(project, DiffPaintingTestAction.rightText());

        myDiffManager.showDiff(project, new SimpleDiffRequest("Diff Viewer Test", left, right, "Left", "Right"));
    }
}
