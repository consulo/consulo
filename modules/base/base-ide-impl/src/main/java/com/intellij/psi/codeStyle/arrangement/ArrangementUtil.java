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
package com.intellij.psi.codeStyle.arrangement;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.arrangement.match.*;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import com.intellij.psi.codeStyle.arrangement.std.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.text.CharArrayUtil;
import consulo.logging.Logger;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.MODIFIER_AS_TYPE;

/**
 * @author Denis Zhdanov
 * @since 7/17/12 11:24 AM
 */
public class ArrangementUtil {
  private static final Logger LOG = Logger.getInstance(ArrangementUtil.class);

  private ArrangementUtil() {
  }

  //region Serialization

  @javax.annotation.Nullable
  public static ArrangementSettings readExternal(@Nonnull Element element, @Nonnull Language language) {
    ArrangementSettingsSerializer serializer = getSerializer(language);
    if (serializer == null) {
      LOG.error("Can't find serializer for language: " + language.getDisplayName() + "(" + language.getID() + ")");
      return null;
    }

    return serializer.deserialize(element);
  }

  public static void writeExternal(@Nonnull Element element, @Nonnull ArrangementSettings settings, @Nonnull Language language) {
    ArrangementSettingsSerializer serializer = getSerializer(language);
    if (serializer == null) {
      LOG.error("Can't find serializer for language: " + language.getDisplayName() + "(" + language.getID() + ")");
      return;
    }

    serializer.serialize(settings, element);
  }

  @Nullable
  private static ArrangementSettingsSerializer getSerializer(@Nonnull Language language) {
    Rearranger<?> rearranger = Rearranger.EXTENSION.forLanguage(language);
    return rearranger == null ? null : rearranger.getSerializer();
  }

  //endregion

