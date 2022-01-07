/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.application.options;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.DataManager;
import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullComputable;
import com.intellij.openapi.util.text.StringUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.Comparing;
import consulo.util.lang.ThreeState;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;

public class CodeCompletionPanel implements NotNullComputable<Layout> {
  private final VerticalLayout myLayout;
  private final ComboBox<ThreeState> myCaseSensitiveCombo2;
  private final CheckBox myCbOnCodeCompletion2;
  private final CheckBox myCbOnSmartTypeCompletion2;
  private final CheckBox myCbSorting2;
  private final CheckBox myCbAutocompletion2;
  private final CheckBox myCbSelectByChars2;
  private final CheckBox myCbAutopopupJavaDoc2;
  private final IntBox myAutopopupJavaDocField2;
  private final CheckBox myCbParameterInfoPopup2;
  private final IntBox myParameterInfoDelayField2;
  private final CheckBox myCbShowFullParameterSignatures2;

  @RequiredUIAccess
  public CodeCompletionPanel(ActionManager actionManager) {
    myLayout = VerticalLayout.create();

    ComboBox.Builder<ThreeState> builder = ComboBox.builder();
    builder.fillByEnumLocalized(ThreeState.class, o -> {
      switch (o) {
        case YES:
          return ApplicationLocalize.comboboxAutocompleteCaseSensitiveAll();
        case NO:
          return ApplicationLocalize.comboboxAutocompleteCaseSensitiveNone();
        case UNSURE:
          return ApplicationLocalize.comboboxAutocompleteCaseSensitiveFirstLetter();
        default:
          throw new UnsupportedOperationException();
      }
    });
    myCaseSensitiveCombo2 = builder.build();

    VerticalLayout completionOptions = VerticalLayout.create();
    completionOptions.add(LabeledBuilder.sided(ApplicationLocalize.comboboxCaseSensitiveCompletion(), myCaseSensitiveCombo2));

    completionOptions.add(Label.create(ApplicationLocalize.labelAutocompleteWhenOnlyOneChoice()));

    String basicShortcut = KeymapUtil.getFirstKeyboardShortcutText(actionManager.getAction(IdeActions.ACTION_CODE_COMPLETION));
    if (StringUtil.isNotEmpty(basicShortcut)) {
      LocalizeValue value = ApplicationLocalize.checkboxAutocompleteBasic().map((localizeManager, s) -> s + " (" + basicShortcut + ")");
      myCbOnCodeCompletion2 = CheckBox.create(value);
    }
    else {
      myCbOnCodeCompletion2 = CheckBox.create(ApplicationLocalize.checkboxAutocompleteBasic());
    }

    String smartShortcut = KeymapUtil.getFirstKeyboardShortcutText(actionManager.getAction(IdeActions.ACTION_SMART_TYPE_COMPLETION));
    if (StringUtil.isNotEmpty(smartShortcut)) {
      LocalizeValue value = ApplicationLocalize.checkboxAutocompleteSmartType().map((localizeManager, s) -> s + " (" + smartShortcut + ")");
      myCbOnSmartTypeCompletion2 = CheckBox.create(value);
    }
    else {
      myCbOnSmartTypeCompletion2 = CheckBox.create(ApplicationLocalize.checkboxAutocompleteSmartType());
    }

    VerticalLayout complGroup = VerticalLayout.create();
    complGroup.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, Image.DEFAULT_ICON_SIZE);
    complGroup.add(myCbOnCodeCompletion2);
    complGroup.add(myCbOnSmartTypeCompletion2);
    completionOptions.add(complGroup);

    myCbSorting2 = CheckBox.create(LocalizeValue.localizeTODO("Sort lookup items lexicographically"));

    if(PowerSaveMode.isEnabled()) {
      myCbAutocompletion2 = CheckBox.create(LocalizeValue.localizeTODO("Autopopup code completion (not available in Power Save mode)"));
    }
    else {
      myCbAutocompletion2 = CheckBox.create(LocalizeValue.localizeTODO("Autopopup code completion"));
    }

