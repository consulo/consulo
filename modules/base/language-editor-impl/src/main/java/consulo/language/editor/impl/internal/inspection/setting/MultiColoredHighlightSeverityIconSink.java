/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class MultiColoredHighlightSeverityIconSink {
    private final Map<String, SeverityAndOccurrences> myScopeToAverageSeverityMap = new LinkedHashMap<>();

    private @Nullable String myDefaultScopeName;

    public @Nullable Image constructIcon(InspectionProfileImpl inspectionProfile) {
        Map<String, HighlightSeverity> computedSeverities = computeSeverities();
        if (computedSeverities == null || myDefaultScopeName == null) {
            return null;
        }

        boolean allScopesHasMixedSeverity = true;
        for (HighlightSeverity severity : computedSeverities.values()) {
            if (!severity.equals(InspectionSeverities.MIXED_FAKE_SEVERITY)) {
                allScopesHasMixedSeverity = false;
                break;
            }
        }
        return allScopesHasMixedSeverity
            ? InspectionSeverities.MIXED_FAKE_LEVEL.getIcon()
            : MultiScopeSeverityIcon.create(computedSeverities, myDefaultScopeName, inspectionProfile);
    }

    private @Nullable Map<String, HighlightSeverity> computeSeverities() {
        if (myScopeToAverageSeverityMap.isEmpty()) {
            return null;
        }
        Map<String, HighlightSeverity> result = new HashMap<>();
        Map.Entry<String, SeverityAndOccurrences> entry = myScopeToAverageSeverityMap.entrySet().iterator().next();
        HighlightSeverity firstSeverity = entry.getValue().getPrimarySeverity();
        if (firstSeverity != null) {
            result.put(entry.getKey(), firstSeverity);
        }
        if (myScopeToAverageSeverityMap.size() == 1) {
            return result;
        }

        SeverityAndOccurrences defaultSeveritiesAndOccurrences = myScopeToAverageSeverityMap.get(myDefaultScopeName);
        if (defaultSeveritiesAndOccurrences == null) {
            for (Map.Entry<String, SeverityAndOccurrences> e : myScopeToAverageSeverityMap.entrySet()) {
                HighlightSeverity primarySeverity = e.getValue().getPrimarySeverity();
                if (primarySeverity != null) {
                    result.put(e.getKey(), primarySeverity);
                }
            }
            return result;
        }
        int allInspectionsCount = defaultSeveritiesAndOccurrences.getOccurrencesSize();
        Map<String, HighlightSeverity> allScopes = defaultSeveritiesAndOccurrences.getOccurrences();
        for (Map.Entry<String, SeverityAndOccurrences> scopeEntry : myScopeToAverageSeverityMap.entrySet()) {
            String currentScope = scopeEntry.getKey();
            SeverityAndOccurrences currentSeverityAndOccurrences = scopeEntry.getValue();
            HighlightSeverity currentSeverity = currentSeverityAndOccurrences.getPrimarySeverity();
            if (currentSeverity == null) {
                continue;
            }
            if (currentSeverity == InspectionSeverities.MIXED_FAKE_SEVERITY
                || currentSeverityAndOccurrences.getOccurrencesSize() == allInspectionsCount
                || currentScope.equals(myDefaultScopeName)) {
                result.put(currentScope, currentSeverity);
                continue;
            }

            Set<String> toolsToCheck = new HashSet<>(allScopes.keySet());
            toolsToCheck.removeAll(currentSeverityAndOccurrences.getOccurrences().keySet());
            boolean mixed = false;
            for (String toolName : toolsToCheck) {
                HighlightSeverity currentToolSeverity = allScopes.get(toolName);
                if (currentToolSeverity != null && !currentSeverity.equals(currentToolSeverity)) {
                    mixed = true;
                    break;
                }
            }
            result.put(currentScope, mixed ? InspectionSeverities.MIXED_FAKE_SEVERITY : currentSeverity);
        }

        return result;
    }

    public void put(ScopeToolState defaultState, List<ScopeToolState> nonDefault) {
        putOne(defaultState);
        if (myDefaultScopeName == null) {
            myDefaultScopeName = defaultState.getScopeId();
        }
        for (ScopeToolState scopeToolState : nonDefault) {
            putOne(scopeToolState);
        }
    }

    private void putOne(ScopeToolState state) {
        if (!state.isEnabled()) {
            return;
        }
        String scopeName = state.getScopeId();
        String inspectionName = state.getTool().getShortName();
        myScopeToAverageSeverityMap.computeIfAbsent(scopeName, name -> new SeverityAndOccurrences())
            .incOccurrences(inspectionName, state.getLevel().getSeverity());
    }
}
