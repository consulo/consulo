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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 04/03/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class InspectionTool implements BatchSuppressableTool {
    @Nonnull
    public InspectionToolState<?> createStateProvider() {
        return DummyInspectionToolState.INSTANCE;
    }

    @Nullable
    public Language getLanguage() {
        return null;
    }

    @Nullable
    public String getAlternativeID() {
        return null;
    }

    @Override
    public boolean isSuppressedFor(@Nonnull PsiElement element) {
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

    @Nonnull
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

    @Nonnull
    public abstract LocalizeValue getGroupDisplayName();

    @Nonnull
    public LocalizeValue[] getGroupPath() {
        LocalizeValue groupDisplayName = getGroupDisplayName();
        if (groupDisplayName == LocalizeValue.of()) {
            groupDisplayName = InspectionLocalize.inspectionGeneralToolsGroupName();
        }

        Language language = getLanguage();
        if (language != null) {
            return new LocalizeValue[]{language.getDisplayName(), groupDisplayName};
        }
        else {
            return new LocalizeValue[]{groupDisplayName};
        }
    }

    @Nonnull
    public abstract LocalizeValue getDisplayName();

    @Nonnull
    public String getShortName() {
        return getShortName(getClass().getSimpleName());
    }

    @Nonnull
    public static String getShortName(@Nonnull Class<? extends InspectionTool> clazz) {
        return getShortName(clazz.getSimpleName());
    }

    @Nonnull
    public static String getShortName(@Nonnull String className) {
        return StringUtil.trimEnd(StringUtil.trimEnd(className, "Inspection"), "InspectionBase");
    }

    @Nonnull
    public abstract HighlightDisplayLevel getDefaultLevel();

    public boolean isEnabledByDefault() {
        return true;
    }

    /**
     * @return short name of tool whose results will be used
     */
    @Nullable
    public String getMainToolId() {
        return null;
    }

    @Nonnull
    public static LocalizeValue getGeneralGroupName() {
        return InspectionLocalize.inspectionGeneralToolsGroupName();
    }

    @Nonnull
    public LocalizeValue getDescription() {
        return LocalizeValue.of();
    }

    /**
     * Override this method to return a html inspection description. Otherwise it will be loaded from resources using ID.
     *
     * @return hard-code inspection description.
     */
    @Nullable
    @Deprecated
    public String getStaticDescription() {
        return null;
    }

    @Deprecated
    @Nullable
    public String getDescriptionFileName() {
        return null;
    }

    @Nullable
    @Deprecated
    protected URL getDescriptionUrl() {
        String fileName = getDescriptionFileName();
        if (fileName == null) {
            return null;
        }
        return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
    }

    @Nonnull
    @Deprecated
    protected Class<? extends InspectionTool> getDescriptionContextClass() {
        return getClass();
    }

    @Nullable
    @Deprecated
    public String loadDescription() {
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
