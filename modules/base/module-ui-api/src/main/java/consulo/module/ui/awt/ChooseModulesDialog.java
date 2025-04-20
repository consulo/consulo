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
package consulo.module.ui.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;import consulo.ui.ex.awt.ChooseElementsDialog;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author Anna.Kozlova
 * @since 2006-08-14
 */
public class ChooseModulesDialog extends ChooseElementsDialog<Module> {
    public ChooseModulesDialog(Component parent, List<Module> items, @Nonnull LocalizeValue title) {
        super(parent, items, title, LocalizeValue.empty(), true);
    }

    public ChooseModulesDialog(Component parent, List<Module> items, @Nonnull LocalizeValue title, @Nonnull LocalizeValue description) {
        super(parent, items, title, description, true);
    }

    public ChooseModulesDialog(
        Project project,
        List<? extends Module> items,
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue description
    ) {
        super(project, items, title, description, true);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ChooseModulesDialog(Component parent, List<Module> items, String title) {
        super(parent, items, title, null, true);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ChooseModulesDialog(Component parent, List<Module> items, String title, @Nullable String description) {
        super(parent, items, title, description, true);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ChooseModulesDialog(Project project, List<? extends Module> items, String title, String description) {
        super(project, items, title, description, true);
    }

    public void setSingleSelectionMode() {
        myChooser.setSingleSelectionMode();
    }

    @Override
    protected Image getItemIcon(Module item) {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    protected String getItemText(Module item) {
        return item.getName();
    }
}
