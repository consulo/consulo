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

package consulo.ide.impl.idea.history.integration.ui.actions;

import consulo.ide.impl.idea.history.integration.LocalHistoryBundle;import consulo.localHistory.LocalHistory;
import consulo.ide.impl.idea.history.integration.IdeaGateway;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.ui.NonEmptyInputValidator;
import consulo.virtualFileSystem.VirtualFile;

public class PutLabelAction extends LocalHistoryActionWithDialog {
    @Override
    protected void showDialog(Project p, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
        String labelName = Messages.showInputDialog(
            p,
            LocalHistoryBundle.message("put.label.name"),
            LocalHistoryBundle.message("put.label.dialog.title"),
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