  @Nonnull
  public static ArrangementMatchCondition combine(@Nonnull ArrangementMatchCondition... nodes) {
    final ArrangementCompositeMatchCondition result = new ArrangementCompositeMatchCondition();
    final ArrangementMatchConditionVisitor visitor = new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition node) {
        result.addOperand(node);
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition node) {
        for (ArrangementMatchCondition operand : node.getOperands()) {
          operand.invite(this);
        }
      }
    };
    for (ArrangementMatchCondition node : nodes) {
      node.invite(visitor);
    }
    return result.getOperands().size() == 1 ? result.getOperands().iterator().next() : result;
  }

  //region ArrangementEntry

  /**
   * Tries to build a text range on the given arguments basis. Expands to the line start/end if possible.
   * <p/>
   * This method is expected to be used in a situation when we want to arrange complete rows.
   * Example:
   * <pre>
   *   class Test {
   *        void test() {
   *        }
   *      int i;
   *   }
   * </pre>
   * Suppose, we want to locate fields before methods. We can move the exact field and method range then but indent will be broken,
   * i.e. we'll get the result below:
   * <pre>
   *   class Test {
   *        int i;
   *      void test() {
   *        }
   *   }
   * </pre>
   * We can expand field and method range to the whole lines and that would allow to achieve the desired result:
   * <pre>
   *   class Test {
   *      int i;
   *        void test() {
   *        }
   *   }
   * </pre>
   * However, this method is expected to just return given range if there are multiple distinct elements at the same line:
   * <pre>
   *   class Test {
   *     void test1(){} void test2() {} int i;
   *   }
   * </pre>
   *
   * @param initialRange  anchor range
   * @param document      target document against which the ranges are built
   * @return              expanded range if possible; <code>null</code> otherwise
   */
  @Nonnull
  public static TextRange expandToLineIfPossible(@Nonnull TextRange initialRange, @Nonnull Document document) {
    CharSequence text = document.getCharsSequence();
    String ws = " \t";

    int startLine = document.getLineNumber(initialRange.getStartOffset());
    int lineStartOffset = document.getLineStartOffset(startLine);
    int i = CharArrayUtil.shiftBackward(text, lineStartOffset + 1, initialRange.getStartOffset() - 1, ws);
    if (i != lineStartOffset) {
      return initialRange;
    }

    int endLine = document.getLineNumber(initialRange.getEndOffset());
    int lineEndOffset = document.getLineEndOffset(endLine);
    i = CharArrayUtil.shiftForward(text, initialRange.getEndOffset(), lineEndOffset, ws);

    return i == lineEndOffset ? TextRange.create(lineStartOffset, lineEndOffset) : initialRange;
  }
  //endregion

  @javax.annotation.Nullable
  public static ArrangementSettingsToken parseType(@Nonnull ArrangementMatchCondition condition) throws IllegalArgumentException {
    final Ref<ArrangementSettingsToken> result = new Ref<ArrangementSettingsToken>();
    condition.invite(new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
        ArrangementSettingsToken type = condition.getType();
        if (StdArrangementTokenType.ENTRY_TYPE.is(condition.getType()) || MODIFIER_AS_TYPE.contains(type)) {
          result.set(condition.getType());
        }
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
        for (ArrangementMatchCondition c : condition.getOperands()) {
          c.invite(this);
          if (result.get() != null) {
            return;
          }
        }
      }
    });

    return result.get();
  }

  public static <T> Set<T> flatten(@Nonnull Iterable<? extends Iterable<T>> data) {
    Set<T> result = ContainerUtilRt.newHashSet();
    for (Iterable<T> i : data) {
      for (T t : i) {
        result.add(t);
      }
    }
    return result;
  }

  @Nonnull
  public static Map<ArrangementSettingsToken, Object> extractTokens(@Nonnull ArrangementMatchCondition condition) {
    final Map<ArrangementSettingsToken, Object> result = ContainerUtilRt.newHashMap();
    condition.invite(new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
        ArrangementSettingsToken type = condition.getType();
        Object value = condition.getValue();
        result.put(condition.getType(), type.equals(value) ? null : value);

        if (type instanceof CompositeArrangementToken) {
          Set<ArrangementSettingsToken> tokens = ((CompositeArrangementToken)type).getAdditionalTokens();
          for (ArrangementSettingsToken token : tokens) {
            result.put(token, null);
          }
        }
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
        for (ArrangementMatchCondition operand : condition.getOperands()) {
          operand.invite(this);
        }
      }
    });
    return result;
  }

  @javax.annotation.Nullable
  public static ArrangementEntryMatcher buildMatcher(@Nonnull ArrangementMatchCondition condition) {
    final Ref<ArrangementEntryMatcher> result = new Ref<ArrangementEntryMatcher>();
    final Stack<CompositeArrangementEntryMatcher> composites = new Stack<CompositeArrangementEntryMatcher>();
    ArrangementMatchConditionVisitor visitor = new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
        ArrangementEntryMatcher matcher = buildMatcher(condition);
        if (matcher == null) {
          return;
        }
        if (composites.isEmpty()) {
          result.set(matcher);
        }
        else {
          composites.peek().addMatcher(matcher);
        }
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
        composites.push(new CompositeArrangementEntryMatcher());
        try {
          for (ArrangementMatchCondition operand : condition.getOperands()) {
            operand.invite(this);
          }
        }
        finally {
          CompositeArrangementEntryMatcher matcher = composites.pop();
          if (composites.isEmpty()) {
            result.set(matcher);
          }
        }
      }
    };
    condition.invite(visitor);
    return result.get();
  }

  @Nullable
  public static ArrangementEntryMatcher buildMatcher(@Nonnull ArrangementAtomMatchCondition condition) {
    if (StdArrangementTokenType.ENTRY_TYPE.is(condition.getType())) {
      return new ByTypeArrangementEntryMatcher(condition);
    }
    else if (StdArrangementTokenType.MODIFIER.is(condition.getType())) {
      return new ByModifierArrangementEntryMatcher(condition);
    }
    else if (StdArrangementTokens.Regexp.NAME.equals(condition.getType())) {
      return new ByNameArrangementEntryMatcher(condition.getValue().toString());
    }
    else if (StdArrangementTokens.Regexp.XML_NAMESPACE.equals(condition.getType())) {
      return new ByNamespaceArrangementEntryMatcher(condition.getValue().toString());
    }
    else {
      return null;
    }
  }

  @Nonnull
  public static ArrangementUiComponent buildUiComponent(@Nonnull StdArrangementTokenUiRole role,
                                                        @Nonnull List<ArrangementSettingsToken> tokens,
                                                        @Nonnull ArrangementColorsProvider colorsProvider,
                                                        @Nonnull ArrangementStandardSettingsManager settingsManager)
          throws IllegalArgumentException
  {
    for (ArrangementUiComponent.Factory factory : ArrangementUiComponent.Factory.EP_NAME.getExtensionList()) {
      ArrangementUiComponent result = factory.build(role, tokens, colorsProvider, settingsManager);
      if (result != null) {
        return result;
      }
    }
    throw new IllegalArgumentException("Unsupported UI token role " + role);
  }

  @Nonnull
  public static List<CompositeArrangementSettingsToken> flatten(@Nonnull CompositeArrangementSettingsToken base) {
    List<CompositeArrangementSettingsToken> result = new ArrayList<>();
    Queue<CompositeArrangementSettingsToken> toProcess = ContainerUtilRt.newLinkedList(base);
    while (!toProcess.isEmpty()) {
      CompositeArrangementSettingsToken token = toProcess.remove();
      result.add(token);
      toProcess.addAll(token.getChildren());
    }
    return result;
  }

  //region Arrangement Sections
  @Nonnull
  public static List<StdArrangementMatchRule> collectMatchRules(@Nonnull List<ArrangementSectionRule> sections) {
    final List<StdArrangementMatchRule> matchRules = ContainerUtil.newArrayList();
    for (ArrangementSectionRule section : sections) {
      matchRules.addAll(section.getMatchRules());
    }
    return matchRules;
  }
  //endregion

  //region Arrangement Custom Tokens
  public static List<ArrangementSectionRule> getExtendedSectionRules(@Nonnull ArrangementSettings settings) {
    return settings instanceof ArrangementExtendableSettings ?
           ((ArrangementExtendableSettings)settings).getExtendedSectionRules() : settings.getSections();
  }

  public static boolean isAliasedCondition(@Nonnull ArrangementAtomMatchCondition condition) {
    return StdArrangementTokenType.ALIAS.is(condition.getType());
  }
  //endregion
}
