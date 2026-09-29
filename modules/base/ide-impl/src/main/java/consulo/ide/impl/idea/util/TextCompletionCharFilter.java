/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors.
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
package consulo.ide.impl.idea.util;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.ui.awt.TextCompletionProvider;
import consulo.language.editor.ui.awt.TextCompletionUtil;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "textCompletion")
public final class TextCompletionCharFilter extends CharFilter {
    @Override
    public @Nullable Result acceptChar(char c, int prefixLength, Lookup lookup) {
        if (!lookup.isCompletion()) {
            return null;
        }

        PsiFile file = lookup.getPsiFile();
        if (file == null) {
            return null;
        }

        TextCompletionProvider provider = TextCompletionUtil.getProvider(file);
        return provider != null ? provider.acceptChar(c) : null;
    }
}
