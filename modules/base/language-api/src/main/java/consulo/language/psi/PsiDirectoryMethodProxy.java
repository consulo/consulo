/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 0:08/11.11.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PsiDirectoryMethodProxy {
  ExtensionPointName<PsiDirectoryMethodProxy> EP_NAME = ExtensionPointName.create(PsiDirectoryMethodProxy.class);

  boolean checkAdd(@Nonnull PsiDirectory directory, @Nonnull PsiElement element) throws IncorrectOperationException;

  boolean checkCreateFile(@Nonnull PsiDirectory directory, @Nonnull String name) throws IncorrectOperationException;

  PsiElement add(@Nonnull PsiDirectory directory, @Nonnull final PsiElement element) throws IncorrectOperationException;
}
