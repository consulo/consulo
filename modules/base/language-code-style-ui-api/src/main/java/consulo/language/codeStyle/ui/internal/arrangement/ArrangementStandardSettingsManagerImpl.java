/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.ArrangementUtil;
import consulo.language.codeStyle.arrangement.match.ArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.std.*;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * Wraps {@link ArrangementStandardSettingsAware} for the common arrangement UI managing code.
 *
 * @author Denis Zhdanov
 * @since 3/7/13 3:11 PM
 */
public class ArrangementStandardSettingsManagerImpl implements ArrangementStandardSettingsManager {

  @Nonnull
  private final ObjectIntMap<ArrangementSettingsToken> myWidths = ObjectMaps.newObjectIntHashMap();
  @Nonnull
  private final ObjectIntMap<ArrangementSettingsToken> myWeights = ObjectMaps.newObjectIntHashMap();

  @Nonnull
  private final Comparator<ArrangementSettingsToken> myComparator = (t1, t2) -> {
    if (myWeights.containsKey(t1)) {
      if (myWeights.containsKey(t2)) {
        return myWeights.getInt(t1) - myWeights.getInt(t2);
      }
      else {
        return -1;
      }
    }
    else if (myWeights.containsKey(t2)) {
      return 1;
    }
    else {
      return t1.compareTo(t2);
    }
  };

  @Nonnull
  private final ArrangementStandardSettingsAware myDelegate;
  @Nonnull
  private final ArrangementColorsProvider myColorsProvider;
  @Nonnull
  private final Collection<Set<ArrangementSettingsToken>> myMutexes;

  @Nullable
  private final StdArrangementSettings myDefaultSettings;
  @Nullable
  private final List<CompositeArrangementSettingsToken> myGroupingTokens;
  @Nullable
  private final List<CompositeArrangementSettingsToken> myMatchingTokens;

  @Nonnull
  private final Collection<StdArrangementRuleAliasToken> myRuleAliases;
  @Nonnull
  private final Set<ArrangementSettingsToken> myRuleAliasMutex;
  @Nullable
  private CompositeArrangementSettingsToken myRuleAliasToken;

  public ArrangementStandardSettingsManagerImpl(@Nonnull ArrangementStandardSettingsAware delegate, @Nonnull ArrangementColorsProvider colorsProvider) {
    this(delegate, colorsProvider, List.of());
  }

  public ArrangementStandardSettingsManagerImpl(@Nonnull ArrangementStandardSettingsAware delegate,
                                                @Nonnull ArrangementColorsProvider colorsProvider,
                                                @Nonnull Collection<StdArrangementRuleAliasToken> aliases) {
    myDelegate = delegate;
    myColorsProvider = colorsProvider;
    myMutexes = delegate.getMutexes();
    myDefaultSettings = delegate.getDefaultSettings();

    SimpleColoredComponent renderer = new SimpleColoredComponent();
    myGroupingTokens = delegate.getSupportedGroupingTokens();
    if (myGroupingTokens != null) {
      parseWidths(myGroupingTokens, renderer);
      buildWeights(myGroupingTokens);
    }

    myMatchingTokens = delegate.getSupportedMatchingTokens();
    if (myMatchingTokens != null) {
      parseWidths(myMatchingTokens, renderer);
      buildWeights(myMatchingTokens);
    }

    Set<ArrangementSettingsToken> aliasTokens = new HashSet<>();
    aliasTokens.addAll(aliases);

    myRuleAliases = aliases;
    myRuleAliasMutex = aliasTokens;
    if (!myRuleAliases.isEmpty()) {
      myRuleAliasToken = new CompositeArrangementSettingsToken(StdArrangementTokens.General.ALIAS, aliasTokens);
    }
  }

  @Override
  @Nonnull
  public Collection<StdArrangementRuleAliasToken> getRuleAliases() {
    return myRuleAliases;
  }

  @Override
  @Nonnull
  public ArrangementStandardSettingsAware getDelegate() {
    return myDelegate;
  }

