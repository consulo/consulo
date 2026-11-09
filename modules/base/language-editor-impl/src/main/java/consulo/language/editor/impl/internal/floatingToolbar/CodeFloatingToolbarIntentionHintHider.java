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
package consulo.language.editor.impl.internal.floatingToolbar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarListener;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

/**
 * The toolbar offers the intentions itself, so the hint of the editor goes away while it is shown.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
@TopicImpl(ComponentScope.PROJECT)
public class CodeFloatingToolbarIntentionHintHider implements CodeFloatingToolbarListener {
    private final Project myProject;

    @Inject
    public CodeFloatingToolbarIntentionHintHider(Project project) {
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    public void toolbarShown(Editor editor) {
        IntentionsUI.getInstance(myProject).hideHintOf(editor);
    }
}
