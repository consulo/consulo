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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityUtil;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class InspectionSeverities {
    public static final HighlightSeverity MIXED_FAKE_SEVERITY = new HighlightSeverity("Mixed", -1);
    public static final HighlightDisplayLevel MIXED_FAKE_LEVEL =
        new HighlightDisplayLevel(MIXED_FAKE_SEVERITY, Image.empty(HighlightDisplayLevel.getEmptyIconDim()));

    private InspectionSeverities() {
    }

    public static SeverityRegistrarImpl getOwnRegistrar(InspectionProfileImpl profile) {
        return (SeverityRegistrarImpl) ((SeverityProvider) profile.getProfileManager()).getOwnSeverityRegistrar();
    }

    public static SortedSet<HighlightSeverity> getSeverities(SeverityRegistrarImpl severityRegistrar) {
        SortedSet<HighlightSeverity> severities = new TreeSet<>(severityRegistrar);
        for (SeverityRegistrarImpl.SeverityBasedTextAttributes type : SeverityUtil.getRegisteredHighlightingInfoTypes(severityRegistrar)) {
            severities.add(type.getSeverity());
        }
        severities.add(HighlightSeverity.ERROR);
        severities.add(HighlightSeverity.WARNING);
        severities.add(HighlightSeverity.WEAK_WARNING);
        severities.add(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING);
        return severities;
    }

    public static LocalizeValue renderSeverity(HighlightSeverity severity) {
        if (severity == MIXED_FAKE_SEVERITY) {
            return InspectionLocalize.inspectionSettingsSeverityMixed();
        }
        return LocalizeValue.of(StringUtil.capitalizeWords(severity.getName().toLowerCase(Locale.ROOT), true));
    }

    public static @Nullable Image getIcon(HighlightSeverity severity) {
        if (severity == MIXED_FAKE_SEVERITY) {
            return MIXED_FAKE_LEVEL.getIcon();
        }
        HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
        return level == null ? null : level.getIcon();
    }

    public static @Nullable HighlightSeverity getSeverity(List<ScopeToolState> scopeToolStates) {
        HighlightSeverity previousValue = null;
        for (ScopeToolState scopeToolState : scopeToolStates) {
            HighlightSeverity currentValue = scopeToolState.getLevel().getSeverity();
            if (previousValue == null) {
                previousValue = currentValue;
            }
            else if (!previousValue.equals(currentValue)) {
                return MIXED_FAKE_SEVERITY;
            }
        }
        return previousValue;
    }
}
