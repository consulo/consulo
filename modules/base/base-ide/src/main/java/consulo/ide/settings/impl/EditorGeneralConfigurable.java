/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.settings.impl;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPass;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.codeInspection.ui.ErrorPropertiesProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurableByProperties;
import consulo.platform.Platform;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.ui.util.LabeledComponents;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-04-19
 */
public class EditorGeneralConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    UISettings uiSettings = UISettings.getInstance();
    RichCopySettings richCopySettings = RichCopySettings.getInstance();
    DaemonCodeAnalyzerSettings daemonCodeAnalyzerSettings = DaemonCodeAnalyzerSettings.getInstance();

    VerticalLayout layout = VerticalLayout.create();

    VerticalLayout mouseLayout = VerticalLayout.create();

    CheckBox cbHonorCamelHumpsWhenSelectingByClicking = CheckBox.create(ApplicationLocalize.checkboxHonorCamelhumpsWordsSettingsOnDoubleClick());
    mouseLayout.add(cbHonorCamelHumpsWhenSelectingByClicking);
    propertyBuilder.add(cbHonorCamelHumpsWhenSelectingByClicking, editorSettings::isMouseClickSelectionHonorsCamelWords, editorSettings::setMouseClickSelectionHonorsCamelWords);

    CheckBox cbEnableWheelFontChange = CheckBox.create(Platform.current().os().isMac()
                                                       ? ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSizeMacos()
                                                       : ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSize());
    mouseLayout.add(cbEnableWheelFontChange);
    propertyBuilder.add(cbEnableWheelFontChange, editorSettings::isWheelFontChangeEnabled, editorSettings::setWheelFontChangeEnabled);

    CheckBox cbEnableDnD = CheckBox.create(ApplicationLocalize.checkboxEnableDragNDropFunctionalityInEditor());
    mouseLayout.add(cbEnableDnD);
    propertyBuilder.add(cbEnableDnD, editorSettings::isDndEnabled, editorSettings::setDndEnabled);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupAdvancedMouseUsages(), mouseLayout));

    VerticalLayout scrollingLayout = VerticalLayout.create();

    CheckBox smoothScrolling = CheckBox.create(ApplicationLocalize.checkboxSmoothScrolling());
    scrollingLayout.add(smoothScrolling);
    propertyBuilder.add(smoothScrolling, editorSettings::isSmoothScrolling, editorSettings::setSmoothScrolling);

    RadioButton preferScrolling = RadioButton.create(LocalizeValue.localizeTODO("Prefer scrolling editor canvas to keep caret line centered"));
    RadioButton preferMovingCaret = RadioButton.create(LocalizeValue.localizeTODO("Prefer moving caret line to minimize editor scrolling"));

    ValueGroup.createBool().add(preferScrolling).add(preferMovingCaret);

    propertyBuilder.add(() -> {
      if (preferMovingCaret.getValueOrError()) {
        return true;
      }

      if (preferScrolling.getValueOrError()) {
        return false;
      }
      throw new IllegalArgumentException();
    }, uiValue -> {
      preferMovingCaret.setValue(uiValue);
      preferScrolling.setValue(!uiValue);
    }, editorSettings::isRefrainFromScrolling, editorSettings::setRefrainFromScrolling);

    scrollingLayout.add(preferScrolling);
    scrollingLayout.add(preferMovingCaret);

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Scrolling"), scrollingLayout));

    VerticalLayout limitsLayout = VerticalLayout.create();

    IntBox clipboardLimit = IntBox.create();
    limitsLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.maximum.number.of.contents.to.keep.in.clipboard"), clipboardLimit));
    propertyBuilder.add(clipboardLimit, () -> uiSettings.MAX_CLIPBOARD_CONTENTS, v -> uiSettings.MAX_CLIPBOARD_CONTENTS = v);

    IntBox recentLimit = IntBox.create();
    limitsLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.recent.files.limit"), recentLimit));
    propertyBuilder.add(recentLimit, uiSettings::getRecentFilesLimit, uiSettings::setRecentFilesLimit);

    IntBox consoleHistoryLimit = IntBox.create();
    limitsLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.console.history.limit"), consoleHistoryLimit));
    propertyBuilder.add(consoleHistoryLimit, () -> uiSettings.CONSOLE_COMMAND_HISTORY_LIMIT, v -> uiSettings.CONSOLE_COMMAND_HISTORY_LIMIT = v);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupLimits(), limitsLayout));

    VerticalLayout virtualSpaceLayout = VerticalLayout.create();

    CheckBox useSoftWrapsInEditor = CheckBox.create(ApplicationLocalize.checkboxUseSoftWrapsAtEditor());
    virtualSpaceLayout.add(useSoftWrapsInEditor);
    propertyBuilder.add(useSoftWrapsInEditor, () -> editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.MAIN_EDITOR), v -> editorSettings.setUseSoftWraps(v, SoftWrapAppliancePlaces.MAIN_EDITOR));

    CheckBox useSoftWrapsInConsole = CheckBox.create(ApplicationLocalize.checkboxUseSoftWrapsAtConsole());
    virtualSpaceLayout.add(useSoftWrapsInConsole);
    propertyBuilder.add(useSoftWrapsInConsole, () -> editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.CONSOLE), v -> editorSettings.setUseSoftWraps(v, SoftWrapAppliancePlaces.CONSOLE));

    CheckBox useCustomSoftWrapIndent = CheckBox.create(ApplicationLocalize.checkboxUseCustomSoftWrapsIndent());
    useCustomSoftWrapIndent.setEnabled(false);
    propertyBuilder.add(useCustomSoftWrapIndent, editorSettings::isUseCustomSoftWrapIndent, editorSettings::setUseCustomSoftWrapIndent);

    IntBox customSoftWrapIndent = IntBox.create();
    customSoftWrapIndent.setEnabled(false);
    propertyBuilder.add(customSoftWrapIndent, editorSettings::getCustomSoftWrapIndent, editorSettings::setCustomSoftWrapIndent);

    useSoftWrapsInConsole.addValueListener(event -> {
      useCustomSoftWrapIndent.setEnabled(event.getValue());
      customSoftWrapIndent.setEnabled(event.getValue());
    });

    DockLayout customSoftWrapLayout = DockLayout.create();
    customSoftWrapLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 15);
    customSoftWrapLayout.left(useCustomSoftWrapIndent);
    customSoftWrapLayout.right(customSoftWrapIndent);
    virtualSpaceLayout.add(customSoftWrapLayout);

    CheckBox allowPlacementCaretAtLineEnd = CheckBox.create(ApplicationLocalize.checkboxAllowPlacementOfCaretAfterEndOfLine());
    propertyBuilder.add(allowPlacementCaretAtLineEnd, editorSettings::isVirtualSpace, editorSettings::setVirtualSpace);
    virtualSpaceLayout.add(allowPlacementCaretAtLineEnd);

    CheckBox allowPlacementCaretInsideTabs = CheckBox.create(ApplicationLocalize.checkboxAllowPlacementOfCaretInsideTabs());
    propertyBuilder.add(allowPlacementCaretInsideTabs, editorSettings::isCaretInsideTabs, editorSettings::setCaretInsideTabs);
    virtualSpaceLayout.add(allowPlacementCaretInsideTabs);

    CheckBox showVirtualSpacesBottom = CheckBox.create(ApplicationLocalize.checkboxShowVirtualSpaceAtFileBottom());
    propertyBuilder.add(showVirtualSpacesBottom, editorSettings::isAdditionalPageAtBottom, editorSettings::setAdditionalPageAtBottom);
    virtualSpaceLayout.add(showVirtualSpacesBottom);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupVirtualSpace(), virtualSpaceLayout));

    VerticalLayout highlightCaretMovementLayout = VerticalLayout.create();

    CheckBox highlightMatchedBrace = CheckBox.create(ApplicationLocalize.checkboxHighlightMatchedBrace());
    propertyBuilder.add(highlightMatchedBrace, () -> codeInsightSettings.HIGHLIGHT_BRACES, v -> codeInsightSettings.HIGHLIGHT_BRACES = v);
    highlightCaretMovementLayout.add(highlightMatchedBrace);

    CheckBox highlightCurrentScope = CheckBox.create(ApplicationLocalize.checkboxHighlightCurrentScope());
    propertyBuilder.add(highlightCurrentScope, () -> codeInsightSettings.HIGHLIGHT_SCOPE, v -> codeInsightSettings.HIGHLIGHT_SCOPE = v);
    highlightCaretMovementLayout.add(highlightCurrentScope);

    CheckBox highlightUsageAtCaret = CheckBox.create(LocalizeValue.localizeTODO("Highlight usages of element at caret"));
    propertyBuilder.add(highlightUsageAtCaret, () -> codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET, v -> codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET = v);
    highlightCaretMovementLayout.add(highlightUsageAtCaret);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupBraceHighlighting(), highlightCaretMovementLayout));

    VerticalLayout formattingLayout = VerticalLayout.create();

    CheckBox showNotificationAfterReformat = CheckBox.create(LocalizeValue.localizeTODO("Show notification after reformat code action"));
    propertyBuilder.add(showNotificationAfterReformat, () -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION,
                        v -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION = v);
    formattingLayout.add(showNotificationAfterReformat);

    CheckBox showNotificationAfterOptimizeImport = CheckBox.create(LocalizeValue.localizeTODO("Show notification after optimize imports action"));
    propertyBuilder.add(showNotificationAfterOptimizeImport, () -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION,
                        v -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION = v);
    formattingLayout.add(showNotificationAfterOptimizeImport);

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Formatting"), formattingLayout));

    VerticalLayout refactoringLayout = VerticalLayout.create();

    CheckBox enableInplaceMode = CheckBox.create(ApplicationLocalize.checkboxRenameLocalVariablesInplace());
    propertyBuilder.add(enableInplaceMode, editorSettings::isVariableInplaceRenameEnabled, editorSettings::setVariableInplaceRenameEnabled);
    refactoringLayout.add(enableInplaceMode);

    CheckBox presectOldName = CheckBox.create(ApplicationLocalize.checkboxRenameLocalVariablesPreselect());
    propertyBuilder.add(enableInplaceMode, editorSettings::isPreselectRename, editorSettings::setPreselectRename);
    refactoringLayout.add(presectOldName);

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Refactorings"), refactoringLayout));

    VerticalLayout richCopyLayout = VerticalLayout.create();

    List<Object> colorSchemeList = new ArrayList<>();
    colorSchemeList.add(RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER);
    EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
    EditorColorsScheme[] schemes = editorColorsManager.getAllSchemes();
    ContainerUtil.addAll(colorSchemeList, schemes);
    ComboBox<Object> colorSchemeBox = ComboBox.create(colorSchemeList);
    colorSchemeBox.setRender((render, index, item) -> {
      if (RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER.equals(item)) {
        render.append(ApplicationBundle.message("combobox.richcopy.color.scheme.active"));
      }
      else if (item instanceof EditorColorsScheme) {
        render.append(((EditorColorsScheme)item).getName());
      }
      else {
        render.append(String.valueOf(item));
      }
    });
    propertyBuilder.add(() -> {
      Object value = colorSchemeBox.getValueOrError();
      if (value instanceof String) {
        return (String)value;
      }
      else if (value instanceof EditorColorsScheme) {
        return ((EditorColorsScheme)value).getName();
      }
      return null;
    }, schemeName -> {
      if (RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER.equals(schemeName)) {
        colorSchemeBox.setValue(schemeName);
      }

      EditorColorsScheme scheme = editorColorsManager.getScheme(schemeName);
      if (scheme != null) {
        colorSchemeBox.setValue(scheme);
      }
      else {
        colorSchemeBox.setValue(RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER);
      }
    }, richCopySettings::getSchemeName, richCopySettings::setSchemeName);
    richCopyLayout.add(LabeledBuilder.sided(ApplicationLocalize.comboboxRichcopyColorScheme(), colorSchemeBox));

    layout.add(LabeledLayout.create(ApplicationLocalize.groupRichcopy(), richCopyLayout));

    VerticalLayout errorHighlightingLayout = VerticalLayout.create();

    IntBox errorStripeHeight = IntBox.create();
    propertyBuilder.add(errorStripeHeight, () -> daemonCodeAnalyzerSettings.ERROR_STRIPE_MARK_MIN_HEIGHT, v -> daemonCodeAnalyzerSettings.ERROR_STRIPE_MARK_MIN_HEIGHT = v);
    errorHighlightingLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.error.stripe.mark.min.height.pixels"), errorStripeHeight));

    IntBox autoReparseDelay = IntBox.create();
    propertyBuilder.add(autoReparseDelay, () -> daemonCodeAnalyzerSettings.AUTOREPARSE_DELAY, v -> daemonCodeAnalyzerSettings.AUTOREPARSE_DELAY = v);
    errorHighlightingLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("editbox.autoreparse.delay.ms"), autoReparseDelay));

    CheckBox nextErrorGoPriorityProblem = CheckBox.create(ApplicationBundle.message("checkbox.next.error.action.goes.to.errors.first"));
    propertyBuilder
            .add(nextErrorGoPriorityProblem, () -> daemonCodeAnalyzerSettings.NEXT_ERROR_ACTION_GOES_TO_ERRORS_FIRST, v -> daemonCodeAnalyzerSettings.NEXT_ERROR_ACTION_GOES_TO_ERRORS_FIRST = v);
    errorHighlightingLayout.add(nextErrorGoPriorityProblem);

    for (ErrorPropertiesProvider provider : ErrorPropertiesProvider.EP_NAME.getExtensionList()) {
      provider.fillProperties(errorHighlightingLayout::add, propertyBuilder);
    }

    layout.add(LabeledLayout.create(ApplicationLocalize.groupErrorHighlighting(), errorHighlightingLayout));

    VerticalLayout otherLayout = VerticalLayout.create();

    ComboBox.Builder<String> stripSpacesConfig = ComboBox.builder();
    stripSpacesConfig.add(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED, ApplicationBundle.message("combobox.strip.modified.lines"));
    stripSpacesConfig.add(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE, ApplicationBundle.message("combobox.strip.all"));
    stripSpacesConfig.add(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE, ApplicationBundle.message("combobox.strip.none"));

    ComboBox<String> stripTrailingSpacesOnSave = stripSpacesConfig.build();
    propertyBuilder.add(stripTrailingSpacesOnSave, editorSettings::getStripTrailingSpaces, editorSettings::setStripTrailingSpaces);
    otherLayout.add(LabeledComponents.leftWithRight(ApplicationBundle.message("combobox.strip.trailing.spaces.on.save"), stripTrailingSpacesOnSave));

    CheckBox ensureLineFeedOnSave = CheckBox.create(LocalizeValue.localizeTODO("Ensure line feed at file end on Save"));
    propertyBuilder.add(ensureLineFeedOnSave, editorSettings::isEnsureNewLineAtEOF, editorSettings::setEnsureNewLineAtEOF);
    otherLayout.add(ensureLineFeedOnSave);

    CheckBox showQuickDocOnMouseMove = CheckBox.create(LocalizeValue.localizeTODO("Show quick doc on mouse move"));
    propertyBuilder.add(showQuickDocOnMouseMove, editorSettings::isShowQuickDocOnMouseOverElement, editorSettings::setShowQuickDocOnMouseOverElement);

    IntBox tooltipDelay = IntBox.create();
    tooltipDelay.setEnabled(false);
    propertyBuilder.add(tooltipDelay, editorSettings::getTooltipsDelay, editorSettings::setTooltipsDelay);
    Label delayMs = Label.create(LocalizeValue.localizeTODO("Delay (ms)"));
    delayMs.setEnabled(false);

    showQuickDocOnMouseMove.addValueListener(event -> {
      tooltipDelay.setEnabled(showQuickDocOnMouseMove.getValue());
      delayMs.setEnabled(showQuickDocOnMouseMove.getValue());
    });

    otherLayout.add(DockLayout.create().left(showQuickDocOnMouseMove).right(HorizontalLayout.create(5).add(delayMs).add(tooltipDelay)));

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Other"), otherLayout));
    return layout;
  }

  @Override
  protected void afterApply() {
    UISettings uiSettings = UISettings.getInstance();

    uiSettings.fireUISettingsChanged();
    
    clearAllIdentifierHighlighters();

    reinitAllEditors();

    restartDaemons();
  }

  public static void restartDaemons() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      DaemonCodeAnalyzer.getInstance(project).settingsChanged();
    }
  }

  private static void clearAllIdentifierHighlighters() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
        if (fileEditor instanceof TextEditor) {
          Document document = ((TextEditor)fileEditor).getEditor().getDocument();
          IdentifierHighlighterPass.clearMyHighlights(document, project);
        }
      }
    }
  }

  public static void reinitAllEditors() {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      ((EditorEx)editor).reinitSettings();
    }
  }
}
