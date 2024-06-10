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

package consulo.ide.impl.idea.application.options.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.action.SmartBackspaceMode;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.editor.CodeInsightSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledComponents;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * To provide additional options in Editor | Smart Keys section register implementation of {@link UnnamedConfigurable} in the plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorSmartKeysConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author yole
 */
@ExtensionImpl
public class EditorSmartKeysConfigurable extends SimpleConfigurable<EditorSmartKeysConfigurable.Panel> implements Configurable, ApplicationConfigurable {
  protected static class Panel implements Supplier<Component> {
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
      myWholeLayout = VerticalLayout.create();
      myWholeLayout.add(myCbSmartHome = CheckBox.create(ApplicationLocalize.checkboxSmartHome().get()));
      myWholeLayout.add(myCbSmartEnd = CheckBox.create(ApplicationLocalize.checkboxSmartEndOnBlankLine().get()));
      myWholeLayout.add(myCbInsertPairBracket = CheckBox.create(ApplicationLocalize.checkboxInsertPairBracket().get()));
      myWholeLayout.add(myCbInsertPairQuote = CheckBox.create(ApplicationLocalize.checkboxInsertPairQuote().get()));
      myWholeLayout.add(myCbReformatBlockOnTypingRBrace = CheckBox.create(ApplicationLocalize.checkboxReformatOnTypingRbrace().get()));
      myWholeLayout.add(myCbCamelWords = CheckBox.create(ApplicationLocalize.checkboxUseCamelhumpsWords().get()));
      myWholeLayout.add(myCbSurroundSelectionOnTyping = CheckBox.create(ApplicationLocalize.checkboxSurroundSelectionOnTypingQuoteOrBrace().get()));
      myWholeLayout.add(mySmartIndentPastedLinesCheckBox = CheckBox.create(ApplicationLocalize.checkboxIndentOnPaste().get()));

      ComboBox.Builder<Integer> reformatOnPasteBuilder = ComboBox.builder();
      reformatOnPasteBuilder
        .add(CodeInsightSettings.NO_REFORMAT, ApplicationLocalize.comboboxPasteReformatNone().get());
      reformatOnPasteBuilder
        .add(CodeInsightSettings.INDENT_BLOCK, ApplicationLocalize.comboboxPasteReformatIndentBlock().get());
      reformatOnPasteBuilder
        .add(CodeInsightSettings.INDENT_EACH_LINE, ApplicationLocalize.comboboxPasteReformatIndentEachLine().get());
      reformatOnPasteBuilder
        .add(CodeInsightSettings.REFORMAT_BLOCK, ApplicationLocalize.comboboxPasteReformatReformatBlock().get());

      myWholeLayout.add(LabeledComponents.left(
        ApplicationLocalize.comboboxPasteReformat().get(),
        myReformatOnPasteCombo = reformatOnPasteBuilder.build()
      ));

      VerticalLayout enterLayout = VerticalLayout.create();
      myWholeLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Enter"), enterLayout));

      enterLayout.add(myCbSmartIndentOnEnter = CheckBox.create(ApplicationLocalize.checkboxSmartIndent().get()));
      enterLayout.add(myCbInsertPairCurlyBraceOnEnter = CheckBox.create(ApplicationLocalize.checkboxInsertPairCurlyBrace().get()));
      enterLayout.add(myCbInsertJavadocStubOnEnter = CheckBox.create(ApplicationLocalize.checkboxJavadocStubAfterSlashStarStar().get()));
      myCbInsertJavadocStubOnEnter.setVisible(hasAnyDocAwareCommenters());

      VerticalLayout backspaceLayout = VerticalLayout.create();
      myWholeLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Backspace"), backspaceLayout));

      ComboBox.Builder<SmartBackspaceMode> smartIndentBuilder = ComboBox.builder();
      smartIndentBuilder.add(SmartBackspaceMode.OFF, ApplicationLocalize.comboboxSmartBackspaceOff().get());
      smartIndentBuilder.add(SmartBackspaceMode.INDENT, ApplicationLocalize.comboboxSmartBackspaceSimple().get());
      smartIndentBuilder.add(SmartBackspaceMode.AUTOINDENT, ApplicationLocalize.comboboxSmartBackspaceSmart().get());
      backspaceLayout.add(LabeledComponents.left(ApplicationLocalize.comboboxSmartBackspace().get(), myCbIndentingBackspace = smartIndentBuilder.build()));
    }

    private static boolean hasAnyDocAwareCommenters() {
      final Collection<Language> languages = Language.getRegisteredLanguages();
      for (Language language : languages) {
        final Commenter commenter = Commenter.forLanguage(language);
        if (commenter instanceof CodeDocumentationAwareCommenter) {
          final CodeDocumentationAwareCommenter docCommenter = (CodeDocumentationAwareCommenter)commenter;
          if (docCommenter.getDocumentationCommentLinePrefix() != null) {
            return true;
          }
        }
      }
      return false;
    }

    @Nonnull
    @Override
    public Component get() {
      return myWholeLayout;
    }
  }

  public EditorSmartKeysConfigurable() {
  }

  @Nonnull
  @Override
  @Nls
  public String getDisplayName() {
    return "Smart Keys";
  }

  @Nonnull
  @Override
  public String getId() {
    return "editor.preferences.smartKeys";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Panel createPanel(@Nonnull Disposable uiDisposable) {
    return new Panel();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Panel panel) {
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
  protected void apply(@Nonnull Panel panel) throws ConfigurationException {
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
  protected void reset(@Nonnull Panel panel) {
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
