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
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokenType;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import consulo.util.collection.MultiValuesMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link ArrangementEntryMatcher} which is based on standard match conditions in form of {@link ArrangementSettingsToken}.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/26/12 11:07 PM
 */
public class StdArrangementEntryMatcher implements ArrangementEntryMatcher {

  @Nonnull
  private final ArrangementMatchCondition myCondition;
  @Nonnull
  private final ArrangementEntryMatcher   myDelegate;

  public StdArrangementEntryMatcher(@Nonnull ArrangementMatchCondition condition) {
    this(condition, new StdMatcherBuilderImpl());
  }

  public StdArrangementEntryMatcher(@Nonnull ArrangementMatchCondition condition, @Nonnull StdMatcherBuilder builder) {
    myCondition = condition;
    myDelegate = doBuildMatcher(condition, builder);
  }

  @Nonnull
  public ArrangementMatchCondition getCondition() {
    return myCondition;
  }

  @Override
  public boolean isMatched(@Nonnull ArrangementEntry entry) {
    return myDelegate.isMatched(entry);
  }

  @Override
  public int hashCode() {
    return myCondition.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StdArrangementEntryMatcher matcher = (StdArrangementEntryMatcher)o;
    return myCondition.equals(matcher.myCondition);
  }

  @Override
  public String toString() {
    return myCondition.toString();
  }

  @Nonnull
  private static ArrangementEntryMatcher doBuildMatcher(@Nonnull ArrangementMatchCondition condition, @Nonnull StdMatcherBuilder builder) {
    MyVisitor visitor = new MyVisitor(builder);
    condition.invite(visitor);
    return visitor.getMatcher();
  }

  /**
   * Used by inner visitor to build matchers from atom conditions.
   */
  public interface StdMatcherBuilder {

    /**
     * Parses given condition storing all data required to later produce a matcher based on the condition. It is called each time an
     * {@link ArrangementAtomMatchCondition} is encountered when traversing {@link ArrangementMatchCondition} on the
     * {@link StdArrangementEntryMatcher} creation.
     * @param condition condition to parse
     */
    void onCondition(@Nonnull ArrangementAtomMatchCondition condition);

    /**
     * Returns a collection of matchers obtained through {@link #addMatcher(ArrangementEntryMatcher) addMatcher} calls or
     * built from info gained by {@link #onCondition(ArrangementAtomMatchCondition) onCondition} calls.
     * @return a collection of matchers
     */
    @Nullable Collection<ArrangementEntryMatcher> buildMatchers();

    /**
     * Adds given matcher to collection provided by {@link #buildMatchers() buildMatchers} calls.
     * @param matcher matcher to be added
     */
    void addMatcher(@Nonnull ArrangementEntryMatcher matcher);
  }

  /**
   * Standard implementation of {@link StdMatcherBuilder}. Constructs entry matchers of types {@link ByTypeArrangementEntryMatcher},
   * {@link ByModifierArrangementEntryMatcher}, {@link ByNameArrangementEntryMatcher}, {@link ByNamespaceArrangementEntryMatcher}.
   */
  public static class StdMatcherBuilderImpl implements StdMatcherBuilder {

    @Nonnull
    private final List<ArrangementEntryMatcher> myMatchers = new ArrayList<>();
    /**
     * Maps token type to all arrangement tokens that were encountered so far by parsing conditions with
     * {@link #onCondition(ArrangementAtomMatchCondition) onCondition} calls.
     */
    @Nonnull
    protected final MultiValuesMap<StdArrangementTokenType, ArrangementAtomMatchCondition> context =
            new MultiValuesMap<StdArrangementTokenType, ArrangementAtomMatchCondition>();
    @Nullable
    private String myNamePattern;
    @Nullable private String myNamespacePattern;
    @Nullable private String myText;

    /**
     * Adds given entry to context by given entry type.
     * @param token token added to context
     */
    protected void addToContext(@Nonnull StdArrangementSettingsToken token, @Nonnull ArrangementAtomMatchCondition condition) {
      StdArrangementTokenType tokenType = token.getTokenType();
      context.put(tokenType, condition);
    }

    @Override
    public void onCondition(@Nonnull ArrangementAtomMatchCondition condition) {
      if (StdArrangementTokens.Regexp.NAME.equals(condition.getType())) {
        myNamePattern = condition.getValue().toString();
        return;
      }
      else if (StdArrangementTokens.Regexp.XML_NAMESPACE.equals(condition.getType())) {
        myNamespacePattern = condition.getValue().toString();
      }
      else if (StdArrangementTokens.Regexp.TEXT.equals(condition.getType())) {
        myText = condition.getValue().toString();
      }
      Object v = condition.getValue();
      ArrangementSettingsToken type = condition.getType();
      if (type instanceof StdArrangementSettingsToken) {
        //Process any StdArrangementSettingsToken. No need to change it when new types of tokens will be processed.
        addToContext((StdArrangementSettingsToken)type, condition);
      }
    }

    @Nullable
    @Override
    public Collection<ArrangementEntryMatcher> buildMatchers() {
      List<ArrangementEntryMatcher> result = new ArrayList<>(myMatchers);
      Collection<ArrangementAtomMatchCondition> entryTokens = context.get(StdArrangementTokenType.ENTRY_TYPE);
      if (entryTokens!= null) {
        result.add(new ByTypeArrangementEntryMatcher(entryTokens));
      }
      Collection<ArrangementAtomMatchCondition> modifierTokens = context.get(StdArrangementTokenType.MODIFIER);
      if (modifierTokens != null) {
        result.add(new ByModifierArrangementEntryMatcher(modifierTokens));
      }
      if (myNamePattern != null) {
        result.add(new ByNameArrangementEntryMatcher(myNamePattern));
      }
      if (myNamespacePattern != null) {
        result.add(new ByNamespaceArrangementEntryMatcher(myNamespacePattern));
      }
      if (myText != null) {
        result.add(new ByTextArrangementEntryMatcher(myText));
      }
      return result;
    }

    @Override
    public void addMatcher(@Nonnull ArrangementEntryMatcher matcher) {
      myMatchers.add(matcher);
    }
  }

  private static class MyVisitor implements ArrangementMatchConditionVisitor {

    @Nonnull
    private final StdMatcherBuilder myMatcherBuilder;
    private boolean nestedComposite;

    private MyVisitor(@Nonnull StdMatcherBuilder matcherBuilder) {
      myMatcherBuilder = matcherBuilder;
    }

    @Override
    public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
      myMatcherBuilder.onCondition(condition);
    }

    @Override
    public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
      if (!nestedComposite) {
        nestedComposite = true;
        for (ArrangementMatchCondition c : condition.getOperands()) {
          c.invite(this);
        }
      }
      else {
        myMatcherBuilder.addMatcher(doBuildMatcher(condition, myMatcherBuilder));
      }
    }

    @SuppressWarnings("ConstantConditions")
    @Nonnull
    public ArrangementEntryMatcher getMatcher() {
      Collection<ArrangementEntryMatcher> matchers = myMatcherBuilder.buildMatchers();

      if (matchers.size() == 1) {
        return matchers.iterator().next();
      } else {
        CompositeArrangementEntryMatcher result = new CompositeArrangementEntryMatcher();
        for (ArrangementEntryMatcher matcher: matchers) {
          result.addMatcher(matcher);
        }
        return result;
      }
    }
  }
}
