/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighterProvider;
import consulo.language.file.LanguageFileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionImpl(order = "last")
public class LanguageFileTypeHighlighterProvider implements SyntaxHighlighterProvider {
  @Override
  @Nullable
  public SyntaxHighlighter create(FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
    if (fileType instanceof LanguageFileType) {
      return SyntaxHighlighterFactory.getSyntaxHighlighter(((LanguageFileType)fileType).getLanguage(), project, file);
    }
    return null;
  }
}