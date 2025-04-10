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
package consulo.externalSystem.internal;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.popup.AWTPopupChooserBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2025-04-10
 */
@ServiceAPI(ComponentScope.APPLICATION)
@Deprecated
@DeprecationInfo("Remove it!")
public interface ExternalSystemInternalAWTHelper {
    <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTree tree);

    @RequiredUIAccess
    void executeImportAction(@Nonnull Project project, @Nullable FileChooserDescriptor descriptor);
}
