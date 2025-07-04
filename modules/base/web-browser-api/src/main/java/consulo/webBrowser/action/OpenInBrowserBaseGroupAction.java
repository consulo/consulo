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

import consulo.component.util.ModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.*;
import consulo.webBrowser.WebBrowser;
import consulo.webBrowser.WebBrowserManager;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class OpenInBrowserBaseGroupAction extends ComputableActionGroup {
    private OpenFileInDefaultBrowserAction myDefaultBrowserAction;

    protected OpenInBrowserBaseGroupAction(boolean popup) {
        super(
            WebBrowserLocalize.actionOpenInBrowserActionGroupText(),
            WebBrowserLocalize.actionOpenInBrowserActionGroupDescription(),
            PlatformIconGroup.nodesPpweb()
        );

        setPopup(popup);
    }

    @Nonnull
    @Override
    protected ModificationTracker getModificationTracker() {
        return WebBrowserManager.getInstance();
    }

    @Nonnull
    @Override
    protected AnAction[] computeChildren(@Nonnull ActionManager manager) {
        List<WebBrowser> browsers = WebBrowserManager.getInstance().getBrowsers();
        boolean addDefaultBrowser = isPopup();
        int offset = addDefaultBrowser ? 1 : 0;
        AnAction[] actions = new AnAction[browsers.size() + offset];

        if (addDefaultBrowser) {
            if (myDefaultBrowserAction == null) {
                myDefaultBrowserAction = new OpenFileInDefaultBrowserAction();
                myDefaultBrowserAction.getTemplatePresentation().setTextValue(LocalizeValue.localizeTODO("Default"));
                myDefaultBrowserAction.getTemplatePresentation().setIcon(PlatformIconGroup.nodesPpweb());
            }
            actions[0] = myDefaultBrowserAction;
        }

        for (int i = 0, size = browsers.size(); i < size; i++) {
            actions[i + offset] = new BaseWebBrowserAction(browsers.get(i));
        }
        return actions;
    }


    public static final class OpenInBrowserGroupAction extends OpenInBrowserBaseGroupAction {
        public OpenInBrowserGroupAction() {
            super(true);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            String place = e.getPlace();

            if (ActionPlaces.PROJECT_VIEW_POPUP.equals(place)) {
                e.getPresentation().setTextValue(WebBrowserLocalize.actionOpenInBrowserActionGroupShortText());
            }
            else {
                e.getPresentation().setTextValue(WebBrowserLocalize.actionOpenInBrowserActionGroupText());
            }
        }
    }

    public static final class OpenInBrowserEditorContextBarGroupAction extends OpenInBrowserBaseGroupAction {
        public OpenInBrowserEditorContextBarGroupAction() {
            super(false);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setVisible(!WebBrowserManager.getInstance().getBrowsers().isEmpty());
        }
    }
}