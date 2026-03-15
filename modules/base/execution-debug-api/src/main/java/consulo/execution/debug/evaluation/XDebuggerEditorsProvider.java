/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.evaluation;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class XDebuggerEditorsProvider {
    
    public abstract FileType getFileType();

    
    @RequiredReadAction
    public abstract Document createDocument(Project project,
                                            String text,
                                            @Nullable XSourcePosition sourcePosition,
                                            EvaluationMode mode);

    
    @RequiredReadAction
    public Document createDocument(Project project,
                                   XExpression expression,
                                   @Nullable XSourcePosition sourcePosition,
                                   EvaluationMode mode) {
        return createDocument(project, expression.getExpression(), sourcePosition, mode);
    }

    
    public Collection<Language> getSupportedLanguages(Project project, @Nullable XSourcePosition sourcePosition) {
        FileType type = getFileType();
        if (type instanceof LanguageFileType) {
            return Collections.singleton(((LanguageFileType) type).getLanguage());
        }
        return Collections.emptyList();
    }

    
    public XExpression createExpression(Project project, Document document, @Nullable Language language, EvaluationMode mode) {
        return XDebuggerUtil.getInstance().createExpression(document.getText(), language, null, mode);
    }

    
    public InlineDebuggerHelper getInlineDebuggerHelper() {
        return InlineDebuggerHelper.DEFAULT;
    }

    public boolean isEvaluateExpressionFieldEnabled() {
        return true;
    }
}