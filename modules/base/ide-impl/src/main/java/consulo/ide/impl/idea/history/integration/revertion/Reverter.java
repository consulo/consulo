/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.history.integration.revertion;

import consulo.ide.impl.idea.history.core.LocalHistoryFacade;
import consulo.ide.impl.idea.history.core.changes.ChangeVisitor;
import consulo.ide.impl.idea.history.core.changes.StructuralChange;
import consulo.ide.impl.idea.history.core.revisions.Revision;
import consulo.ide.impl.idea.history.integration.IdeaGateway;
import consulo.ide.impl.idea.history.integration.LocalHistoryBundle;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.application.util.DateFormatUtil;

import java.io.IOException;
import java.util.*;

public abstract class Reverter {
    private final Project myProject;
    protected LocalHistoryFacade myVcs;
    protected IdeaGateway myGateway;

    protected Reverter(Project p, LocalHistoryFacade vcs, IdeaGateway gw) {
        myProject = p;
        myVcs = vcs;
        myGateway = gw;
    }

    public List<String> askUserForProceeding() throws IOException {
        return Collections.emptyList();
    }

    public List<String> checkCanRevert() throws IOException {
        if (!askForReadOnlyStatusClearing()) {
            return Collections.singletonList(LocalHistoryBundle.message("revert.error.files.are.read.only"));
        }
        return Collections.emptyList();
    }

    protected boolean askForReadOnlyStatusClearing() throws IOException {
        return myGateway.ensureFilesAreWritable(myProject, getFilesToClearROStatus());
    }

    protected List<VirtualFile> getFilesToClearROStatus() throws IOException {
        final Set<VirtualFile> files = new HashSet<>();

        myVcs.accept(selective(new ChangeVisitor() {
            @Override
            public void visit(StructuralChange c) throws StopVisitingException {
                files.addAll(myGateway.getAllFilesFrom(c.getPath()));
            }
        }));

        return new ArrayList<>(files);
    }

    protected ChangeVisitor selective(ChangeVisitor v) {
        return v;
    }

    @RequiredUIAccess
    public void revert() throws IOException {
        try {
            new WriteCommandAction(myProject, getCommandName()) {
                @Override
                protected void run(Result objectResult) throws Throwable {
                    myGateway.saveAllUnsavedDocuments();
                    doRevert();
                    myGateway.saveAllUnsavedDocuments();
                }
            }.execute();
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioe) {
                throw ioe;
            }
            throw e;
        }
    }

    public String getCommandName() {
        Revision to = getTargetRevision();
        String name = to.getChangeSetName();
        String date = DateFormatUtil.formatDateTime(to.getTimestamp());
        if (name != null) {
            return LocalHistoryBundle.message("system.label.revert.to.change.date", name, date);
        }
        else {
            return LocalHistoryBundle.message("system.label.revert.to.date", date);
        }
    }

    protected abstract Revision getTargetRevision();

    protected abstract void doRevert() throws IOException, FilesTooBigForDiffException;
}
