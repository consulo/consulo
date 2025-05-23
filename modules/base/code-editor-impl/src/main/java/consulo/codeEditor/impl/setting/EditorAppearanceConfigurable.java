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
package consulo.codeEditor.impl.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.setting.AdditionalEditorAppearanceSettingProvider;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-Jul-22
 */
@ExtensionImpl
public class EditorAppearanceConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    private final Application myApplication;
    private final Provider<PersistentEditorSettings> myEditorSettingsExternalizable;
    private final Provider<CodeEditorInternalHelper> myEditorInternalHelper;

    @Inject
    public EditorAppearanceConfigurable(Application application, Provider<PersistentEditorSettings> editorSettingsExternalizable, Provider<CodeEditorInternalHelper> editorInternalHelper) {
        myApplication = application;
        myEditorSettingsExternalizable = editorSettingsExternalizable;
        myEditorInternalHelper = editorInternalHelper;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createLayout(@Nonnull PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
        PersistentEditorSettings editorSettings = myEditorSettingsExternalizable.get();
        CodeEditorInternalHelper codeEditorInternalHelper = myEditorInternalHelper.get();

        VerticalLayout root = VerticalLayout.create();

        CheckBox caretBlinkingBox = CheckBox.create(ApplicationLocalize.checkboxCaretBlinkingMs());
        propertyBuilder.add(caretBlinkingBox, editorSettings::isBlinkCaret, editorSettings::setBlinkCaret);

        IntBox caretBlinkingTimeBox = IntBox.create();
        caretBlinkingBox.addValueListener(event -> caretBlinkingTimeBox.setEnabled(event.getValue()));
        propertyBuilder.add(caretBlinkingTimeBox, editorSettings::getBlinkPeriod, editorSettings::setBlinkPeriod);
        root.add(DockLayout.create().left(caretBlinkingBox).right(caretBlinkingTimeBox));

        CheckBox useBlockCaret = CheckBox.create(ApplicationLocalize.checkboxUseBlockCaret());
        propertyBuilder.add(useBlockCaret, editorSettings::isBlockCursor, editorSettings::setBlockCursor);
        root.add(useBlockCaret);

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

        CheckBox showVerticalIndents = CheckBox.create(ApplicationLocalize.labelAppearanceShowVerticalIndentGuides());
        propertyBuilder.add(showVerticalIndents, editorSettings::isIndentGuidesShown, editorSettings::setIndentGuidesShown);
        root.add(showVerticalIndents);

        CheckBox showParameterHints = CheckBox.create(ApplicationLocalize.checkboxShowParameterNameHints());
        boolean hasAnyInlayExtensions = codeEditorInternalHelper.hasAnyInlayExtensions();
        showParameterHints.setVisible(hasAnyInlayExtensions);
        propertyBuilder.add(showParameterHints, editorSettings::isShowParameterNameHints, editorSettings::setShowParameterNameHints);

        Button configureButton = Button.create(LocalizeValue.localizeTODO("Configure..."));
        configureButton.setVisible(hasAnyInlayExtensions);
        configureButton.addClickListener(event -> myEditorInternalHelper.get().showParametersHitOptions());
        root.add(DockLayout.create().left(showParameterHints).right(configureButton));

        List<AdditionalEditorAppearanceSettingProvider> providers = new ArrayList<>(myApplication.getExtensionList(AdditionalEditorAppearanceSettingProvider.class));
        providers.sort((o1, o2) -> o1.getLabelName().compareIgnoreCase(o2.getLabelName()));

        for (AdditionalEditorAppearanceSettingProvider provider : providers) {
            VerticalLayout childRoot = VerticalLayout.create();
            provider.fillProperties(propertyBuilder, childRoot::add);
            root.add(LabeledLayout.create(provider.getLabelName(), childRoot));
        }

        return root;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Appearance";
    }

    @Nonnull
    @Override
    public String getId() {
        return "editor.preferences.appearance";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }
}
