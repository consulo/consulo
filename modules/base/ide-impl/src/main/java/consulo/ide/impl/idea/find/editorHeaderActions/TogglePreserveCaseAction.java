/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.find.FindBundle;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class TogglePreserveCaseAction extends EditorHeaderToggleAction implements Embeddable {
    public TogglePreserveCaseAction() {
        super(
            FindBundle.message("find.options.replace.preserve.case"),
            AllIcons.Actions.PreserveCase,
            AllIcons.Actions.PreserveCaseHover,
            AllIcons.Actions.PreserveCaseSelected
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
        FindModel findModel = search != null ? search.getFindModel() : null;
        e.getPresentation().setEnabled(findModel != null && !findModel.isRegularExpressions());

        super.update(e);
    }

    @Override
    protected boolean isSelected(@Nonnull SearchSession session) {
        return session.getFindModel().isPreserveCase();
    }

    @Override
    protected void setSelected(@Nonnull SearchSession session, boolean selected) {
        session.getFindModel().setPreserveCase(selected);
    }
}