  private void parseWidths(@Nonnull Collection<CompositeArrangementSettingsToken> compositeTokens, @Nonnull SimpleColoredComponent renderer) {
    int width = 0;
    for (CompositeArrangementSettingsToken compositeToken : compositeTokens) {
      width = Math.max(width, parseWidth(compositeToken.getToken(), renderer));
    }
    for (CompositeArrangementSettingsToken compositeToken : compositeTokens) {
      myWidths.putInt(compositeToken.getToken(), width);
      parseWidths(compositeToken.getChildren(), renderer);
    }
  }

  private void buildWeights(@Nonnull Collection<CompositeArrangementSettingsToken> compositeTokens) {
    for (CompositeArrangementSettingsToken token : compositeTokens) {
      myWeights.putInt(token.getToken(), myWeights.size());
      buildWeights(token.getChildren());
    }
  }

  /**
   * @see ArrangementStandardSettingsAware#getDefaultSettings()
   */
  @Nullable
  public StdArrangementSettings getDefaultSettings() {
    return myDefaultSettings;
  }

  @Override
  public boolean isSectionRulesSupported() {
    return myDelegate instanceof ArrangementSectionRuleAwareSettings;
  }

  /**
   * @see ArrangementStandardSettingsAware#getSupportedGroupingTokens()
   */
  @Override
  @Nullable
  public List<CompositeArrangementSettingsToken> getSupportedGroupingTokens() {
    return myGroupingTokens;
  }

  /**
   * @see ArrangementStandardSettingsAware#getSupportedMatchingTokens()
   */
  @Override
  @Nullable
  public List<CompositeArrangementSettingsToken> getSupportedMatchingTokens() {
    if (myMatchingTokens == null || myRuleAliasToken == null) {
      return myMatchingTokens;
    }

    List<CompositeArrangementSettingsToken> allTokens = ContainerUtil.newArrayList(myMatchingTokens);
    allTokens.add(myRuleAliasToken);
    return allTokens;
  }

  @Override
  public boolean isEnabled(@Nonnull ArrangementSettingsToken token, @Nullable ArrangementMatchCondition current) {
    if (myRuleAliasMutex.contains(token)) {
      return true;
    }
    return myDelegate.isEnabled(token, current);
  }

  @Nonnull
  public ArrangementEntryMatcher buildMatcher(@Nonnull ArrangementMatchCondition condition) throws IllegalArgumentException {
    ArrangementEntryMatcher matcher = ArrangementUtil.buildMatcher(condition);
    if (matcher == null) {
      matcher = myDelegate.buildMatcher(condition);
    }
    return matcher;
  }

  @Override
  @Nonnull
  public Collection<Set<ArrangementSettingsToken>> getMutexes() {
    if (myRuleAliasMutex.isEmpty()) {
      return myMutexes;
    }
    List<Set<ArrangementSettingsToken>> allMutexes = ContainerUtil.newArrayList(myMutexes);
    allMutexes.add(myRuleAliasMutex);
    return allMutexes;
  }

  @Override
  public int getWidth(@Nonnull ArrangementSettingsToken token) {
    if (myWidths.containsKey(token)) {
      return myWidths.getInt(token);
    }
    return parseWidth(token, new SimpleColoredComponent());
  }

  private int parseWidth(@Nonnull ArrangementSettingsToken token, @Nonnull SimpleColoredComponent renderer) {
    renderer.clear();
    String value = getPresentationValue(token);
    renderer.append(value, TextAttributesUtil.fromTextAttributes(myColorsProvider.getTextAttributes(token, true)));
    int result = renderer.getPreferredSize().width;

    renderer.clear();
    renderer.append(value, TextAttributesUtil.fromTextAttributes(myColorsProvider.getTextAttributes(token, false)));
    return Math.max(result, renderer.getPreferredSize().width);
  }

  @Nonnull
  private static String getPresentationValue(@Nonnull ArrangementSettingsToken token) {
    if (token instanceof InvertibleArrangementSettingsToken) {
      return ((InvertibleArrangementSettingsToken)token).getInvertedRepresentationValue();
    }
    return token.getRepresentationValue();
  }

  @Override
  public List<ArrangementSettingsToken> sort(@Nonnull Collection<ArrangementSettingsToken> tokens) {
    List<ArrangementSettingsToken> result = new ArrayList<>(tokens);
    result.sort(myComparator);
    return result;
  }
}