    completionOptions.add(myCbAutocompletion2);

    myCbSelectByChars2 = CheckBox.create(LocalizeValue.localizeTODO("Insert selected variant by typing dot, space, etc."));
    myCbSelectByChars2.setEnabled(false);

    VerticalLayout indentChars = VerticalLayout.create().add(myCbSelectByChars2);
    indentChars.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, Image.DEFAULT_ICON_SIZE);
    completionOptions.add(indentChars);

    DockLayout autoPopuDocLine = DockLayout.create();
    myCbAutopopupJavaDoc2 = CheckBox.create(ApplicationLocalize.editboxAutopopupJavadocInMs());
    autoPopuDocLine.left(myCbAutopopupJavaDoc2);
    myAutopopupJavaDocField2 = IntBox.create();
    myAutopopupJavaDocField2.setEnabled(false);
    autoPopuDocLine.right(myAutopopupJavaDocField2);

    completionOptions.add(autoPopuDocLine);

    myLayout.add(LabeledLayout.create(ApplicationLocalize.titleCodeCompletion(), completionOptions));

    VerticalLayout parameterInfoGroup = VerticalLayout.create();

    myCbParameterInfoPopup2 = CheckBox.create(ApplicationLocalize.editboxAutopopupInMs());

    myParameterInfoDelayField2 = IntBox.create();
    myParameterInfoDelayField2.setEnabled(false);

    parameterInfoGroup.add(DockLayout.create().left(myCbParameterInfoPopup2).right(myParameterInfoDelayField2));

    myCbShowFullParameterSignatures2 = CheckBox.create(ApplicationLocalize.checkboxShowFullSignatures());

    parameterInfoGroup.add(myCbShowFullParameterSignatures2);
    
    myLayout.add(LabeledLayout.create(ApplicationLocalize.titleParameterInfo(), parameterInfoGroup));

    myCbAutocompletion2.addValueListener(event -> myCbSelectByChars2.setEnabled(myCbAutocompletion2.getValue()));

    myCbAutopopupJavaDoc2.addValueListener(event -> myAutopopupJavaDocField2.setEnabled(myCbAutopopupJavaDoc2.getValue()));

    myCbParameterInfoPopup2.addValueListener(event -> myParameterInfoDelayField2.setEnabled(myCbParameterInfoPopup2.getValue()));
  }

  @RequiredUIAccess
  public void reset() {
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    final ThreeState caseSensitiveValue;
    switch (codeInsightSettings.COMPLETION_CASE_SENSITIVE) {
      case CodeInsightSettings.ALL:
        caseSensitiveValue = ThreeState.YES;
        break;
      case CodeInsightSettings.NONE:
        caseSensitiveValue = ThreeState.NO;
        break;
      default:
        caseSensitiveValue = ThreeState.UNSURE;
        break;
    }
    myCaseSensitiveCombo2.setValue(caseSensitiveValue);

    myCbSelectByChars2.setValue(codeInsightSettings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS);

    myCbOnCodeCompletion2.setValue(codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION);
    myCbOnSmartTypeCompletion2.setValue(codeInsightSettings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION);

    myCbAutocompletion2.setValue(codeInsightSettings.AUTO_POPUP_COMPLETION_LOOKUP);

    myCbAutopopupJavaDoc2.setValue(codeInsightSettings.AUTO_POPUP_JAVADOC_INFO);
    myAutopopupJavaDocField2.setEnabled(codeInsightSettings.AUTO_POPUP_JAVADOC_INFO);
    myAutopopupJavaDocField2.setValue(codeInsightSettings.JAVADOC_INFO_DELAY);

    myCbParameterInfoPopup2.setValue(codeInsightSettings.AUTO_POPUP_PARAMETER_INFO);
    myParameterInfoDelayField2.setEnabled(codeInsightSettings.AUTO_POPUP_PARAMETER_INFO);
    myParameterInfoDelayField2.setValue(codeInsightSettings.PARAMETER_INFO_DELAY);
    myCbShowFullParameterSignatures2.setValue(codeInsightSettings.SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO);

    myCbSorting2.setValue(UISettings.getInstance().SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY);
  }

  public void apply() {
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    codeInsightSettings.COMPLETION_CASE_SENSITIVE = getCaseSensitiveValue();

    codeInsightSettings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = myCbSelectByChars2.getValue();
    codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION = myCbOnCodeCompletion2.getValue();
    codeInsightSettings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = myCbOnSmartTypeCompletion2.getValue();
    codeInsightSettings.SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO = myCbShowFullParameterSignatures2.getValue();

    codeInsightSettings.AUTO_POPUP_PARAMETER_INFO = myCbParameterInfoPopup2.getValue();
    codeInsightSettings.AUTO_POPUP_COMPLETION_LOOKUP = myCbAutocompletion2.getValue();
    codeInsightSettings.AUTO_POPUP_JAVADOC_INFO = myCbAutopopupJavaDoc2.getValue();

    codeInsightSettings.PARAMETER_INFO_DELAY = myParameterInfoDelayField2.getValueOrError();
    codeInsightSettings.JAVADOC_INFO_DELAY = myAutopopupJavaDocField2.getValueOrError();

    UISettings.getInstance().SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY = myCbSorting2.getValue();

    Project project = DataManager.getInstance().getDataContext(myLayout).getData(CommonDataKeys.PROJECT);
    if (project != null) {
      DaemonCodeAnalyzer.getInstance(project).settingsChanged();
    }
  }

  public boolean isModified() {
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    boolean isModified = false;

    //noinspection ConstantConditions
    isModified |= getCaseSensitiveValue() != codeInsightSettings.COMPLETION_CASE_SENSITIVE;

    isModified |= isModified(myCbOnCodeCompletion2, codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION);
    isModified |= isModified(myCbSelectByChars2, codeInsightSettings.SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS);
    isModified |= isModified(myCbOnSmartTypeCompletion2, codeInsightSettings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION);
    isModified |= isModified(myCbShowFullParameterSignatures2, codeInsightSettings.SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO);
    isModified |= isModified(myCbParameterInfoPopup2, codeInsightSettings.AUTO_POPUP_PARAMETER_INFO);
    isModified |= isModified(myCbAutocompletion2, codeInsightSettings.AUTO_POPUP_COMPLETION_LOOKUP);

    isModified |= isModified(myCbAutopopupJavaDoc2, codeInsightSettings.AUTO_POPUP_JAVADOC_INFO);
    isModified |= isModified(myParameterInfoDelayField2, codeInsightSettings.PARAMETER_INFO_DELAY);
    isModified |= isModified(myAutopopupJavaDocField2, codeInsightSettings.JAVADOC_INFO_DELAY);
    isModified |= isModified(myCbSorting2, UISettings.getInstance().SORT_LOOKUP_ELEMENTS_LEXICOGRAPHICALLY);

    return isModified;
  }

  @Nonnull
  @Override
  public Layout compute() {
    return myLayout;
  }

  private static <V> boolean isModified(ValueComponent<V> valueComponent, V value) {
    return !Comparing.equal(valueComponent.getValue(), value);
  }

  @MagicConstant(intValues = {CodeInsightSettings.ALL, CodeInsightSettings.NONE, CodeInsightSettings.FIRST_LETTER})
  private int getCaseSensitiveValue() {
    ThreeState value = myCaseSensitiveCombo2.getValue();
    if (value == ThreeState.YES) {
      return CodeInsightSettings.ALL;
    }
    else if (value == ThreeState.NO) {
      return CodeInsightSettings.NONE;
    }
    else {
      return CodeInsightSettings.FIRST_LETTER;
    }
  }

}