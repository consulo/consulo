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
package consulo.ui.ex.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@ActionImpl(id = IdeActions.ACTION_CONTEXT_HELP)
public class ContextHelpAction extends AnAction implements DumbAware {
    private final String myHelpID;

    @Inject
    public ContextHelpAction() {
        this(null);
    }

    public ContextHelpAction(@Nullable String helpID) {
        super(ActionLocalize.actionContexthelpText(), ActionLocalize.actionContexthelpDescription());
        myHelpID = helpID;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        String helpId = getHelpId(dataContext);
        if (helpId != null) {
            HelpManager.getInstance().invokeHelp(helpId);
        }
    }

    @Nullable
    protected String getHelpId(DataContext dataContext) {
        return myHelpID != null ? myHelpID : dataContext.getData(HelpManager.HELP_ID);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();

        if (ActionPlaces.MAIN_MENU.equals(event.getPlace())) {
            DataContext dataContext = event.getDataContext();
            presentation.setEnabled(getHelpId(dataContext) != null);
        }
        else {
            presentation.setIcon(PlatformIconGroup.actionsHelp());
            presentation.setTextValue(CommonLocalize.buttonHelp());
        }
    }
}
