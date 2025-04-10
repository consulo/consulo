/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.inspection;

import consulo.content.scope.NamedScope;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class GlobalInspectionContextUtil {
    public static RefElement retrieveRefElement(@Nonnull PsiElement element, @Nonnull GlobalInspectionContext globalContext) {
        PsiFile elementFile = element.getContainingFile();
        RefElement refElement = globalContext.getRefManager().getReference(elementFile);
        if (refElement == null) {
            PsiElement context = InjectedLanguageManager.getInstance(elementFile.getProject()).getInjectionHost(elementFile);
            if (context != null) {
                refElement = globalContext.getRefManager().getReference(context.getContainingFile());
            }
        }
        return refElement;
    }

    public static boolean isToCheckMember(
        @Nonnull RefElement owner,
        @Nonnull InspectionTool tool,
        Tools tools,
        ProfileManager profileManager
    ) {
        return isToCheckFile(owner.getContainingFile(), tool, tools, profileManager) && !owner.isSuppressed(tool.getShortName());
    }

    public static boolean isToCheckFile(PsiFile file, @Nonnull InspectionTool tool, Tools tools, ProfileManager profileManager) {
        if (tools != null && file != null) {
            for (ScopeToolState state : tools.getTools()) {
                NamedScope namedScope = state.getScope(file.getProject());
                if (namedScope == null
                    || namedScope.getValue().contains(file.getVirtualFile(), file.getProject(), profileManager.getScopesManager())) {
                    if (state.isEnabled()) {
                        InspectionToolWrapper toolWrapper = state.getTool();
                        if (toolWrapper.getTool() == tool) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean canRunInspections(@Nonnull Project project, boolean online) {
        for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
            if (!factory.isProjectConfiguredToRunInspections(project, online)) {
                return false;
            }
        }
        return true;
    }
}
