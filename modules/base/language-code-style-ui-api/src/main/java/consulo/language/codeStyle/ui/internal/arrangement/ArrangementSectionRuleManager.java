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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.ArrangementUtil;
import consulo.language.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.language.codeStyle.arrangement.std.CompositeArrangementSettingsToken;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.General.TYPE;
import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.Regexp.TEXT;
import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.Section.END_SECTION;
import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.Section.START_SECTION;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementSectionRuleManager {
  private static final Set<ArrangementSettingsToken> MUTEXES = Set.of(START_SECTION, END_SECTION);
  private static final Set<ArrangementSettingsToken> TOKENS = Set.of(START_SECTION, END_SECTION, TEXT);

  private Commenter myCommenter;

  private ArrangementMatchingRulesControl myControl;
  private ArrangementMatchingRuleEditor myEditor;

  @Nullable
  public static ArrangementSectionRuleManager getInstance(@Nonnull Language language,
                                                          @Nonnull ArrangementStandardSettingsManager settingsManager,
                                                          @Nonnull ArrangementColorsProvider colorsProvider,
                                                          @Nonnull ArrangementMatchingRulesControl control) {
    if (settingsManager.isSectionRulesSupported()) {
      return new ArrangementSectionRuleManager(language, settingsManager, colorsProvider, control);
    }
    return null;
  }

  private ArrangementSectionRuleManager(@Nonnull Language language,
                                        @Nonnull ArrangementStandardSettingsManager settingsManager,
                                        @Nonnull ArrangementColorsProvider colorsProvider,
                                        @Nonnull ArrangementMatchingRulesControl control) {
    myCommenter = Commenter.forLanguage(language);
    myControl = control;
    List<CompositeArrangementSettingsToken> tokens = new ArrayList<>();
    tokens.add(new CompositeArrangementSettingsToken(TYPE, ContainerUtil.newArrayList(START_SECTION, END_SECTION)));
    tokens.add(new CompositeArrangementSettingsToken(TEXT));
    myEditor = new ArrangementMatchingRuleEditor(settingsManager, tokens, colorsProvider, control);
  }

  public ArrangementMatchingRuleEditor getEditor() {
    return myEditor;
  }

  @Nonnull
  public static Set<ArrangementSettingsToken> getSectionMutexes() {
    return MUTEXES;
  }

  public static boolean isEnabled(@Nonnull ArrangementSettingsToken token) {
    return TOKENS.contains(token);
  }

  public void showEditor(int rowToEdit) {
    myControl.showEditor(myEditor, rowToEdit);
  }

  public boolean isSectionRule(@Nullable Object element) {
    return element instanceof StdArrangementMatchRule && getSectionRuleData((StdArrangementMatchRule)element) != null;
  }

  @Nullable
  public ArrangementSectionRuleData getSectionRuleData(@Nonnull StdArrangementMatchRule element) {
    ArrangementMatchCondition condition = element.getMatcher().getCondition();
    return getSectionRuleData(condition);
  }

  @Nullable
  public ArrangementSectionRuleData getSectionRuleData(@Nonnull ArrangementMatchCondition condition) {
    final Ref<Boolean> isStart = new Ref<Boolean>();
    final Ref<String> text = new Ref<String>();
    condition.invite(new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
        ArrangementSettingsToken type = condition.getType();
        if (type.equals(START_SECTION)) {
          isStart.set(true);
        }
        else if (type.equals(END_SECTION)) {
          isStart.set(false);
        }
        else if (type.equals(TEXT)) {
          text.set(condition.getValue().toString());
        }
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
        for (ArrangementMatchCondition c : condition.getOperands()) {
          c.invite(this);
          if (!text.isNull() && !isStart.isNull()) {
            return;
          }
        }
      }
    });

    if (isStart.isNull()) {
      return null;
    }
    return new ArrangementSectionRuleData(processSectionText(StringUtil.notNullize(text.get())), isStart.get());
  }

  @Nonnull
  public StdArrangementMatchRule createDefaultSectionRule() {
    ArrangementAtomMatchCondition type = new ArrangementAtomMatchCondition(START_SECTION);
    ArrangementAtomMatchCondition text = new ArrangementAtomMatchCondition(TEXT, createDefaultSectionText());
    ArrangementMatchCondition condition = ArrangementUtil.combine(type, text);
    return new StdArrangementMatchRule(new StdArrangementEntryMatcher(condition));
  }

  @Nonnull
  private String processSectionText(@Nonnull String text) {
    String lineCommentPrefix = myCommenter.getLineCommentPrefix();
    if (lineCommentPrefix != null && text.startsWith(lineCommentPrefix)) {
      return text;
    }

    String prefix = myCommenter.getBlockCommentPrefix();
    String suffix = myCommenter.getBlockCommentSuffix();
    if (prefix != null && suffix != null &&
        text.length() >= prefix.length() + suffix.length() && text.startsWith(prefix) && text.endsWith(suffix)) {
      return text;
    }
    return lineCommentPrefix != null ? wrapIntoLineComment(lineCommentPrefix, text) :
           prefix != null && suffix != null ? wrapIntoBlockComment(prefix, suffix, text) : "";
  }

  @Nonnull
  private String createDefaultSectionText() {
    if (myCommenter != null) {
      String lineCommentPrefix = myCommenter.getLineCommentPrefix();
      if (StringUtil.isNotEmpty(lineCommentPrefix)) {
        return wrapIntoLineComment(lineCommentPrefix, "");
      }

      String prefix = myCommenter.getBlockCommentPrefix();
      String suffix = myCommenter.getBlockCommentSuffix();
      if (StringUtil.isNotEmpty(prefix) && StringUtil.isNotEmpty(suffix)) {
        return wrapIntoBlockComment(prefix, suffix, " ");
      }
    }
    return "";
  }

  private static String wrapIntoBlockComment(@Nonnull String prefix, @Nonnull String suffix, @Nonnull String text) {
    return prefix + text + suffix;
  }

  private static String wrapIntoLineComment(@Nonnull String lineCommentPrefix, @Nonnull String text) {
    return lineCommentPrefix + text;
  }

  public static class ArrangementSectionRuleData {
    private boolean myIsSectionStart;
    private String myText;

    private ArrangementSectionRuleData(@Nonnull String text, boolean isStart) {
      myText = text;
      myIsSectionStart = isStart;
    }

    public boolean isSectionStart() {
      return myIsSectionStart;
    }

    @Nonnull
    public String getText() {
      return myText;
    }
  }
}
