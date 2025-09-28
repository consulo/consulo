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

import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class InactiveEditorAction extends EditorAction {
    protected InactiveEditorAction(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    protected InactiveEditorAction(@Nonnull LocalizeValue text, EditorActionHandler defaultHandler) {
        super(text, defaultHandler);
    }

    @Nullable
    @Override
    protected Editor getEditor(@Nonnull DataContext dataContext) {
        return dataContext.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    }
}