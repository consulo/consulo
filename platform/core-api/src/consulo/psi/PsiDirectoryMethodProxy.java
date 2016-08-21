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
package consulo.psi;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 0:08/11.11.13
 */
public interface PsiDirectoryMethodProxy {
  ExtensionPointName<PsiDirectoryMethodProxy> EP_NAME = ExtensionPointName.create("com.intellij.psi.directoryMethodProxy");

  boolean checkAdd(@NotNull PsiDirectory directory, @NotNull PsiElement element) throws IncorrectOperationException;

  boolean checkCreateFile(@NotNull PsiDirectory directory, @NotNull String name) throws IncorrectOperationException;

  PsiElement add(@NotNull PsiDirectory directory, @NotNull final PsiElement element) throws IncorrectOperationException;
}
