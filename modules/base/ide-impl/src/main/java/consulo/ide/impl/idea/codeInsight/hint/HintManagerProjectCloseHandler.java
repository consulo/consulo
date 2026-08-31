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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.hint.HintManager;
import consulo.project.Project;
import consulo.project.ProjectCloseHandler;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.inject.Inject;

@ExtensionImpl
public class HintManagerProjectCloseHandler implements ProjectCloseHandler {
    private final Project myProject;
    private final HintManager myHintManager;

    @Inject
    public HintManagerProjectCloseHandler(Project project, HintManager hintManager) {
        myProject = project;
        myHintManager = hintManager;
    }

    @Override
    public Coroutine<?, ?> projectClosed() {
        return Coroutine.first(UIAction.<Object, Object>apply(input -> {
            ((HintManagerImpl) myHintManager).onProjectClosed(myProject);
            return input;
        }));
    }
}
