/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface LanguageEditorInternalHelper {
  @Nonnull
  static LanguageEditorInternalHelper getInstance() {
    return Application.get().getInstance(LanguageEditorInternalHelper.class);
  }

  void doWrapLongLinesIfNecessary(@Nonnull final Editor editor,
                                  @Nonnull final Project project,
                                  @Nonnull Language language,
                                  @Nonnull Document document,
                                  int startOffset,
                                  int endOffset,
                                  List<? extends TextRange> enabledRanges);

  @Contract("null,_,_->null;!null,_,_->!null")
  default Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, final int offset) {
    return editor;
  }
}
