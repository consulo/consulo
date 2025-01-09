/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.plugins.whatsNew;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.ide.impl.updateSettings.impl.UpdateHistory;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@ExtensionImpl
public class WhatsNewConfigurationFileEditorProvider implements ConfigurationFileEditorProvider {
    private final Application myApplication;
    private final Provider<UpdateHistory> myUpdateHistoryProvider;

    @Inject
    public WhatsNewConfigurationFileEditorProvider(Application application, Provider<UpdateHistory> updateHistoryProvider) {
        myApplication = application;
        myUpdateHistoryProvider = updateHistoryProvider;
    }

    @Nullable
    @Override
    public ColorValue getColor() {
        return TargetAWT.from(LightColors.BLUE);
    }

    @Nonnull
    @Override
    public String getId() {
        return "whats_new";
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return myApplication.getIcon();
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return IdeLocalize.whatsnewActionCustomText(myApplication.getName()).map(Presentation.NO_MNEMONIC);
    }

    @Nonnull
    @Override
    public ConfigurationFileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
        return new WhatsNewVirtualFileEditor(project, myUpdateHistoryProvider.get(), file);
    }
}
