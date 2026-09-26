/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.webBrowser.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.IdeActions;
import consulo.webBrowser.localize.WebBrowserLocalize;

@ActionImpl(
    id = "OpenInBrowserGroup",
    parents = {
        @ActionParentRef(
            value = @ActionRef(id = "ViewMenu"),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(id = IdeActions.ACTION_VIEW_SOURCE)
        ),
        @ActionParentRef(@ActionRef(id = "RevealGroup"))
    }
)
public final class OpenInBrowserGroupAction extends OpenInBrowserBaseGroupAction implements AnActionWithSyncUpdate {
    public OpenInBrowserGroupAction() {
        super(true);
    }

    @Override
    public void update(AnActionEvent e) {
        String place = e.getPlace();

        if (ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) {
            e.getPresentation().setText(WebBrowserLocalize.actionOpenInBrowserActionGroupShortText());
        }
        else {
            e.getPresentation().setText(WebBrowserLocalize.actionOpenInBrowserActionGroupText());
        }
    }
}
