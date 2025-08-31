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
package consulo.language.codeStyle.arrangement;

import consulo.language.codeStyle.arrangement.group.ArrangementGroupingRule;
import consulo.language.codeStyle.arrangement.match.*;
import consulo.language.codeStyle.arrangement.std.*;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ArrangementSettingsSerializer} which knows how to handle {@link StdArrangementSettings built-in arrangement tokens}
 * and {@link Mixin can be used as a base for custom serializer implementation}.
 *
 * @author Denis Zhdanov
 * @since 7/18/12 10:37 AM
 */
public class DefaultArrangementSettingsSerializer implements ArrangementSettingsSerializer {
  private static final Logger LOG = Logger.getInstance(DefaultArrangementSettingsSerializer.class);

  @Nonnull
  @NonNls private static final String GROUPS_ELEMENT_NAME     = "groups";
  @Nonnull
  @NonNls private static final String GROUP_ELEMENT_NAME      = "group";
  @Nonnull
  @NonNls private static final String RULES_ELEMENT_NAME      = "rules";
  @Nonnull
  @NonNls private static final String TOKENS_ELEMENT_NAME     = "tokens";
  @Nonnull
  @NonNls private static final String TOKEN_ELEMENT_NAME      = "token";
  @Nonnull
  @NonNls private static final String TOKEN_ID                = "id";
  @Nonnull
  @NonNls private static final String TOKEN_NAME              = "name";
  @Nonnull
  @NonNls private static final String SECTION_ELEMENT_NAME    = "section";
  @Nonnull
  @NonNls private static final String SECTION_START_ATTRIBUTE = "start_comment";
  @Nonnull
  @NonNls private static final String SECTION_END_ATTRIBUTE   = "end_comment";
  @Nonnull
  @NonNls private static final String RULE_ELEMENT_NAME       = "rule";
  @Nonnull
  @NonNls private static final String TYPE_ELEMENT_NAME       = "type";
  @Nonnull
  @NonNls private static final String MATCHER_ELEMENT_NAME    = "match";
  @Nonnull
  @NonNls private static final String ORDER_TYPE_ELEMENT_NAME = "order";

  @Nonnull
  private final DefaultArrangementEntryMatcherSerializer myMatcherSerializer;
  @Nonnull
  private final Mixin                                    myMixin;
  @Nonnull
  private final ArrangementSettings myDefaultSettings;

  public DefaultArrangementSettingsSerializer(@Nonnull StdArrangementSettings defaultSettings) {
    this(Mixin.NULL, defaultSettings);
  }

  public DefaultArrangementSettingsSerializer(@Nonnull Mixin mixin, @Nonnull StdArrangementSettings defaultSettings) {
    myMixin = new MutableMixin(mixin);
    myMatcherSerializer = new DefaultArrangementEntryMatcherSerializer(myMixin);
    myDefaultSettings = defaultSettings;
  }

  @Override
  public void serialize(@Nonnull ArrangementSettings s, @Nonnull Element holder) {
    if (!(s instanceof StdArrangementSettings)) {
      return;
    }

    StdArrangementSettings settings = (StdArrangementSettings)s;
    if (settings instanceof ArrangementExtendableSettings && myDefaultSettings instanceof ArrangementExtendableSettings) {
      Set<StdArrangementRuleAliasToken> tokensDefinition = ((ArrangementExtendableSettings)settings).getRuleAliases();
      boolean isDefault = tokensDefinition.equals(((ArrangementExtendableSettings)myDefaultSettings).getRuleAliases());
      if (!isDefault) {
        Element tokensElement = new Element(TOKENS_ELEMENT_NAME);
        for (StdArrangementRuleAliasToken definition : tokensDefinition) {
          Element tokenElement = new Element(TOKEN_ELEMENT_NAME);
          tokenElement.setAttribute(TOKEN_ID, definition.getId());
          tokenElement.setAttribute(TOKEN_NAME, definition.getName());

          Element rulesElement = new Element(RULES_ELEMENT_NAME);
          for (StdArrangementMatchRule rule : definition.getDefinitionRules()) {
            rulesElement.addContent(serialize(rule));
          }
          tokenElement.addContent(rulesElement);
          tokensElement.addContent(tokenElement);
        }
        holder.addContent(tokensElement);
      }
    }

    List<ArrangementGroupingRule> groupings = settings.getGroupings();
    boolean isDefaultGroupings = groupings.equals(myDefaultSettings.getGroupings());
    if (!isDefaultGroupings) {
      Element groupingsElement = new Element(GROUPS_ELEMENT_NAME);
      holder.addContent(groupingsElement);
      for (ArrangementGroupingRule group : groupings) {
        Element groupElement = new Element(GROUP_ELEMENT_NAME);
        groupingsElement.addContent(groupElement);
        groupElement.addContent(new Element(TYPE_ELEMENT_NAME).setText(group.getGroupingType().getId()));
        groupElement.addContent(new Element(ORDER_TYPE_ELEMENT_NAME).setText(group.getOrderType().getId()));
      }
    }

    List<ArrangementSectionRule> sections = settings.getSections();
    boolean isDefaultRules = sections.equals((myDefaultSettings).getSections());
    if (!isDefaultRules) {
      Element rulesElement = new Element(RULES_ELEMENT_NAME);
      holder.addContent(rulesElement);
      for (ArrangementSectionRule section : sections) {
        rulesElement.addContent(serialize(section));
      }
    }
  }

