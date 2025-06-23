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
package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nullable;

/**
 * Syntax highlight provider for per file. For highlight per language use {@link SyntaxHighlighterFactory}
 *
 * @author yole
 * @since 2007-11-06
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SyntaxHighlighterProvider {
  @Nullable
  SyntaxHighlighter create(FileType fileType, @Nullable Project project, @Nullable VirtualFile file);
}