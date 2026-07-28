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
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.setting.AdditionalEditorAppearanceSettingProvider;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;

import java.util.function.Consumer;

/**
 * Exposes the global "render documentation comments" setting in Editor | Appearance.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class DocRenderAppearanceSettingProvider implements AdditionalEditorAppearanceSettingProvider {
    @Override
    public LocalizeValue getLabelName() {
        return LocalizeValue.localizeTODO("Documentation");
    }

    @Override
    public void fillProperties(SimpleConfigurableByProperties.PropertyBuilder builder, Consumer<Component> layoutBuilder) {
        EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();

        CheckBox renderedDocBox = CheckBox.create(LocalizeValue.localizeTODO("Render documentation comments"));
        builder.add(renderedDocBox, editorSettings::isDocCommentRenderingEnabled, value -> {
            editorSettings.setDocCommentRenderingEnabled(value);
            resetAllEditorsToDefaultState();
        });
        layoutBuilder.accept(renderedDocBox);
    }

    /**
     * Sets all doc comments to their default state (rendered or not rendered) for all opened editors.
     */
    private static void resetAllEditorsToDefaultState() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            DocRenderManager.resetEditorToDefaultState(editor);
        }
    }
}