  @Nullable
  @Override
  public ArrangementSettings deserialize(@Nonnull Element element) {
    Set<StdArrangementRuleAliasToken> tokensDefinition = deserializeTokensDefinition(element, myDefaultSettings);
    List<ArrangementGroupingRule> groupingRules = deserializeGropings(element, myDefaultSettings);
    Element rulesElement = element.getChild(RULES_ELEMENT_NAME);
    List<ArrangementSectionRule> sectionRules = ContainerUtil.newArrayList();
    if(rulesElement == null) {
      sectionRules.addAll(myDefaultSettings.getSections());
    }
    else {
      sectionRules.addAll(deserializeSectionRules(rulesElement, tokensDefinition));
      if (sectionRules.isEmpty()) {
        // for backward compatibility
        List<StdArrangementMatchRule> rules = deserializeRules(rulesElement, tokensDefinition);
        return StdArrangementSettings.createByMatchRules(groupingRules, rules);
      }
    }

    if (tokensDefinition == null) {
      return new StdArrangementSettings(groupingRules, sectionRules);
    }
    return new StdArrangementExtendableSettings(groupingRules, sectionRules, tokensDefinition);
  }

  @jakarta.annotation.Nullable
  private Set<StdArrangementRuleAliasToken> deserializeTokensDefinition(@Nonnull Element element, @Nonnull ArrangementSettings defaultSettings) {
    if (!(defaultSettings instanceof ArrangementExtendableSettings)) {
      return null;
    }

    Element tokensRoot = element.getChild(TOKENS_ELEMENT_NAME);
    if (tokensRoot == null) {
      return ((ArrangementExtendableSettings)myDefaultSettings).getRuleAliases();
    }

    Set<StdArrangementRuleAliasToken> tokenDefinitions = new HashSet<StdArrangementRuleAliasToken>();
    List<Element> tokens = tokensRoot.getChildren(TOKEN_ELEMENT_NAME);
    for (Element token : tokens) {
      Attribute id = token.getAttribute(TOKEN_ID);
      Attribute name = token.getAttribute(TOKEN_NAME);
      assert id != null && name != null : "Can not find id for token: " + token;
      Element rules = token.getChild(RULES_ELEMENT_NAME);
      List<StdArrangementMatchRule> tokenRules =
              rules == null ? List.of() : deserializeRules(rules, null);
      tokenDefinitions.add(new StdArrangementRuleAliasToken(id.getValue(), name.getValue(), tokenRules));
    }
    return tokenDefinitions;
  }

  @Nonnull
  private List<ArrangementGroupingRule> deserializeGropings(@Nonnull Element element, @Nullable ArrangementSettings defaultSettings) {
    Element groups = element.getChild(GROUPS_ELEMENT_NAME);
    if (groups == null) {
      return defaultSettings == null ? new ArrayList<>() : defaultSettings.getGroupings();
    }

    List<ArrangementGroupingRule> groupings = new ArrayList<ArrangementGroupingRule>();
    for (Object group : groups.getChildren(GROUP_ELEMENT_NAME)) {
      Element groupElement = (Element)group;

      // Grouping type.
      String groupingTypeId = groupElement.getChildText(TYPE_ELEMENT_NAME);
      ArrangementSettingsToken groupingType = StdArrangementTokens.byId(groupingTypeId);
      if (groupingType == null) {
        groupingType = myMixin.deserializeToken(groupingTypeId);
      }
      if (groupingType == null) {
        LOG.warn(String.format("Can't deserialize grouping type token by id '%s'", groupingTypeId));
        continue;
      }

      // Order type.
      String orderTypeId = groupElement.getChildText(ORDER_TYPE_ELEMENT_NAME);
      ArrangementSettingsToken orderType = StdArrangementTokens.byId(orderTypeId);
      if (orderType == null) {
        orderType = myMixin.deserializeToken(orderTypeId);
      }
      if (orderType == null) {
        LOG.warn(String.format("Can't deserialize grouping order type token by id '%s'", orderTypeId));
        continue;
      }
      groupings.add(new ArrangementGroupingRule(groupingType, orderType));
    }
    return groupings;
  }

  @Nonnull
  private List<ArrangementSectionRule> deserializeSectionRules(@Nonnull Element rulesElement,
                                                               @jakarta.annotation.Nullable Set<StdArrangementRuleAliasToken> tokens) {
    List<ArrangementSectionRule> sectionRules = new ArrayList<ArrangementSectionRule>();
    for (Object o : rulesElement.getChildren(SECTION_ELEMENT_NAME)) {
      Element sectionElement = (Element)o;
      List<StdArrangementMatchRule> rules = deserializeRules(sectionElement, tokens);
      Attribute start = sectionElement.getAttribute(SECTION_START_ATTRIBUTE);
      String startComment = start != null ? start.getValue().trim() : null;
      Attribute end = sectionElement.getAttribute(SECTION_END_ATTRIBUTE);
      String endComment = end != null ? end.getValue().trim() : null;
      sectionRules.add(ArrangementSectionRule.create(startComment, endComment, rules));
    }
    return sectionRules;
  }

