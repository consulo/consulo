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

package consulo.language.impl.internal.dataRule;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class PsiFileRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink, @Nonnull DataSnapshot snapshot) {
        sink.lazyValue(PsiFile.KEY, dataProvider -> {
            PsiElement element = dataProvider.get(PsiElement.KEY);
            if (element != null) {
                return element.getContainingFile();
            }
            Project project = dataProvider.get(Project.KEY);
            if (project != null) {
                VirtualFile vFile = dataProvider.get(VirtualFile.KEY);
                if (vFile != null) {
                    return PsiManager.getInstance(project).findFile(vFile);
                }
            }
            return null;
        });
    }
}
