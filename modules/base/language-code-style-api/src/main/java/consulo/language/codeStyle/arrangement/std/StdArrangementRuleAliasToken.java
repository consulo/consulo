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

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class StdArrangementRuleAliasToken extends StdArrangementSettingsToken implements Cloneable {
  private String myName;

  /**
   * All usages of alias token will be replaced by this sequence of rules
   */
  private List<StdArrangementMatchRule> myDefinitionRules;

  public StdArrangementRuleAliasToken(@Nonnull String name) {
    this(name, List.of());
  }

  public StdArrangementRuleAliasToken(@Nonnull String name,
                                      @Nonnull List<StdArrangementMatchRule> definitionRules) {
    this(createIdByName(name), name, definitionRules);
    myDefinitionRules = definitionRules;
  }


  public StdArrangementRuleAliasToken(@Nonnull String id, @Nonnull String name,
                                      @Nonnull List<StdArrangementMatchRule> definitionRules) {
    super(id, createRepresentationValue(name), StdArrangementTokenType.ALIAS);
    myName = name;
    myDefinitionRules = definitionRules;
  }

  @Nonnull
  private static String createRepresentationValue(@Nonnull String name) {
    return "by " + name;
  }

  private static String createIdByName(@Nonnull String name) {
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

  public void setTokenName(@Nonnull String name) {
    myId = name.replaceAll("\\s+", "_");
    myRepresentationName = createRepresentationValue(name);
    myName = name;
  }

  @Override
  protected StdArrangementRuleAliasToken clone() {
    final List<StdArrangementMatchRule> newValue = new ArrayList<StdArrangementMatchRule>(myDefinitionRules.size());
    for (StdArrangementMatchRule rule : myDefinitionRules) {
      newValue.add(rule.clone());
    }
    return new StdArrangementRuleAliasToken(getName(), newValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StdArrangementRuleAliasToken token = (StdArrangementRuleAliasToken)o;

    if (!super.equals(o)) return false;
    if (myDefinitionRules != null ? !myDefinitionRules.equals(token.myDefinitionRules) : token.myDefinitionRules != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myDefinitionRules != null ? myDefinitionRules.hashCode() : 0);
    return result;
  }
}
