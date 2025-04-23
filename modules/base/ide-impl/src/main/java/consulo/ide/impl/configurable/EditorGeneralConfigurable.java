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
package consulo.ide.impl.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.setting.AdditionalEditorGeneralSettingProvider;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.IdentifierHighlighterPass;
import consulo.ide.impl.idea.openapi.editor.richcopy.settings.RichCopySettings;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.application.localize.ApplicationLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-04-19
 */
@ExtensionImpl
public class EditorGeneralConfigurable extends SimpleConfigurableByProperties implements Configurable, ApplicationConfigurable {
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

    CheckBox cbHonorCamelHumpsWhenSelectingByClicking =
      CheckBox.create(ApplicationLocalize.checkboxHonorCamelhumpsWordsSettingsOnDoubleClick());
    mouseLayout.add(cbHonorCamelHumpsWhenSelectingByClicking);
    propertyBuilder.add(
      cbHonorCamelHumpsWhenSelectingByClicking,
      editorSettings::isMouseClickSelectionHonorsCamelWords,
      editorSettings::setMouseClickSelectionHonorsCamelWords
    );

    CheckBox cbEnableWheelFontChange = CheckBox.create(
      Platform.current().os().isMac()
        ? ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSizeMacos()
        : ApplicationLocalize.checkboxEnableCtrlMousewheelChangesFontSize()
    );
    mouseLayout.add(cbEnableWheelFontChange);
    propertyBuilder.add(
      cbEnableWheelFontChange,
      editorSettings::isWheelFontChangeEnabled,
      editorSettings::setWheelFontChangeEnabled
    );

