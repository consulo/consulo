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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.EditorFactory;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.content.DocumentContent;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.patch.PatchReader;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.versionControlSystem.impl.internal.patch.apply.GenericPatchApplier;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "ApplyPatchMergeTestAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI")))
public class ApplyPatchMergeTestAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ApplyPatchMergeTestAction.class);

    private static final String LOCAL = """
        line 1
        line 2
        line 3
        alpha
        beta
        gamma
        line 7
        line 8
        delta
        epsilon
        line 11
        line 12
        """;

    private static final String PATCH = """
        --- a/sample.txt
        +++ b/sample.txt
        @@ -3,5 +3,5 @@
         line 3
         alpha
        -beta
        +BETA
         gamma
         line 7
        @@ -8,3 +8,3 @@
         line 8
        -Delta
        +delta
         epsilon
        @@ -20,3 +20,3 @@
         omega
        -zeta
        +ZETA
         omega2
        """;

    private final DiffManager myDiffManager;
    private final DiffContentFactory myDiffContentFactory;
    private final EditorFactory myEditorFactory;

    @Inject
    public ApplyPatchMergeTestAction(DiffManager diffManager, DiffContentFactory diffContentFactory, EditorFactory editorFactory) {
        super(LocalizeValue.localizeTODO("Apply Patch Merge Test"));
        myDiffManager = diffManager;
        myDiffContentFactory = diffContentFactory;
        myEditorFactory = editorFactory;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        List<TextFilePatch> patches;
        try {
            patches = new PatchReader(PATCH).readTextPatches();
        }
        catch (PatchSyntaxException ex) {
            LOG.error(ex);
            return;
        }

        GenericPatchApplier applier = new GenericPatchApplier(LOCAL, patches.get(0).getHunks());
        applier.execute();
        AppliedTextPatch appliedPatch = AppliedTextPatch.create(applier.getAppliedInfo());

        Document output = myEditorFactory.createDocument(LOCAL);
        DocumentContent resultContent = myDiffContentFactory.create(project, output);

        myDiffManager.showMerge(project, new ApplyPatchMergeRequest(
            project,
            resultContent,
            appliedPatch,
            LOCAL,
            "Apply Patch Merge Test",
            "Local",
            "Result",
            "Patch",
            null
        ));
    }
}
