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
package consulo.language.codeStyle.arrangement.group;

import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates information about grouping rules to use during arrangement.
 * <p/>
 * E.g. a rule might look like 'keep together class methods which implement methods from particular interface'.
 *
 * @author Denis Zhdanov
 * @since 2012-09-18
 */
public class ArrangementGroupingRule {
    private final ArrangementSettingsToken myGroupingType;

    private final ArrangementSettingsToken myOrderType;

    public ArrangementGroupingRule(ArrangementSettingsToken groupingType) {
        this(groupingType, StdArrangementTokens.Order.KEEP);
    }

    public ArrangementGroupingRule(ArrangementSettingsToken groupingType, ArrangementSettingsToken orderType) {
        myGroupingType = groupingType;
        myOrderType = orderType;
    }

    public ArrangementSettingsToken getGroupingType() {
        return myGroupingType;
    }

    public ArrangementSettingsToken getOrderType() {
        return myOrderType;
    }

    @Override
    public ArrangementGroupingRule clone() {
        return new ArrangementGroupingRule(myGroupingType, myOrderType);
    }

    @Override
    public int hashCode() {
        return 31 * myGroupingType.hashCode() + myOrderType.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrangementGroupingRule that = (ArrangementGroupingRule) o;

        return myOrderType == that.myOrderType
            && myGroupingType.equals(that.myGroupingType);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", myGroupingType, myOrderType);
    }
}
