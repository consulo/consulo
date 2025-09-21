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

import consulo.fileEditor.impl.internal.search.SearchSession;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;

public class ToggleMatchCase extends EditorSearchToggleAction implements Embeddable {
    public ToggleMatchCase() {
        super(
            FindLocalize.findCaseSensitive(),
            PlatformIconGroup.actionsMatchcase(),
            PlatformIconGroup.actionsMatchcasehovered(),
            PlatformIconGroup.actionsMatchcaseselected()
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return false;
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
