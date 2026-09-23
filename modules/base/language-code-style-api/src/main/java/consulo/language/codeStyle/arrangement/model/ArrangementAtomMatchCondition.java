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
package consulo.language.codeStyle.arrangement.model;

import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates a single atom match condition, e.g. 'entry type is field' or 'entry has 'static' modifier' etc.
 * <p/>
 * Not thread-safe..
 *
 * @author Denis Zhdanov
 * @since 2012-08-08
 */
public class ArrangementAtomMatchCondition implements ArrangementMatchCondition {
    private final ArrangementSettingsToken myType;

    private final Object myValue;

    private boolean myInverted;

    public ArrangementAtomMatchCondition(ArrangementSettingsToken type) {
        this(type, type);
    }

    public ArrangementAtomMatchCondition(ArrangementSettingsToken type, Object value) {
        myType = type;
        myValue = value;
    }

    public ArrangementSettingsToken getType() {
        return myType;
    }

    public Object getValue() {
        return myValue;
    }

    @Override
    public void invite(ArrangementMatchConditionVisitor visitor) {
        visitor.visit(this);
    }

    public void setInverted(boolean inverted) {
        myInverted = inverted;
    }

    @Override
    public int hashCode() {
        int result = myType.hashCode();
        result = 31 * result + myValue.hashCode();
        result = 31 * result + (myInverted ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrangementAtomMatchCondition that = (ArrangementAtomMatchCondition) o;

        return myInverted == that.myInverted
            && myType.equals(that.myType)
            && myValue.equals(that.myValue);
    }

    @Override
    public ArrangementAtomMatchCondition clone() {
        ArrangementAtomMatchCondition result = new ArrangementAtomMatchCondition(myType, myValue);
        result.setInverted(myInverted);
        return result;
    }

    @Override
    public String toString() {
        if (myType.equals(myValue)) {
            return String.format("%s%s", myInverted ? "not " : "", myType.getRepresentationValue());
        }
        else {
            return String.format("%s: %s%s", myType.getRepresentationValue(), myInverted ? "not " : "", myValue.toString().toLowerCase());
        }
    }
}
