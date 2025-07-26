/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.codeEditor.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.impl.internal.textEditor.TextComponentEditorImpl;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.awt.UIExAWTDataKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.text.JTextComponent;

/**
 * @author yole
 */
public abstract class TextComponentEditorAction extends EditorAction {
    protected TextComponentEditorAction(@Nonnull EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    @Override
    @Nullable
    protected Editor getEditor(@Nonnull DataContext dataContext) {
        return getEditorFromContext(dataContext);
    }

    @Nullable
    public static Editor getEditorFromContext(@Nonnull DataContext dataContext) {
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor != null) {
            return editor;
        }
        Object data = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        return data instanceof JTextComponent textComponent
            ? new TextComponentEditorImpl(dataContext.getData(Project.KEY), textComponent) : null;
    }
}