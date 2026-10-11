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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Dmitry Batkovich
 */
public class SeverityAndOccurrences {
    private @Nullable HighlightSeverity myPrimarySeverity;
    private final Map<String, HighlightSeverity> myOccurrences = new HashMap<>();

    public SeverityAndOccurrences incOccurrences(String toolName, HighlightSeverity severity) {
        if (myPrimarySeverity == null) {
            myPrimarySeverity = severity;
        }
        else if (!Objects.equals(severity, myPrimarySeverity)) {
            myPrimarySeverity = InspectionSeverities.MIXED_FAKE_SEVERITY;
        }
        myOccurrences.put(toolName, severity);
        return this;
    }

    public @Nullable HighlightSeverity getPrimarySeverity() {
        return myPrimarySeverity;
    }

    public int getOccurrencesSize() {
        return myOccurrences.size();
    }

    public Map<String, HighlightSeverity> getOccurrences() {
        return myOccurrences;
    }
}
