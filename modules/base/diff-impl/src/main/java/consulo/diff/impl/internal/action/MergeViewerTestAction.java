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
import consulo.codeEditor.EditorFactory;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestFactory;
import consulo.diff.InvalidDiffRequestException;
import consulo.diff.merge.MergeRequest;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "MergeViewerTestAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI")))
public class MergeViewerTestAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(MergeViewerTestAction.class);

    private final DiffManager myDiffManager;
    private final DiffRequestFactory myDiffRequestFactory;
    private final EditorFactory myEditorFactory;

    @Inject
    public MergeViewerTestAction(DiffManager diffManager, DiffRequestFactory diffRequestFactory, EditorFactory editorFactory) {
        super(LocalizeValue.localizeTODO("Merge Viewer Test"));
        myDiffManager = diffManager;
        myDiffRequestFactory = diffRequestFactory;
        myEditorFactory = editorFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        Document output = myEditorFactory.createDocument(DiffPaintingTestAction.baseText());

        try {
            MergeRequest request = myDiffRequestFactory.createMergeRequest(
                project,
                null,
                output,
                List.of(DiffPaintingTestAction.leftText(), DiffPaintingTestAction.baseText(), DiffPaintingTestAction.rightText()),
                "Merge Viewer Test",
                List.of("Left", "Result", "Right"),
                null
            );
            myDiffManager.showMerge(project, request);
        }
        catch (InvalidDiffRequestException ex) {
            LOG.error(ex);
        }
    }
}
