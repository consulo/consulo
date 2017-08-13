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

package com.intellij.application.options.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.SmartBackspaceMode;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullComputable;
import consulo.options.SimpleConfigurable;
import consulo.ui.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * To provide additional options in Editor | Smart Keys section register implementation of {@link com.intellij.openapi.options.UnnamedConfigurable} in the plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorSmartKeysConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author yole
 */
public class EditorSmartKeysConfigurable extends SimpleConfigurable<EditorSmartKeysConfigurable.Panel> implements Configurable {
  protected static class Panel implements NotNullComputable<Component> {
    private CheckBox myCbSmartHome;
    private CheckBox myCbSmartEnd;
    private CheckBox myCbInsertPairBracket;
    private CheckBox myCbInsertPairQuote;
    private CheckBox myCbCamelWords;
    private CheckBox myCbSmartIndentOnEnter;
    private ComboBox<Integer> myReformatOnPasteCombo;

    private CheckBox myCbInsertPairCurlyBraceOnEnter;
    private CheckBox myCbInsertJavadocStubOnEnter;
    private CheckBox myCbSurroundSelectionOnTyping;
    private CheckBox myCbReformatBlockOnTypingRBrace;
    private CheckBox mySmartIndentPastedLinesCheckBox;
    private ComboBox<SmartBackspaceMode> myCbIndentingBackspace;

    private final VerticalLayout myWholeLayout;

    @RequiredUIAccess
    public Panel() {
      myWholeLayout = Layouts.vertical();
      myWholeLayout.add(myCbSmartHome = Components.checkBox(ApplicationBundle.message("checkbox.smart.home")));
      myWholeLayout.add(myCbSmartEnd = Components.checkBox(ApplicationBundle.message("checkbox.smart.end.on.blank.line")));
      myWholeLayout.add(myCbInsertPairBracket = Components.checkBox(ApplicationBundle.message("checkbox.insert.pair.bracket")));
      myWholeLayout.add(myCbInsertPairQuote = Components.checkBox(ApplicationBundle.message("checkbox.insert.pair.quote")));
      myWholeLayout.add(myCbReformatBlockOnTypingRBrace = Components.checkBox(ApplicationBundle.message("checkbox.reformat.on.typing.rbrace")));
      myWholeLayout.add(myCbCamelWords = Components.checkBox(ApplicationBundle.message("checkbox.use.camelhumps.words")));
      myWholeLayout.add(myCbSurroundSelectionOnTyping = Components.checkBox(ApplicationBundle.message("checkbox.surround.selection.on.typing.quote.or.brace")));
      myWholeLayout.add(mySmartIndentPastedLinesCheckBox = Components.checkBox(ApplicationBundle.message("checkbox.indent.on.paste")));

      ComboBoxes.SimpleBuilder<Integer> reformatOnPasteBuilder = ComboBoxes.simple();
      reformatOnPasteBuilder.add(CodeInsightSettings.NO_REFORMAT, ApplicationBundle.message("combobox.paste.reformat.none"));
      reformatOnPasteBuilder.add(CodeInsightSettings.INDENT_BLOCK, ApplicationBundle.message("combobox.paste.reformat.indent.block"));
      reformatOnPasteBuilder.add(CodeInsightSettings.INDENT_EACH_LINE, ApplicationBundle.message("combobox.paste.reformat.indent.each.line"));
      reformatOnPasteBuilder.add(CodeInsightSettings.REFORMAT_BLOCK, ApplicationBundle.message("combobox.paste.reformat.reformat.block"));

      myWholeLayout.add(LabeledComponents.left(ApplicationBundle.message("combobox.paste.reformat"), myReformatOnPasteCombo = reformatOnPasteBuilder.build()));

      VerticalLayout enterLayout = Layouts.vertical();
      myWholeLayout.add(Layouts.labeled("Enter").set(enterLayout));

      enterLayout.add(myCbSmartIndentOnEnter = Components.checkBox(ApplicationBundle.message("checkbox.smart.indent")));
      enterLayout.add(myCbInsertPairCurlyBraceOnEnter = Components.checkBox(ApplicationBundle.message("checkbox.insert.pair.curly.brace")));
      enterLayout.add(myCbInsertJavadocStubOnEnter = Components.checkBox(ApplicationBundle.message("checkbox.javadoc.stub.after.slash.star.star")));
      myCbInsertJavadocStubOnEnter.setVisible(hasAnyDocAwareCommenters());

      VerticalLayout backspaceLayout = Layouts.vertical();
      myWholeLayout.add(Layouts.labeled("Backspace").set(backspaceLayout));

      ComboBoxes.SimpleBuilder<SmartBackspaceMode> smartIndentBuilder = ComboBoxes.simple();
      smartIndentBuilder.add(SmartBackspaceMode.OFF, ApplicationBundle.message("combobox.smart.backspace.off"));
      smartIndentBuilder.add(SmartBackspaceMode.INDENT, ApplicationBundle.message("combobox.smart.backspace.simple"));
      smartIndentBuilder.add(SmartBackspaceMode.AUTOINDENT, ApplicationBundle.message("combobox.smart.backspace.smart"));
      backspaceLayout.add(LabeledComponents.left(ApplicationBundle.message("combobox.smart.backspace"), myCbIndentingBackspace = smartIndentBuilder.build()));
    }

