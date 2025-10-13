/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.inspection.scheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.editor.internal.InspectionCacheService;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17-Feb-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface InspectionProjectProfileManager extends SeverityProvider, ProjectProfileManager {
    @Nonnull
    static InspectionProjectProfileManager getInstance(Project project) {
        return project.getInstance(InspectionProjectProfileManager.class);
    }

    @Nonnull
    default InspectionProfile getCurrentProfile() {
        return getInspectionProfile();
    }

    @Nonnull
    InspectionProfile getInspectionProfile();

    public static boolean isInformationLevel(String shortName, @Nonnull PsiElement element) {
        HighlightDisplayKey key = InspectionCacheService.getInstance().get().find(shortName);
        if (key != null) {
            HighlightDisplayLevel errorLevel = getInstance(element.getProject()).getCurrentProfile().getErrorLevel(key, element);
            return HighlightDisplayLevel.DO_NOT_SHOW.equals(errorLevel);
        }
        return false;
    }
}
