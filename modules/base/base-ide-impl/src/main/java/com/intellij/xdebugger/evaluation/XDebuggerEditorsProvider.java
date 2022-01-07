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
package com.intellij.xdebugger.evaluation;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class XDebuggerEditorsProvider {
  @Nonnull
  public abstract FileType getFileType();

  @Nonnull
  public abstract Document createDocument(@Nonnull Project project,
                                          @Nonnull String text,
                                          @javax.annotation.Nullable XSourcePosition sourcePosition,
                                          @Nonnull EvaluationMode mode);

  @Nonnull
  public Document createDocument(@Nonnull Project project,
                                 @Nonnull XExpression expression,
                                 @Nullable XSourcePosition sourcePosition,
                                 @Nonnull EvaluationMode mode) {
    return createDocument(project, expression.getExpression(), sourcePosition, mode);
  }

  @Nonnull
  public Collection<Language> getSupportedLanguages(@Nonnull Project project, @Nullable XSourcePosition sourcePosition) {
    FileType type = getFileType();
    if (type instanceof LanguageFileType) {
      return Collections.singleton(((LanguageFileType)type).getLanguage());
    }
    return Collections.emptyList();
  }

  @Nonnull
  public XExpression createExpression(@Nonnull Project project, @Nonnull Document document, @javax.annotation.Nullable Language language, @Nonnull EvaluationMode mode) {
    return XDebuggerUtil.getInstance().createExpression(document.getText(), language, null, mode);
  }

  @Nonnull
  public InlineDebuggerHelper getInlineDebuggerHelper() {
    return InlineDebuggerHelper.DEFAULT;
  }
}