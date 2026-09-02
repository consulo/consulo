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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.StubVariantFilter;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandConditions;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * Evaluates a sand declaration's guard against every context of its declaring file —
 * standalone module options plus each recorded inclusion environment. A variant guarded by
 * a flag no context provides is filtered out of the default index view.
 */
@ExtensionImpl
public class SandStubVariantFilter implements StubVariantFilter {
    private final Project myProject;

    @Inject
    public SandStubVariantFilter(Project project) {
        myProject = project;
    }

    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }

    @Override
    public boolean isActive(PsiElement variant) {
        if (!(variant instanceof SandClass sandClass)) {
            return true;
        }
        String condition = sandClass.getCondition();
        if (condition.isEmpty()) {
            return true;
        }

        PsiFile psiFile = sandClass.getContainingFile();
        VirtualFile file = psiFile == null ? null : psiFile.getVirtualFile();
        if (file == null) {
            return true;
        }

        for (Set<String> environment : SandFlagEnv.allContexts(myProject, file)) {
            if (SandConditions.matches(condition, environment)) {
                return true;
            }
        }
        return false;
    }
}
