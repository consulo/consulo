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

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 14, 2002
 * Time: 6:49:27 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
public class CodeBlockEndAction extends EditorAction {
    public CodeBlockEndAction() {
        super(new Handler());
        setInjectedContext(true);
    }

    private static class Handler extends EditorActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            if (project != null) {
                CodeBlockUtil.moveCaretToCodeBlockEnd(project, editor, false);
            }
        }
    }
}
