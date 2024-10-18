/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.find.FindBundle;
import consulo.find.FindSettings;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.application.AllIcons;
import jakarta.annotation.Nonnull;

public class ToggleMatchCase extends EditorHeaderToggleAction implements Embeddable {
    public ToggleMatchCase() {
        super(
            FindBundle.message("find.case.sensitive"),
            AllIcons.Actions.MatchCase,
            AllIcons.Actions.MatchCaseHovered,
            AllIcons.Actions.MatchCaseSelected
        );
    }

    @Override
    protected boolean isSelected(@Nonnull SearchSession session) {
        return session.getFindModel().isCaseSensitive();
    }

    @Override
    protected void setSelected(@Nonnull SearchSession session, boolean selected) {
        session.getFindModel().setCaseSensitive(selected);
        FindSettings.getInstance().setLocalCaseSensitive(selected);
    }
}
