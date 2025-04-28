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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
public final class ViewClassHierarchyAction extends ChangeViewTypeActionBase {
    public ViewClassHierarchyAction() {
        super(
            IdeLocalize.actionViewClassHierarchy(),
            IdeLocalize.actionDescriptionViewClassHierarchy(),
            PlatformIconGroup.hierarchyClasshierarchy()
        );
    }

    @Override
    protected final String getTypeName() {
        return TypeHierarchyBrowserBase.TYPE_HIERARCHY_TYPE;
    }

    @Override
    @RequiredUIAccess
    public final void update(@Nonnull AnActionEvent event) {
        super.update(event);
        TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
        event.getPresentation().setEnabled(browser != null && !browser.isInterface());
    }
}
