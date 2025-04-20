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
package consulo.compiler.artifact.ui.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.ChooseElementsDialog;
import consulo.project.Project;
import consulo.compiler.artifact.Artifact;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class ChooseArtifactsDialog extends ChooseElementsDialog<Artifact> {
    public ChooseArtifactsDialog(
        Project project,
        List<? extends Artifact> items,
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue description
    ) {
        super(project, items, title, description, true);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ChooseArtifactsDialog(Project project, List<? extends Artifact> items, String title, String description) {
        super(project, items, title, description, true);
    }

    @Override
    protected String getItemText(Artifact item) {
        return item.getName();
    }

    @Override
    protected Image getItemIcon(Artifact item) {
        return item.getArtifactType().getIcon();
    }
}