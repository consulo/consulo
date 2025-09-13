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
import consulo.localHistory.LocalHistory;
import consulo.localHistory.impl.internal.IdeaGateway;
import consulo.localHistory.localize.LocalHistoryLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.NonEmptyInputValidator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;

@ActionImpl(id = "LocalHistory.PutLabel")
public class PutLabelAction extends LocalHistoryActionWithDialog {
    public PutLabelAction() {
        super(LocalHistoryLocalize.actionPutLabelText());
    }

    @Override
    @RequiredUIAccess
    protected void showDialog(Project p, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
        String labelName = Messages.showInputDialog(
            p,
            LocalHistoryLocalize.putLabelName().get(),
            LocalHistoryLocalize.putLabelDialogTitle().get(),
            null,
            "",
            new NonEmptyInputValidator()
        );
        if (labelName == null) {
            return;
        }
        LocalHistory.getInstance().putUserLabel(p, LocalizeValue.of(labelName));
    }
}