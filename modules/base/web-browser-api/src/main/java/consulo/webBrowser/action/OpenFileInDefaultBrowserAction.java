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

import consulo.application.ReadAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class OpenFileInDefaultBrowserAction extends DumbAwareAction {
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Pair<OpenInBrowserRequest, WebBrowserUrlProvider> result = ReadAction.compute(() -> BaseOpenInBrowserAction.doUpdate(e));
        if (result == null) {
            return;
        }

        WebBrowserUrlProvider browserUrlProvider = result.second;
        String text = getTemplatePresentation().getText();
        String description = getTemplatePresentation().getDescription();
        if (browserUrlProvider != null) {
            String customDescription = browserUrlProvider.getOpenInBrowserActionDescription(result.first.getFile());
            if (customDescription != null) {
                description = customDescription;
            }
            if (WebFileFilter.isFileAllowed(result.first.getFile())) {
                description += " (hold Shift to open URL of local file)";
            }
        }

        presentation.setText(text);
        presentation.setDescription(description);

        WebBrowser browser = findUsingBrowser();
        if (browser != null) {
            presentation.setIcon(browser.getIcon());
        }

        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            presentation.setVisible(presentation.isEnabled());
        }
    }

    @Nullable
    private static WebBrowser findUsingBrowser() {
        WebBrowserManager browserManager = WebBrowserManager.getInstance();
        if (browserManager.getDefaultBrowserPolicy() == DefaultBrowserPolicy.FIRST) {
            return browserManager.getFirstActiveBrowser();
        }
        else if (browserManager.getDefaultBrowserPolicy() == DefaultBrowserPolicy.ALTERNATIVE) {
            String path = WebBrowserManager.getInstance().getAlternativeBrowserPath();
            if (!StringUtil.isEmpty(path)) {
                WebBrowser browser = browserManager.findBrowserById(path);
                if (browser == null) {
                    for (WebBrowser item : browserManager.getActiveBrowsers()) {
                        if (path.equals(item.getPath())) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        BaseOpenInBrowserAction.open(e, findUsingBrowser());
    }
}
