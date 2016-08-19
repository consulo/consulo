/*
 * Copyright 2013 must-be.org
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
package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 13:37/25.08.13
 */
public interface LanguageVersionWithDefinition<T extends Language> extends LanguageVersion<T> {
  boolean isMyElement(@Nullable PsiElement element);

  boolean isMyFile(@Nullable Project project, @Nullable VirtualFile virtualFile);
}
