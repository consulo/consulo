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
package consulo.localHistory.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.localHistory.impl.internal.IdeaGateway;
import consulo.localHistory.impl.internal.LocalHistoryFacade;
import consulo.localHistory.impl.internal.ui.view.SelectionHistoryDialog;
import consulo.localHistory.localize.LocalHistoryLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.history.VcsSelection;
import consulo.versionControlSystem.history.VcsSelectionUtil;
import consulo.versionControlSystem.internal.VcsContextWrapper;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ActionImpl(id = "LocalHistory.ShowSelectionHistory")
public class ShowSelectionHistoryAction extends ShowHistoryAction {
    public ShowSelectionHistoryAction() {
        super(LocalHistoryLocalize.actionShowSelectionHistoryText());
    }

    @RequiredUIAccess
    @Override
    protected void showDialog(Project p, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
        VcsSelection sel = Objects.requireNonNull(getSelection(e, false));

        int from = sel.getSelectionStartLineNumber();
        int to = sel.getSelectionEndLineNumber();

        new SelectionHistoryDialog(p, gw, f, from, to).show();
    }

    @Override
    protected LocalizeValue getTextValue(AnActionEvent e) {
        VcsSelection sel = getSelection(e, true);
        return sel == null ? super.getTextValue(e) : sel.getActionName();
    }

    @Override
    public void update(AnActionEvent e) {
        if (!e.hasData(EditorKeys.EDITOR_SNAPSHOT)) {
            e.getPresentation().setVisible(false);
        }
        else {
            super.update(e);
        }
    }

    @Override
    protected boolean isEnabled(LocalHistoryFacade vcs, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
        return super.isEnabled(vcs, gw, f, e) && !f.isDirectory() && getSelection(e, true) != null;
    }

    private static @Nullable VcsSelection getSelection(AnActionEvent e, boolean forUpdate) {
        VcsContext c = VcsContextWrapper.createCachedInstanceOn(e, forUpdate ? EditorKeys.EDITOR_SNAPSHOT : Editor.KEY);
        return VcsSelectionUtil.getSelection(c);
    }
}
