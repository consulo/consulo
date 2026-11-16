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
package consulo.language.editor.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.document.Document;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import java.util.Collection;

/**
 * The implementation behind {@link consulo.language.editor.highlight.BackgroundUpdateHighlightersUtil}.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface BackgroundUpdateHighlightersInternal {
    static BackgroundUpdateHighlightersInternal getInstance() {
        return Application.get().getInstance(BackgroundUpdateHighlightersInternal.class);
    }

    @RequiredReadAction
    void setHighlightersToEditor(
        Project project,
        PsiFile psiFile,
        Document document,
        int startOffset,
        int endOffset,
        Collection<? extends HighlightInfo> highlights,
        int group
    );
}
