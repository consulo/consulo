/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.internal.inspection.DummyInspectionToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 04/03/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class InspectionTool implements BatchSuppressableTool {
    
    public InspectionToolState<?> createStateProvider() {
        return DummyInspectionToolState.INSTANCE;
    }

    public @Nullable Language getLanguage() {
        return null;
    }

    public @Nullable String getAlternativeID() {
        return null;
    }

    @Override
    public boolean isSuppressedFor(PsiElement element) {
        for (InspectionSuppressor suppressor : SuppressionUtil.getSuppressors(element)) {
            String toolId = getSuppressId();
            if (suppressor.isSuppressedFor(element, toolId)) {
                return true;
            }
            String alternativeId = getAlternativeID();
            if (alternativeId != null && !alternativeId.equals(toolId) && suppressor.isSuppressedFor(element, alternativeId)) {
                return true;
            }
        }
        return false;
    }

    protected String getSuppressId() {
        return getShortName();
    }

    
    @Override
    public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
        if (element != null) {
            SuppressQuickFix[] quickFixes = SuppressQuickFix.EMPTY_ARRAY;
            for (InspectionSuppressor suppressor : SuppressionUtil.getSuppressors(element)) {
                quickFixes = ArrayUtil.mergeArrays(quickFixes, suppressor.getSuppressActions(element, getShortName()));
            }
            return quickFixes;
        }
        return SuppressQuickFix.EMPTY_ARRAY;
    }

    public void cleanup(Project project) {
    }

    
    public abstract LocalizeValue getGroupDisplayName();

    
    public LocalizeValue[] getGroupPath() {
        LocalizeValue groupDisplayName = getGroupDisplayName().orIfEmpty(InspectionLocalize.inspectionGeneralToolsGroupName());

        Language language = getLanguage();
        if (language != null) {
            return new LocalizeValue[]{language.getDisplayName(), groupDisplayName};
        }
        else {
            return new LocalizeValue[]{groupDisplayName};
        }
    }

    
    public abstract LocalizeValue getDisplayName();

    
    public String getShortName() {
        return getShortName(getClass().getSimpleName());
    }

    
    public static String getShortName(Class<? extends InspectionTool> clazz) {
        return getShortName(clazz.getSimpleName());
    }

    
    public static String getShortName(String className) {
        return StringUtil.trimEnd(StringUtil.trimEnd(className, "Inspection"), "InspectionBase");
    }

    
    public abstract HighlightDisplayLevel getDefaultLevel();

    public boolean isEnabledByDefault() {
        return true;
    }

    /**
     * @return short name of tool whose results will be used
     */
    public @Nullable String getMainToolId() {
        return null;
    }

    
    public static LocalizeValue getGeneralGroupName() {
        return InspectionLocalize.inspectionGeneralToolsGroupName();
    }

    
    public LocalizeValue getDescription() {
        return LocalizeValue.empty();
    }

    /**
     * Override this method to return a html inspection description. Otherwise it will be loaded from resources using ID.
     *
     * @return hard-code inspection description.
     */
    @Deprecated
    public @Nullable String getStaticDescription() {
        return null;
    }

    @Deprecated
    public @Nullable String getDescriptionFileName() {
        return null;
    }

    @Deprecated
    protected @Nullable URL getDescriptionUrl() {
        String fileName = getDescriptionFileName();
        if (fileName == null) {
            return null;
        }
        return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
    }

    
    @Deprecated
    protected Class<? extends InspectionTool> getDescriptionContextClass() {
        return getClass();
    }

    @Deprecated
    public @Nullable String loadDescription() {
        String description = getStaticDescription();
        if (description != null) {
            return description;
        }

        try {
            URL descriptionUrl = getDescriptionUrl();
            if (descriptionUrl == null) {
                return null;
            }
            return ResourceUtil.loadText(descriptionUrl);
        }
        catch (IOException ignored) {
        }

        return null;
    }
}
