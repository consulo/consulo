/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.ArtifactType;
import consulo.ui.ex.awt.ColoredListCellRenderer;

import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author nik
 */
public class ArtifactTypeCellRenderer extends ColoredListCellRenderer<ArtifactType> {
    @Override
    protected void customizeCellRenderer(
        @Nonnull JList<? extends ArtifactType> list,
        ArtifactType value,
        int index,
        boolean selected,
        boolean hasFocus
    ) {
        setIcon(value.getIcon());
        append(value.getPresentableName().get());
    }
}
