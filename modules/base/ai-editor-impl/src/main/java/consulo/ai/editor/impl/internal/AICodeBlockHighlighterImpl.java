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
package consulo.ai.editor.impl.internal;

import consulo.ai.AICodeBlockHighlighter;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.language.Language;
import consulo.language.editor.codeBlock.CodeBlockHighlighter;
import consulo.language.plain.PlainTextLanguage;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceImpl
@Singleton
public class AICodeBlockHighlighterImpl implements AICodeBlockHighlighter {
    private final CodeBlockHighlighter myHighlighter;

    @Inject
    public AICodeBlockHighlighterImpl(CodeBlockHighlighter highlighter) {
        myHighlighter = highlighter;
    }

    @RequiredReadAction
    @Override
    public String highlight(Project project, @Nullable String language, String code) {
        Language found = myHighlighter.guessLanguage(language);
        return myHighlighter.newHtmlBuilder(project, found == null ? PlainTextLanguage.INSTANCE : found, code)
            .generateWrappedTags()
            .generateFontFamily()
            .build();
    }
}
