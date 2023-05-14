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
package consulo.language.codeStyle.arrangement.match;

import consulo.language.codeStyle.arrangement.ArrangementUtil;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.Section.END_SECTION;
import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.Section.START_SECTION;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementSectionRule implements Cloneable {
  @Nullable
  private final String myStartComment;
  @Nullable
  private final String myEndComment;
  private final List<StdArrangementMatchRule> myMatchRules;

  private ArrangementSectionRule(@Nullable String start, @Nullable String end, @Nonnull List<StdArrangementMatchRule> rules) {
    myStartComment = start;
    myEndComment = end;
    myMatchRules = rules;
  }

  public List<StdArrangementMatchRule> getMatchRules() {
    return myMatchRules;
  }

  public static ArrangementSectionRule create(@Nonnull StdArrangementMatchRule... rules) {
    return create(null, null, rules);
  }

  public static ArrangementSectionRule create(@Nullable String start, @Nullable String end, @Nonnull StdArrangementMatchRule... rules) {
    return create(start, end, ContainerUtil.newArrayList(rules));
  }

  public static ArrangementSectionRule create(@Nullable String start, @Nullable String end, @Nonnull List<StdArrangementMatchRule> rules) {
    final List<StdArrangementMatchRule> matchRules = ContainerUtil.newArrayList();
    if (StringUtil.isNotEmpty(start)) {
      matchRules.add(createSectionRule(start, START_SECTION));
    }
    matchRules.addAll(rules);
    if (StringUtil.isNotEmpty(end)) {
      matchRules.add(createSectionRule(end, END_SECTION));
    }
    return new ArrangementSectionRule(start, end, matchRules);
  }

  @Nullable
  private static StdArrangementMatchRule createSectionRule(@Nullable String comment, @Nonnull ArrangementSettingsToken token) {
    if (StringUtil.isEmpty(comment)) {
      return null;
    }
    final ArrangementAtomMatchCondition type = new ArrangementAtomMatchCondition(token);
    final ArrangementAtomMatchCondition text = new ArrangementAtomMatchCondition(StdArrangementTokens.Regexp.TEXT, comment);
    final ArrangementMatchCondition condition = ArrangementUtil.combine(type, text);
    return new StdArrangementMatchRule(new StdArrangementEntryMatcher(condition));
  }

  @Nullable
  public String getStartComment() {
    return myStartComment;
  }

  @Nullable
  public String getEndComment() {
    return myEndComment;
  }

  @Override
  public int hashCode() {
    int factor = 31;
    int hash = StringUtil.notNullize(myStartComment).hashCode() + factor * StringUtil.notNullize(myEndComment).hashCode();
    for (StdArrangementMatchRule rule : myMatchRules) {
      factor *= factor;
      hash += rule.hashCode() * factor;
    }
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ArrangementSectionRule section = (ArrangementSectionRule)o;
    if (!StringUtil.equals(myStartComment, section.myStartComment) || !StringUtil.equals(myEndComment, section.myEndComment) || myMatchRules.size() != section.getMatchRules().size()) {
      return false;
    }

    final List<StdArrangementMatchRule> matchRules = section.getMatchRules();
    for (int i = 0; i < myMatchRules.size(); i++) {
      final StdArrangementMatchRule rule1 = myMatchRules.get(i);
      final StdArrangementMatchRule rule2 = matchRules.get(i);
      if (!rule1.equals(rule2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    if (StringUtil.isEmpty(myStartComment)) {
      return "Section: [" + StringUtil.join(myMatchRules, ",") + "]";
    }
    return "Section " + "(" + myStartComment + (StringUtil.isEmpty(myEndComment) ? "" : ", " + myEndComment) + ")";
  }

  @Override
  public ArrangementSectionRule clone() {
    final List<StdArrangementMatchRule> rules = ContainerUtil.newArrayList();
    for (StdArrangementMatchRule myMatchRule : myMatchRules) {
      rules.add(myMatchRule.clone());
    }
    return new ArrangementSectionRule(myStartComment, myEndComment, rules);
  }
}
