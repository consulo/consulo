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
package consulo.ide.impl.language.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.psi.impl.source.codeStyle.CodeFormatterFacade;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.project.Project;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ServiceImpl
public class LanguageEditorInternalHelperImpl implements LanguageEditorInternalHelper {
  @Override
  public void doWrapLongLinesIfNecessary(@Nonnull Editor editor,
                                         @Nonnull Project project,
                                         @Nonnull Language language,
                                         @Nonnull Document document,
                                         int startOffset,
                                         int endOffset,
                                         List<? extends TextRange> enabledRanges) {
    final CodeFormatterFacade codeFormatter = new CodeFormatterFacade(CodeStyleSettingsManager.getSettings(project), language);

    codeFormatter.doWrapLongLinesIfNecessary(editor, project, document, startOffset, endOffset, enabledRanges);
  }
}
