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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.search.SearchableOptionContributor;
import consulo.configurable.search.SearchableOptionProcessor;
import consulo.language.editor.impl.internal.inspection.setting.InspectionToolsConfigurable;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.InspectionCacheService;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.regex.Pattern;

/**
 * The inspections, found in settings search by their names and descriptions - on the Inspections page.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class InspectionSearchableOptionContributor extends SearchableOptionContributor {
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

    private final Provider<InspectionCacheService> myInspectionCacheService;

    @Inject
    public InspectionSearchableOptionContributor(Provider<InspectionCacheService> inspectionCacheService) {
        myInspectionCacheService = inspectionCacheService;
    }

    @Override
    public void processOptions(SearchableOptionProcessor processor) {
        LocalizeValue pageName = InspectionLocalize.inspectionSettingsIdeDisplayName();
        for (InspectionToolWrapper tool : myInspectionCacheService.get().get().wrapTools()) {
            LocalizeValue name = tool.getDisplayName();
            processor.addOption(name, name, tool.getShortName(), InspectionToolsConfigurable.ID, pageName);

            String description = tool.loadDescription();
            if (description != null) {
                processor.addOption(
                    LocalizeValue.of(HTML_PATTERN.matcher(description).replaceAll(" ")),
                    name,
                    tool.getShortName(),
                    InspectionToolsConfigurable.ID,
                    pageName
                );
            }
        }
    }
}