  @Nonnull
  private List<StdArrangementMatchRule> deserializeRules(@Nonnull Element element, @Nullable Set<StdArrangementRuleAliasToken> aliases) {
    if (aliases != null && myMixin instanceof MutableMixin) {
      ((MutableMixin)myMixin).setMyRuleAliases(aliases);
    }
    List<StdArrangementMatchRule> rules = new ArrayList<StdArrangementMatchRule>();
    for (Object o : element.getChildren(RULE_ELEMENT_NAME)) {
      Element ruleElement = (Element)o;
      Element matcherElement = ruleElement.getChild(MATCHER_ELEMENT_NAME);
      if (matcherElement == null) {
        continue;
      }

      StdArrangementEntryMatcher matcher = null;
      for (Object c : matcherElement.getChildren()) {
        matcher = myMatcherSerializer.deserialize((Element)c);
        if (matcher != null) {
          break;
        }
      }

      if (matcher == null) {
        return new ArrayList<>();
      }

      Element orderTypeElement = ruleElement.getChild(ORDER_TYPE_ELEMENT_NAME);
      ArrangementSettingsToken orderType = null;
      if (orderTypeElement != null) {
        String orderTypeId = orderTypeElement.getText();
        orderType = StdArrangementTokens.byId(orderTypeId);
        if (orderType == null) {
          orderType = myMixin.deserializeToken(orderTypeId);
        }
        if (orderType == null) {
          LOG.warn(String.format("Can't deserialize matching rule order type for id '%s'. Falling back to default (%s)",
                                 orderTypeId, ArrangementMatchRule.DEFAULT_ORDER_TYPE.getId()));
        }
      }
      if (orderType == null) {
        orderType = ArrangementMatchRule.DEFAULT_ORDER_TYPE;
      }
      rules.add(new StdArrangementMatchRule(matcher, orderType));
    }
    return rules;
  }

  @Nullable
  public Element serialize(@Nonnull ArrangementMatchRule rule) {
    Element matcherElement = myMatcherSerializer.serialize(rule.getMatcher());
    if (matcherElement == null) {
      return null;
    }

    Element result = new Element(RULE_ELEMENT_NAME);
    result.addContent(new Element(MATCHER_ELEMENT_NAME).addContent(matcherElement));
    if (rule.getOrderType() != ArrangementMatchRule.DEFAULT_ORDER_TYPE) {
      result.addContent(new Element(ORDER_TYPE_ELEMENT_NAME).setText(rule.getOrderType().getId()));
    }
    return result;
  }

  @jakarta.annotation.Nullable
  public Element serialize(@Nonnull ArrangementSectionRule section) {
    Element sectionElement = new Element(SECTION_ELEMENT_NAME);
    if (StringUtil.isNotEmpty(section.getStartComment())) {
      // or only != null ?
      sectionElement.setAttribute(SECTION_START_ATTRIBUTE, section.getStartComment());
    }
    if (StringUtil.isNotEmpty(section.getEndComment())) {
      sectionElement.setAttribute(SECTION_END_ATTRIBUTE, section.getEndComment());
    }

    //TODO: serialize start & end comment as rule?
    List<StdArrangementMatchRule> rules = section.getMatchRules();
    for (int i = 0; i < rules.size(); i++) {
      StdArrangementMatchRule rule = rules.get(i);
      if ((i != 0 || StringUtil.isEmpty(section.getStartComment())) &&
          (i != rules.size() - 1 || StringUtil.isEmpty(section.getEndComment()))) {
        sectionElement.addContent(serialize(rule));
      }
    }
    return sectionElement;
  }

  public static class MutableMixin implements Mixin {
    private final Mixin myDelegate;
    private Set<StdArrangementRuleAliasToken> myRuleAliases;

    public MutableMixin(Mixin delegate) {
      myDelegate = delegate;
    }

    public void setMyRuleAliases(Set<StdArrangementRuleAliasToken> aliases) {
      myRuleAliases = aliases;
    }

    @Nullable
    @Override
    public ArrangementSettingsToken deserializeToken(@Nonnull String id) {
      ArrangementSettingsToken token = myDelegate.deserializeToken(id);
      if (token != null || myRuleAliases == null) {
        return token;
      }

      for (StdArrangementRuleAliasToken alias : myRuleAliases) {
        if (StringUtil.equals(alias.getId(), id)) {
          return alias;
        }
      }
      return null;
    }
  }

  public interface Mixin {

    Mixin NULL = new Mixin() {
      @Nullable
      @Override
      public ArrangementSettingsToken deserializeToken(@Nonnull String id) { return null; }
    };

    @Nullable
    ArrangementSettingsToken deserializeToken(@Nonnull String id);
  }
}