    CheckBox cbEnableDnD = CheckBox.create(ApplicationLocalize.checkboxEnableDragNDropFunctionalityInEditor());
    mouseLayout.add(cbEnableDnD);
    propertyBuilder.add(cbEnableDnD, editorSettings::isDndEnabled, editorSettings::setDndEnabled);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupAdvancedMouseUsages(), mouseLayout));

    VerticalLayout scrollingLayout = VerticalLayout.create();

    CheckBox smoothScrolling = CheckBox.create(ApplicationLocalize.checkboxSmoothScrolling());
    scrollingLayout.add(smoothScrolling);
    propertyBuilder.add(smoothScrolling, editorSettings::isSmoothScrolling, editorSettings::setSmoothScrolling);

    RadioButton preferScrolling =
      RadioButton.create(LocalizeValue.localizeTODO("Prefer scrolling editor canvas to keep caret line centered"));
    RadioButton preferMovingCaret =
      RadioButton.create(LocalizeValue.localizeTODO("Prefer moving caret line to minimize editor scrolling"));

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
    limitsLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.editboxMaximumNumberOfContentsToKeepInClipboard().get(),
      clipboardLimit
    ));
    propertyBuilder.add(
      clipboardLimit,
      () -> uiSettings.MAX_CLIPBOARD_CONTENTS,
      v -> uiSettings.MAX_CLIPBOARD_CONTENTS = v
    );

    IntBox recentLimit = IntBox.create();
    limitsLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.editboxRecentFilesLimit().get(),
      recentLimit
    ));
    propertyBuilder.add(recentLimit, uiSettings::getRecentFilesLimit, uiSettings::setRecentFilesLimit);

    IntBox consoleHistoryLimit = IntBox.create();
    limitsLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.editboxConsoleHistoryLimit().get(),
      consoleHistoryLimit
    ));
    propertyBuilder.add(
      consoleHistoryLimit,
      () -> uiSettings.CONSOLE_COMMAND_HISTORY_LIMIT,
      v -> uiSettings.CONSOLE_COMMAND_HISTORY_LIMIT = v
    );

    layout.add(LabeledLayout.create(ApplicationLocalize.groupLimits(), limitsLayout));

    VerticalLayout virtualSpaceLayout = VerticalLayout.create();

    CheckBox useSoftWrapsInEditor = CheckBox.create(ApplicationLocalize.checkboxUseSoftWrapsAtEditor());
    virtualSpaceLayout.add(useSoftWrapsInEditor);
    propertyBuilder.add(
      useSoftWrapsInEditor,
      () -> editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.MAIN_EDITOR),
      v -> editorSettings.setUseSoftWraps(v, SoftWrapAppliancePlaces.MAIN_EDITOR)
    );

    CheckBox useSoftWrapsInConsole = CheckBox.create(ApplicationLocalize.checkboxUseSoftWrapsAtConsole());
    virtualSpaceLayout.add(useSoftWrapsInConsole);
    propertyBuilder.add(
      useSoftWrapsInConsole,
      () -> editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.CONSOLE),
      v -> editorSettings.setUseSoftWraps(v, SoftWrapAppliancePlaces.CONSOLE)
    );

    CheckBox useCustomSoftWrapIndent = CheckBox.create(ApplicationLocalize.checkboxUseCustomSoftWrapsIndent());
    useCustomSoftWrapIndent.setEnabled(false);
    propertyBuilder.add(
      useCustomSoftWrapIndent,
      editorSettings::isUseCustomSoftWrapIndent,
      editorSettings::setUseCustomSoftWrapIndent
    );

    IntBox customSoftWrapIndent = IntBox.create();
    customSoftWrapIndent.setEnabled(false);
    propertyBuilder.add(
      customSoftWrapIndent,
      editorSettings::getCustomSoftWrapIndent,
      editorSettings::setCustomSoftWrapIndent
    );

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
    propertyBuilder.add(
      allowPlacementCaretInsideTabs,
      editorSettings::isCaretInsideTabs,
      editorSettings::setCaretInsideTabs
    );
    virtualSpaceLayout.add(allowPlacementCaretInsideTabs);

    CheckBox showVirtualSpacesBottom = CheckBox.create(ApplicationLocalize.checkboxShowVirtualSpaceAtFileBottom());
    propertyBuilder.add(
      showVirtualSpacesBottom,
      editorSettings::isAdditionalPageAtBottom,
      editorSettings::setAdditionalPageAtBottom
    );
    virtualSpaceLayout.add(showVirtualSpacesBottom);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupVirtualSpace(), virtualSpaceLayout));

    VerticalLayout highlightCaretMovementLayout = VerticalLayout.create();

    CheckBox highlightMatchedBrace = CheckBox.create(ApplicationLocalize.checkboxHighlightMatchedBrace());
    propertyBuilder.add(
      highlightMatchedBrace,
      () -> codeInsightSettings.HIGHLIGHT_BRACES,
      v -> codeInsightSettings.HIGHLIGHT_BRACES = v
    );
    highlightCaretMovementLayout.add(highlightMatchedBrace);

    CheckBox highlightCurrentScope = CheckBox.create(ApplicationLocalize.checkboxHighlightCurrentScope());
    propertyBuilder.add(
      highlightCurrentScope,
      () -> codeInsightSettings.HIGHLIGHT_SCOPE,
      v -> codeInsightSettings.HIGHLIGHT_SCOPE = v
    );
    highlightCaretMovementLayout.add(highlightCurrentScope);

    CheckBox highlightUsageAtCaret = CheckBox.create(LocalizeValue.localizeTODO("Highlight usages of element at caret"));
    propertyBuilder.add(
      highlightUsageAtCaret,
      () -> codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET,
      v -> codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET = v
    );
    highlightCaretMovementLayout.add(highlightUsageAtCaret);

    layout.add(LabeledLayout.create(ApplicationLocalize.groupBraceHighlighting(), highlightCaretMovementLayout));

    VerticalLayout formattingLayout = VerticalLayout.create();

    CheckBox showNotificationAfterReformat =
      CheckBox.create(LocalizeValue.localizeTODO("Show notification after reformat code action"));
    propertyBuilder.add(
      showNotificationAfterReformat,
      () -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION,
      v -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION = v
    );
    formattingLayout.add(showNotificationAfterReformat);

    CheckBox showNotificationAfterOptimizeImport =
      CheckBox.create(LocalizeValue.localizeTODO("Show notification after optimize imports action"));
    propertyBuilder.add(
      showNotificationAfterOptimizeImport,
      () -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION,
      v -> editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION = v
    );
    formattingLayout.add(showNotificationAfterOptimizeImport);

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Formatting"), formattingLayout));

    VerticalLayout refactoringLayout = VerticalLayout.create();

    CheckBox enableInplaceMode = CheckBox.create(ApplicationLocalize.checkboxRenameLocalVariablesInplace());
    propertyBuilder.add(
      enableInplaceMode,
      editorSettings::isVariableInplaceRenameEnabled,
      editorSettings::setVariableInplaceRenameEnabled
    );
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
        render.append(ApplicationLocalize.comboboxRichcopyColorSchemeActive());
      }
      else if (item instanceof EditorColorsScheme scheme) {
        render.append(scheme.getName());
      }
      else {
        render.append(String.valueOf(item));
      }
    });
    propertyBuilder.add(() -> {
      Object value = colorSchemeBox.getValueOrError();
      if (value instanceof String stringValue) {
        return stringValue;
      }
      else if (value instanceof EditorColorsScheme scheme) {
        return scheme.getName();
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
    propertyBuilder.add(
      errorStripeHeight,
      () -> daemonCodeAnalyzerSettings.ERROR_STRIPE_MARK_MIN_HEIGHT,
      v -> daemonCodeAnalyzerSettings.ERROR_STRIPE_MARK_MIN_HEIGHT = v
    );
    errorHighlightingLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.editboxErrorStripeMarkMinHeightPixels().get(),
      errorStripeHeight
    ));

    IntBox autoReparseDelay = IntBox.create();
    propertyBuilder.add(
      autoReparseDelay,
      () -> daemonCodeAnalyzerSettings.AUTOREPARSE_DELAY,
      v -> daemonCodeAnalyzerSettings.AUTOREPARSE_DELAY = v
    );
    errorHighlightingLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.editboxAutoreparseDelayMs().get(),
      autoReparseDelay
    ));

    CheckBox nextErrorGoPriorityProblem =
      CheckBox.create(ApplicationLocalize.checkboxNextErrorActionGoesToErrorsFirst());
    propertyBuilder.add(
      nextErrorGoPriorityProblem,
      () -> daemonCodeAnalyzerSettings.NEXT_ERROR_ACTION_GOES_TO_ERRORS_FIRST,
      v -> daemonCodeAnalyzerSettings.NEXT_ERROR_ACTION_GOES_TO_ERRORS_FIRST = v
    );
    errorHighlightingLayout.add(nextErrorGoPriorityProblem);

    Application.get().getExtensionPoint(AdditionalEditorGeneralSettingProvider.class)
      .forEach(provider -> provider.fillProperties(propertyBuilder, errorHighlightingLayout::add));

    layout.add(LabeledLayout.create(ApplicationLocalize.groupErrorHighlighting(), errorHighlightingLayout));

    VerticalLayout otherLayout = VerticalLayout.create();

    ComboBox.Builder<String> stripSpacesConfig = ComboBox.builder();
    stripSpacesConfig.add(
      EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED,
      ApplicationLocalize.comboboxStripModifiedLines().get()
    );
    stripSpacesConfig.add(
      EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE,
      ApplicationLocalize.comboboxStripAll().get()
    );
    stripSpacesConfig.add(
      EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE,
      ApplicationLocalize.comboboxStripNone().get()
    );

    ComboBox<String> stripTrailingSpacesOnSave = stripSpacesConfig.build();
    propertyBuilder.add(
      stripTrailingSpacesOnSave,
      editorSettings::getStripTrailingSpaces,
      editorSettings::setStripTrailingSpaces
    );
    otherLayout.add(LabeledComponents.leftWithRight(
      ApplicationLocalize.comboboxStripTrailingSpacesOnSave().get(),
      stripTrailingSpacesOnSave
    ));

    CheckBox ensureLineFeedOnSave = CheckBox.create(LocalizeValue.localizeTODO("Ensure line feed at file end on Save"));
    propertyBuilder.add(
      ensureLineFeedOnSave,
      editorSettings::isEnsureNewLineAtEOF,
      editorSettings::setEnsureNewLineAtEOF
    );
    otherLayout.add(ensureLineFeedOnSave);

    CheckBox showQuickDocOnMouseMove = CheckBox.create(LocalizeValue.localizeTODO("Show quick doc on mouse move"));
    propertyBuilder.add(
      showQuickDocOnMouseMove,
      editorSettings::isShowQuickDocOnMouseOverElement,
      editorSettings::setShowQuickDocOnMouseOverElement
    );

    IntBox tooltipDelay = IntBox.create();
    tooltipDelay.setEnabled(false);
    propertyBuilder.add(tooltipDelay, editorSettings::getTooltipsDelay, editorSettings::setTooltipsDelay);
    Label delayMs = Label.create(LocalizeValue.localizeTODO("Delay (ms)"));
    delayMs.setEnabled(false);

    showQuickDocOnMouseMove.addValueListener(event -> {
      tooltipDelay.setEnabled(showQuickDocOnMouseMove.getValue());
      delayMs.setEnabled(showQuickDocOnMouseMove.getValue());
    });

    otherLayout.add(
      DockLayout.create()
        .left(showQuickDocOnMouseMove)
        .right(HorizontalLayout.create(5).add(delayMs).add(tooltipDelay))
    );

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

  @Nonnull
  @Override
  public String getId() {
    return "editor.general";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  public String getDisplayName() {
    return "General";
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
        if (fileEditor instanceof TextEditor textEditor) {
          Document document = textEditor.getEditor().getDocument();
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
