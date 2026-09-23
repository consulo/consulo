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
import consulo.language.codeStyle.arrangement.ModifierAwareArrangementEntry;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 2012-08-26
 */
public class ByModifierArrangementEntryMatcher implements ArrangementEntryMatcher {
    private final Set<ArrangementAtomMatchCondition> myModifiers = new HashSet<>();

    public ByModifierArrangementEntryMatcher(ArrangementAtomMatchCondition interestedModifier) {
        myModifiers.add(interestedModifier);
    }

    public ByModifierArrangementEntryMatcher(Collection<ArrangementAtomMatchCondition> interestedModifiers) {
        myModifiers.addAll(interestedModifiers);
    }

    @Override
    public boolean isMatched(ArrangementEntry entry) {
        if (entry instanceof ModifierAwareArrangementEntry modifierEntry) {
            Set<ArrangementSettingsToken> modifiers = modifierEntry.getModifiers();
            for (ArrangementAtomMatchCondition condition : myModifiers) {
                boolean isInverted = condition.getValue() instanceof Boolean bValue && !bValue;
                if (isInverted == modifiers.contains(condition.getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myModifiers.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByModifierArrangementEntryMatcher that = (ByModifierArrangementEntryMatcher) o;

        return myModifiers.equals(that.myModifiers);
    }

    @Override
    public String toString() {
        return "with modifiers " + myModifiers;
    }
}