    private static boolean hasAnyDocAwareCommenters() {
      final Collection<Language> languages = Language.getRegisteredLanguages();
      for (Language language : languages) {
        final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
        if (commenter instanceof CodeDocumentationAwareCommenter) {
          final CodeDocumentationAwareCommenter docCommenter = (CodeDocumentationAwareCommenter)commenter;
          if (docCommenter.getDocumentationCommentLinePrefix() != null) {
            return true;
          }
        }
      }
      return false;
    }

    @NotNull
    @Override
    public Component compute() {
      return myWholeLayout;
    }
  }

  public EditorSmartKeysConfigurable() {
  }

  @Override
  @Nls
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.smartkey";
  }

  @RequiredUIAccess
  @NotNull
  @Override
  protected Panel createPanel() {
    return new Panel();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@NotNull Panel panel) {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    boolean isModified = isModified(panel.myReformatOnPasteCombo, codeInsightSettings.REFORMAT_ON_PASTE);
    isModified |= isModified(panel.myCbSmartHome, editorSettings.isSmartHome());
    isModified |= isModified(panel.myCbSmartEnd, codeInsightSettings.SMART_END_ACTION);

    isModified |= isModified(panel.myCbSmartIndentOnEnter, codeInsightSettings.SMART_INDENT_ON_ENTER);
    isModified |= isModified(panel.myCbInsertPairCurlyBraceOnEnter, codeInsightSettings.INSERT_BRACE_ON_ENTER);
    isModified |= isModified(panel.myCbInsertJavadocStubOnEnter, codeInsightSettings.JAVADOC_STUB_ON_ENTER);

    isModified |= isModified(panel.myCbInsertPairBracket, codeInsightSettings.AUTOINSERT_PAIR_BRACKET);
    isModified |= isModified(panel.mySmartIndentPastedLinesCheckBox, codeInsightSettings.INDENT_TO_CARET_ON_PASTE);
    isModified |= isModified(panel.myCbInsertPairQuote, codeInsightSettings.AUTOINSERT_PAIR_QUOTE);
    isModified |= isModified(panel.myCbReformatBlockOnTypingRBrace, codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE);
    isModified |= isModified(panel.myCbCamelWords, editorSettings.isCamelWords());

    isModified |= isModified(panel.myCbSurroundSelectionOnTyping, codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED);

    isModified |= isModified(panel.myCbIndentingBackspace, codeInsightSettings.getBackspaceMode());

    return isModified;
  }

  @RequiredUIAccess
  @Override
  protected void apply(@NotNull Panel panel) throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    editorSettings.setSmartHome(panel.myCbSmartHome.getValue());
    codeInsightSettings.SMART_END_ACTION = panel.myCbSmartEnd.getValue();
    codeInsightSettings.SMART_INDENT_ON_ENTER = panel.myCbSmartIndentOnEnter.getValue();
    codeInsightSettings.INSERT_BRACE_ON_ENTER = panel.myCbInsertPairCurlyBraceOnEnter.getValue();
    codeInsightSettings.INDENT_TO_CARET_ON_PASTE = panel.mySmartIndentPastedLinesCheckBox.getValue();
    codeInsightSettings.JAVADOC_STUB_ON_ENTER = panel.myCbInsertJavadocStubOnEnter.getValue();
    codeInsightSettings.AUTOINSERT_PAIR_BRACKET = panel.myCbInsertPairBracket.getValue();
    codeInsightSettings.AUTOINSERT_PAIR_QUOTE = panel.myCbInsertPairQuote.getValue();
    codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE = panel.myCbReformatBlockOnTypingRBrace.getValue();
    codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED = panel.myCbSurroundSelectionOnTyping.getValue();
    editorSettings.setCamelWords(panel.myCbCamelWords.getValue());
    codeInsightSettings.REFORMAT_ON_PASTE = panel.myReformatOnPasteCombo.getValue();
    codeInsightSettings.setBackspaceMode(panel.myCbIndentingBackspace.getValue());
  }

  @RequiredUIAccess
  @Override
  protected void reset(@NotNull Panel panel) {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    panel.myReformatOnPasteCombo.setValue(codeInsightSettings.REFORMAT_ON_PASTE);

    panel.myCbSmartHome.setValue(editorSettings.isSmartHome());
    panel.myCbSmartEnd.setValue(codeInsightSettings.SMART_END_ACTION);

    panel.myCbSmartIndentOnEnter.setValue(codeInsightSettings.SMART_INDENT_ON_ENTER);
    panel.myCbInsertPairCurlyBraceOnEnter.setValue(codeInsightSettings.INSERT_BRACE_ON_ENTER);
    panel.myCbInsertJavadocStubOnEnter.setValue(codeInsightSettings.JAVADOC_STUB_ON_ENTER);

    panel.myCbInsertPairBracket.setValue(codeInsightSettings.AUTOINSERT_PAIR_BRACKET);
    panel.mySmartIndentPastedLinesCheckBox.setValue(codeInsightSettings.INDENT_TO_CARET_ON_PASTE);
    panel.myCbInsertPairQuote.setValue(codeInsightSettings.AUTOINSERT_PAIR_QUOTE);
    panel.myCbReformatBlockOnTypingRBrace.setValue(codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE);
    panel.myCbCamelWords.setValue(editorSettings.isCamelWords());

    panel.myCbSurroundSelectionOnTyping.setValue(codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED);

    panel.myCbIndentingBackspace.setValue(codeInsightSettings.getBackspaceMode());
  }

  private static <T> boolean isModified(ValueComponent<T> checkBox, T value) {
    return !Comparing.equal(checkBox.getValue(), value);
  }
}
