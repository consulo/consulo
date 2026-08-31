/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.setting.AdditionalEditorAppearanceSettingProvider;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.TabCharacterPaintMode;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-Jul-22
 */
@ExtensionImpl
public class EditorAppearanceConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    public static final Comparator<AdditionalEditorAppearanceSettingProvider> APPEARANCE_SETTING_PROVIDER_LABEL_NAME_COMPARATOR =
        Comparator.comparing(AdditionalEditorAppearanceSettingProvider::getLabelName, LocalizeValue.comparator());

    private final Application myApplication;
    private final Provider<PersistentEditorSettings> myEditorSettingsExternalizable;
    private final Provider<CodeEditorInternalHelper> myEditorInternalHelper;

    @Inject
    public EditorAppearanceConfigurable(Application application,
                                        Provider<PersistentEditorSettings> editorSettingsExternalizable,
                                        Provider<CodeEditorInternalHelper> editorInternalHelper) {
        myApplication = application;
        myEditorSettingsExternalizable = editorSettingsExternalizable;
        myEditorInternalHelper = editorInternalHelper;
    }

    @RequiredUIAccess
    
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        PersistentEditorSettings editorSettings = myEditorSettingsExternalizable.get();
        CodeEditorInternalHelper codeEditorInternalHelper = myEditorInternalHelper.get();

        VerticalLayout root = VerticalLayout.create();

        CheckBox caretBlinkingBox = CheckBox.create(ApplicationLocalize.checkboxCaretBlinkingMs());
        propertyBuilder.add(caretBlinkingBox, editorSettings::isBlinkCaret, editorSettings::setBlinkCaret);

        IntBox caretBlinkingTimeBox = IntBox.create();
        caretBlinkingBox.addValueListener(event -> caretBlinkingTimeBox.setEnabled(event.getValue()));
        propertyBuilder.add(caretBlinkingTimeBox, editorSettings::getBlinkPeriod, editorSettings::setBlinkPeriod);
        root.add(DockLayout.create().left(caretBlinkingBox).right(caretBlinkingTimeBox));

        CheckBox smoothBlinkBox = CheckBox.create(LocalizeValue.localizeTODO("Smooth caret blinking"));
        propertyBuilder.add(smoothBlinkBox, editorSettings::isSmoothCaretBlinking, editorSettings::setSmoothCaretBlinking);
        smoothBlinkBox.setEnabled(editorSettings.isBlinkCaret());
        caretBlinkingBox.addValueListener(event -> smoothBlinkBox.setEnabled(event.getValue()));
        root.add(smoothBlinkBox);

        CheckBox animatedCaretBox = CheckBox.create(LocalizeValue.localizeTODO("Use animated caret"));
        propertyBuilder.add(animatedCaretBox, editorSettings::isAnimatedCaret, editorSettings::setAnimatedCaret);

        ComboBox<EditorSettings.CaretEasing> caretEasingBox = ComboBox.create(Arrays.asList(EditorSettings.CaretEasing.values()));
        caretEasingBox.setTextRenderer(value -> switch (value) {
            case NINJA -> LocalizeValue.localizeTODO("Ninja");
            case EASE -> LocalizeValue.localizeTODO("Ease");
        });
        propertyBuilder.add(caretEasingBox, editorSettings::getCaretEasing, editorSettings::setCaretEasing);
        caretEasingBox.setEnabled(editorSettings.isAnimatedCaret());
        animatedCaretBox.addValueListener(event -> caretEasingBox.setEnabled(event.getValue()));
        root.add(DockLayout.create().left(animatedCaretBox).right(caretEasingBox));

        CheckBox useBlockCaret = CheckBox.create(ApplicationLocalize.checkboxUseBlockCaret());
        propertyBuilder.add(useBlockCaret, editorSettings::isBlockCursor, editorSettings::setBlockCursor);
        root.add(useBlockCaret);

        CheckBox fullLineHeightCaret = CheckBox.create(LocalizeValue.localizeTODO("Use full line height caret"));
        propertyBuilder.add(fullLineHeightCaret, editorSettings::isFullLineHeightCursor, editorSettings::setFullLineHeightCursor);
        root.add(fullLineHeightCaret);

        CheckBox showRightMargin = CheckBox.create(ApplicationLocalize.checkboxRightMargin());
        propertyBuilder.add(showRightMargin, editorSettings::isRightMarginShown, editorSettings::setRightMarginShown);
        root.add(showRightMargin);

        CheckBox showLineNumbers = CheckBox.create(ApplicationLocalize.checkboxShowLineNumbers());
        propertyBuilder.add(showLineNumbers, editorSettings::isLineNumbersShown, editorSettings::setLineNumbersShown);
        root.add(showLineNumbers);

        CheckBox stickyLinesBox = CheckBox.create(LocalizeValue.localizeTODO("Show sticky lines"));
        propertyBuilder.add(stickyLinesBox, editorSettings::isStickyLineShown, editorSettings::setStickyLinesShown);

        IntBox stickyLimitBox = IntBox.create(5);
        propertyBuilder.add(stickyLimitBox, editorSettings::getStickyLinesLimit, editorSettings::setStickyLinesLimit);
        stickyLinesBox.addValueListener(event -> stickyLimitBox.setEnabled(event.getValue()));

        root.add(DockLayout.create().left(stickyLinesBox).right(stickyLimitBox));

        CheckBox showMethodSeparators = CheckBox.create(ApplicationLocalize.checkboxShowMethodSeparators());
        propertyBuilder.add(showMethodSeparators, codeEditorInternalHelper::isShowMethodSeparators, codeEditorInternalHelper::setShowMethodSeparators);
        root.add(showMethodSeparators);

        CheckBox showWhitespaces = CheckBox.create(ApplicationLocalize.checkboxShowWhitespaces());
        propertyBuilder.add(showWhitespaces, editorSettings::isWhitespacesShown, editorSettings::setWhitespacesShown);
        root.add(showWhitespaces);

        CheckBox showSelectionWhitespaces = CheckBox.create(LocalizeValue.localizeTODO("Show whitespaces in selection"));
        propertyBuilder.add(showSelectionWhitespaces, editorSettings::isSelectionWhitespacesShown, editorSettings::setSelectionWhitespacesShown);
        showSelectionWhitespaces.setEnabled(editorSettings.isWhitespacesShown());
        showWhitespaces.addValueListener(event -> showSelectionWhitespaces.setEnabled(event.getValue()));
        root.add(showSelectionWhitespaces);

        CheckBox showSpecialChars = CheckBox.create(LocalizeValue.localizeTODO("Show special characters"));
        propertyBuilder.add(showSpecialChars, editorSettings::isShowingSpecialChars, editorSettings::setShowingSpecialChars);
        root.add(showSpecialChars);

        ComboBox<TabCharacterPaintMode> tabPaintModeBox = ComboBox.create(Arrays.asList(TabCharacterPaintMode.values()));
        tabPaintModeBox.setTextRenderer(value -> value == null ? LocalizeValue.empty() : value.getText());
        propertyBuilder.add(tabPaintModeBox, editorSettings::getTabCharacterPaintMode, editorSettings::setTabCharacterPaintMode);
        root.add(DockLayout.create().left(Label.create(LocalizeValue.localizeTODO("Tab character:"))).right(tabPaintModeBox));

        CheckBox showVerticalIndents = CheckBox.create(ApplicationLocalize.labelAppearanceShowVerticalIndentGuides());
        propertyBuilder.add(showVerticalIndents, editorSettings::isIndentGuidesShown, editorSettings::setIndentGuidesShown);
        root.add(showVerticalIndents);

        CheckBox highlightSelectionOccurrences = CheckBox.create(LocalizeValue.localizeTODO("Highlight occurrences of selected text"));
        propertyBuilder.add(highlightSelectionOccurrences, editorSettings::isHighlightSelectionOccurrences, editorSettings::setHighlightSelectionOccurrences);
        root.add(highlightSelectionOccurrences);

        List<AdditionalEditorAppearanceSettingProvider> providers = new ArrayList<>(myApplication.getExtensionList(AdditionalEditorAppearanceSettingProvider.class));
        providers.sort(APPEARANCE_SETTING_PROVIDER_LABEL_NAME_COMPARATOR);

        for (AdditionalEditorAppearanceSettingProvider provider : providers) {
            VerticalLayout childRoot = VerticalLayout.create();
            provider.fillProperties(propertyBuilder, childRoot::add);
            root.add(LabeledLayout.create(provider.getLabelName(), childRoot));
        }

        return root;
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        super.apply(component);

        EditorFactory.getInstance().refreshAllEditors();
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Appearance");
    }

    
    @Override
    public String getId() {
        return "editor.preferences.appearance";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }
}
