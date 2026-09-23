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
package consulo.language.codeStyle.arrangement.std;

import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class StdArrangementRuleAliasToken extends StdArrangementSettingsToken implements Cloneable {
    private String myName;

    /**
     * All usages of alias token will be replaced by this sequence of rules
     */
    private List<StdArrangementMatchRule> myDefinitionRules;

    public StdArrangementRuleAliasToken(String name) {
        this(name, List.of());
    }

    public StdArrangementRuleAliasToken(String name, List<StdArrangementMatchRule> definitionRules) {
        this(createIdByName(name), name, definitionRules);
        myDefinitionRules = definitionRules;
    }

    public StdArrangementRuleAliasToken(String id, String name, List<StdArrangementMatchRule> definitionRules) {
        super(id, createRepresentationValue(name), StdArrangementTokenType.ALIAS);
        myName = name;
        myDefinitionRules = definitionRules;
    }

    private static String createRepresentationValue(String name) {
        return "by " + name;
    }

    private static String createIdByName(String name) {
        return name.replaceAll("\\s+", "_");
    }

    public String getName() {
        return myName;
    }

    public List<StdArrangementMatchRule> getDefinitionRules() {
        return myDefinitionRules;
    }

    public void setDefinitionRules(List<StdArrangementMatchRule> definitionRules) {
        myDefinitionRules = definitionRules;
    }

    public void setTokenName(String name) {
        myId = name.replaceAll("\\s+", "_");
        myRepresentationName = createRepresentationValue(name);
        myName = name;
    }

    @Override
    protected StdArrangementRuleAliasToken clone() {
        List<StdArrangementMatchRule> newValue = new ArrayList<>(myDefinitionRules.size());
        for (StdArrangementMatchRule rule : myDefinitionRules) {
            newValue.add(rule.clone());
        }
        return new StdArrangementRuleAliasToken(getName(), newValue);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StdArrangementRuleAliasToken that = (StdArrangementRuleAliasToken) o;

        return super.equals(o)
            && Objects.equals(myDefinitionRules, that.myDefinitionRules);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(myDefinitionRules);
    }
}
