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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighterProvider;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl(order = "first")
public class ScratchSyntaxHighlighterProvider implements SyntaxHighlighterProvider {
  @Override
  @Nullable
  public SyntaxHighlighter create(@Nonnull FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
    if (project == null || file == null) return null;
    if (!ScratchUtil.isScratch(file)) return null;

    Language language = LanguageUtil.getLanguageForPsi(project, file);
    return language == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
  }
}
