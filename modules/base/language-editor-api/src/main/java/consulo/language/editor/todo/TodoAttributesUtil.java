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
package consulo.language.editor.todo;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.language.psi.search.TodoAttributes;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;

public class TodoAttributesUtil {
    @Nonnull
    public static TodoAttributes createDefault() {
        return new TodoAttributes(PlatformIconGroup.generalTododefault(), getDefaultColorSchemeTextAttributes());
    }

    @Nonnull
    public static TextAttributes getDefaultColorSchemeTextAttributes() {
        return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.TODO_DEFAULT_ATTRIBUTES).clone();
    }

    @Nonnull
    public static TextAttributes getTextAttributes(@Nonnull TodoAttributes todoAttributes) {
        if (todoAttributes.shouldUseCustomTodoColor()) {
            return todoAttributes.getTextAttributes();
        }

        return getDefaultColorSchemeTextAttributes();
    }
}
