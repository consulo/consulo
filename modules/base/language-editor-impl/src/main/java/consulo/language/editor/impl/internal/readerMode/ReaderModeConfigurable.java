/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.EditorColorsManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.language.editor.readerMode.ReaderModeProvider.ReaderMode;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author VISTALL
 */
@ExtensionImpl
public class ReaderModeConfigurable extends SimpleConfigurableByProperties implements ProjectConfigurable {
    private final Project myProject;

    @Inject
    public ReaderModeConfigurable(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        ReaderModeSettings settings = ReaderModeSettings.getInstance(myProject);

        VerticalLayout root = VerticalLayout.create();

        CheckBox enabledBox = CheckBox.create(LocalizeValue.localizeTODO("Enable Reader mode"));
        propertyBuilder.add(enabledBox, settings::isEnabled, settings::setEnabled);
        root.add(enabledBox);

        ComboBox<ReaderMode> modeBox = ComboBox.create(Arrays.asList(ReaderMode.values()));
        modeBox.setTextRenderer(value -> switch (value) {
            case LIBRARIES -> LocalizeValue.localizeTODO("Library files");
            case READ_ONLY -> LocalizeValue.localizeTODO("Read-only files");
            case LIBRARIES_AND_READ_ONLY -> LocalizeValue.localizeTODO("Library and read-only files");
        });
        propertyBuilder.add(modeBox, settings::getMode, settings::setMode);
        modeBox.setEnabled(settings.isEnabled());
        enabledBox.addValueListener(event -> modeBox.setEnabled(event.getValue()));
        root.add(DockLayout.create().left(Label.create(LocalizeValue.localizeTODO("Apply to:"))).right(modeBox));

        boolean renderedDocs = EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled();
        CheckBox renderedDocsBox = CheckBox.create(LocalizeValue.localizeTODO("Render documentation comments"));
        propertyBuilder.add(renderedDocsBox, settings::isShowRenderedDocs, settings::setShowRenderedDocs);
        renderedDocsBox.setEnabled(settings.isEnabled() && !renderedDocs);
        enabledBox.addValueListener(event -> renderedDocsBox.setEnabled(event.getValue() && !renderedDocs));
        root.add(renderedDocsBox);

        CheckBox showWarningsBox = CheckBox.create(LocalizeValue.localizeTODO("Show warnings and errors"));
        propertyBuilder.add(showWarningsBox, settings::isShowWarnings, settings::setShowWarnings);
        showWarningsBox.setEnabled(settings.isEnabled());
        enabledBox.addValueListener(event -> showWarningsBox.setEnabled(event.getValue()));
        root.add(showWarningsBox);

        CheckBox showInlaysHintsBox = CheckBox.create(LocalizeValue.localizeTODO("Show inlay hints"));
        propertyBuilder.add(showInlaysHintsBox, settings::isShowInlaysHints, settings::setShowInlaysHints);
        showInlaysHintsBox.setEnabled(settings.isEnabled());
        enabledBox.addValueListener(event -> showInlaysHintsBox.setEnabled(event.getValue()));
        root.add(showInlaysHintsBox);

        boolean useLigatures = EditorColorsManager.getInstance().getGlobalScheme().getFontPreferences().useLigatures();
        CheckBox showLigaturesBox = CheckBox.create(LocalizeValue.localizeTODO("Show ligatures"));
        propertyBuilder.add(showLigaturesBox, settings::isShowLigatures, settings::setShowLigatures);
        showLigaturesBox.setEnabled(settings.isEnabled() && !useLigatures);
        enabledBox.addValueListener(event -> showLigaturesBox.setEnabled(event.getValue() && !useLigatures));
        root.add(showLigaturesBox);

        CheckBox increaseLineSpacingBox = CheckBox.create(LocalizeValue.localizeTODO("Increase line spacing"));
        propertyBuilder.add(increaseLineSpacingBox, settings::isIncreaseLineSpacing, settings::setIncreaseLineSpacing);
        increaseLineSpacingBox.setEnabled(settings.isEnabled());
        enabledBox.addValueListener(event -> increaseLineSpacingBox.setEnabled(event.getValue()));
        root.add(increaseLineSpacingBox);

        CheckBox enableVisualFormattingBox = CheckBox.create(LocalizeValue.localizeTODO("Visual formatting layer"));
        propertyBuilder.add(enableVisualFormattingBox, settings::isEnableVisualFormatting, settings::setEnableVisualFormatting);
        enableVisualFormattingBox.setEnabled(settings.isEnabled());
        enabledBox.addValueListener(event -> enableVisualFormattingBox.setEnabled(event.getValue()));
        root.add(enableVisualFormattingBox);

        return root;
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        super.apply(component);

        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getProject() != myProject) {
                continue;
            }
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            ReaderModeSettings.applyReaderMode(myProject, editor, file, true, true);
        }
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Reader Mode");
    }

    @Override
    public String getId() {
        return "editor.preferences.readerMode";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }
}
