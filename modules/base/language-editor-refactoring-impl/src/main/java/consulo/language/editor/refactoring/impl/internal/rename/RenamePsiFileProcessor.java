/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.impl.internal.rename;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.rename.RenamePsiFileProcessorBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl(order = "last")
public class RenamePsiFileProcessor extends RenamePsiFileProcessorBase {
    @Override
    public boolean canProcessElement(@Nonnull PsiElement element) {
        return element instanceof PsiFileSystemItem;
    }
}
