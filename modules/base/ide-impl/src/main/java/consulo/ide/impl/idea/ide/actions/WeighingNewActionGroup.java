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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.action.CreateFileAction;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ActionImpl(id = "WeighingNewGroup")
public class WeighingNewActionGroup extends WeighingActionGroup {
    private ActionGroup myDelegate;

    @Override
    protected ActionGroup getDelegate() {
        if (myDelegate == null) {
            myDelegate = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_NEW);
            getTemplatePresentation().setTextValue(myDelegate.getTemplatePresentation().getTextValue());
            setPopup(myDelegate.isPopup());
        }
        return myDelegate;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setTextValue(getTemplatePresentation().getTextValue());
    }

    @Override
    protected boolean shouldBeChosenAnyway(AnAction action) {
        Class<? extends AnAction> aClass = action.getClass();
        return aClass == CreateFileAction.class
            || aClass == CreateDirectoryOrPackageAction.class
            || "NewModuleInGroupAction".equals(aClass.getSimpleName()); //todo why is it in idea module?
    }
}
