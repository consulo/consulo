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
package consulo.ide.impl.idea.codeInsight.completion.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.codeInsight.completion.CompletionPhase;
import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.project.Project;
import consulo.project.ProjectCloseHandler;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.inject.Inject;

@ExtensionImpl
public class CompletionServiceProjectCloseHandler implements ProjectCloseHandler {
    private final Project myProject;

    @Inject
    public CompletionServiceProjectCloseHandler(Project project) {
        myProject = project;
    }

    @Override
    public Coroutine<?, ?> beforeProjectClose() {
        return Coroutine.first(UIAction.<Object, Object>apply(input -> {
            CompletionProgressIndicator indicator = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
            if (indicator != null && indicator.getProject() == myProject) {
                indicator.closeAndFinish(true);
                CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            }
            else if (indicator == null) {
                CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);
            }
            return input;
        }));
    }
}
