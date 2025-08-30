/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.versionControlSystem.log.VcsLogHighlighterFactory;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties.VcsLogHighlighterProperty;

@ActionImpl(id = "Vcs.Log.HighlightersActionGroup")
public class HighlightersActionGroup extends ActionGroup {
    private static class EnableHighlighterAction extends BooleanPropertyToggleAction {
        @Nonnull
        private final VcsLogHighlighterFactory myFactory;

        private EnableHighlighterAction(@Nonnull VcsLogHighlighterFactory factory) {
            super(factory.getTitle());
            myFactory = factory;
        }

        @Override
        protected VcsLogUiProperties.VcsLogUiProperty<Boolean> getProperty() {
            return VcsLogHighlighterProperty.get(myFactory.getId());
        }
    }

    public HighlightersActionGroup() {
        super(VersionControlSystemLogLocalize.groupHighlightText());
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();

        if (e != null && e.hasData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES)) {
            actions.add(AnSeparator.create(VersionControlSystemLogLocalize.groupHighlightSeparator()));
            e.getRequiredData(Project.KEY).getExtensionPoint(VcsLogHighlighterFactory.class).forEach(factory -> {
                if (factory.showMenuItem()) {
                    actions.add(new EnableHighlighterAction(factory));
                }
            });
        }

        return actions.toArray(new AnAction[actions.size()]);
    }
}
