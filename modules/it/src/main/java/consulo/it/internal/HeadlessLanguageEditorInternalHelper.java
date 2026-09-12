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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.project.Project;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Reparse-driven daemon listeners resolve this helper headlessly (for example
 * {@code DaemonListeners} updating editor highlighters on VFS renames); everything editor-visual
 * is a no-op here and the interface defaults already do nothing.
 *
 * @author VISTALL
 */
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
@Singleton
public class HeadlessLanguageEditorInternalHelper implements LanguageEditorInternalHelper {
    @Override
    public void doWrapLongLinesIfNecessary(Editor editor,
                                           Project project,
                                           Language language,
                                           Document document,
                                           int startOffset,
                                           int endOffset,
                                           List<? extends TextRange> enabledRanges) {
    }
}
