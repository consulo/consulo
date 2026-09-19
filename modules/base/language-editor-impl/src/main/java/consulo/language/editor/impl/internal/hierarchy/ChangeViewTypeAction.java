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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.hierarchy.HierarchyViewType;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import org.jspecify.annotations.Nullable;

/**
 * Switches the open hierarchy to one of the views its model offers. The same class backs both the toolbar
 * toggles, which are built per view the model names, and the registered actions a keymap or a popup group
 * can reach.
 *
 * @author cdr
 */
public class ChangeViewTypeAction extends ToggleAction {
    private final HierarchyViewType myViewType;

    public ChangeViewTypeAction(HierarchyViewType viewType) {
        super(viewType.getPresentableName(), viewType.getDescription(), viewType.getIcon());
        myViewType = viewType;
    }

    @Override
    public final boolean isSelected(AnActionEvent event) {
        HierarchyBrowser browser = getBrowser(event.getDataContext());
        return browser != null && myViewType.equals(browser.getCurrentViewType());
    }

    @Override
    @RequiredUIAccess
    public final void setSelected(AnActionEvent event, boolean flag) {
        if (!flag) {
            return;
        }

        HierarchyBrowser browser = getBrowser(event.getDataContext());
        Application.get().invokeLater(() -> {
            if (browser != null) {
                browser.changeViewType(myViewType);
            }
        });
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        super.update(event);
        HierarchyBrowser browser = getBrowser(event.getDataContext());
        event.getPresentation().setEnabled(browser != null && ReadAction.compute(() -> browser.isViewTypeEnabled(myViewType)));
    }

    protected static @Nullable HierarchyBrowser getBrowser(DataContext context) {
        return context.getData(HierarchyBrowser.KEY);
    }
}
