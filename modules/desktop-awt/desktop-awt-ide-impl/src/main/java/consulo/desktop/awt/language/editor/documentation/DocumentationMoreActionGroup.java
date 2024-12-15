/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.language.editor.documentation;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.RightAlignedToolbarAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-15
 *
 * Used custom right menu - due need implement {@link HintManagerImpl.ActionToIgnore}
 */
public class DocumentationMoreActionGroup extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore, DumbAware, RightAlignedToolbarAction {
    public DocumentationMoreActionGroup() {
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsMorevertical();
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }
}
