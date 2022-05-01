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

/*
 * User: anna
 * Date: 25-Jan-2008
 */
package consulo.ide.impl.idea.codeEditor.printing;

import consulo.component.extension.ExtensionPointName;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.TreeMap;

public abstract class PrintOption {
  public static final ExtensionPointName<PrintOption> EP_NAME = ExtensionPointName.create("consulo.printOption");
  
  @Nullable
  public abstract TreeMap<Integer, PsiReference> collectReferences(PsiFile psiFile, Map<PsiFile, PsiFile> filesMap);

  @Nonnull
  public abstract UnnamedConfigurable createConfigurable();
}