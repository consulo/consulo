/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement.match;

import consulo.language.codeStyle.arrangement.ArrangementEntry;
import consulo.language.codeStyle.arrangement.TypeAwareArrangementEntry;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters {@link ArrangementEntry entries} by {@link TypeAwareArrangementEntry#getTypes() their types}.
 * <p/>
 * <b>Note:</b> type-unaware entry will not be matched by the current rule.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2012-07-17
 */
public class ByTypeArrangementEntryMatcher implements ArrangementEntryMatcher {
    private final Set<ArrangementAtomMatchCondition> myTypes = new HashSet<>();

    public ByTypeArrangementEntryMatcher(ArrangementAtomMatchCondition interestedType) {
        myTypes.add(interestedType);
    }

    public ByTypeArrangementEntryMatcher(Collection<ArrangementAtomMatchCondition> interestedTypes) {
        myTypes.addAll(interestedTypes);
    }

    @Override
    public boolean isMatched(ArrangementEntry entry) {
        if (entry instanceof TypeAwareArrangementEntry arrangementEntry) {
            Set<ArrangementSettingsToken> types = arrangementEntry.getTypes();
            for (ArrangementAtomMatchCondition condition : myTypes) {
                boolean isInverted = condition.getValue() instanceof Boolean bValue && !bValue;
                if (isInverted == types.contains(condition.getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Set<ArrangementAtomMatchCondition> getTypes() {
        return myTypes;
    }

    @Override
    public int hashCode() {
        return myTypes.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByTypeArrangementEntryMatcher that = (ByTypeArrangementEntryMatcher) o;
        return myTypes.equals(that.myTypes);
    }

    @Override
    public String toString() {
        return String.format("of type '%s'", myTypes);
    }
}
