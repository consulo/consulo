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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class ProjectStructureElementConfigurable<T> extends NamedConfigurable<T> {
    protected ProjectStructureElementConfigurable() {
    }

    protected ProjectStructureElementConfigurable(boolean isNameEditable, @Nullable Runnable updateTree) {
        super(isNameEditable, updateTree);
    }

    @Nullable
    public abstract ProjectStructureElement getProjectStructureElement();

    @Override
    protected void setBorder() {
        myNamePanel.setBorder(JBUI.Borders.empty(0, 10, 6, 10));
    }

    @Override
    @Nullable
    public Image getIcon(boolean open) {
        return null;
    }
}
