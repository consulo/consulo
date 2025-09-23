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
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @since 2002-05-14
 */
@ActionImpl(id = "EditorCodeBlockStart")
public class CodeBlockStartAction extends EditorAction {
    private static class MyHandler extends EditorActionHandler {
        public MyHandler() {
            super(true);
        }

        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            if (project != null) {
                CodeBlockUtil.moveCaretToCodeBlockStart(project, editor, false);
            }
        }
    }

    public CodeBlockStartAction() {
        super(ActionLocalize.actionEditorcodeblockstartText(), new MyHandler());
        setInjectedContext(true);
    }
}
