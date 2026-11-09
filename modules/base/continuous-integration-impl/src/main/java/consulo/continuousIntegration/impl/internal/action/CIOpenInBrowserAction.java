/*
 * Copyright 2013-2026 consulo.io
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
package consulo.continuousIntegration.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIDataKeys;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

/**
 * Opens the selected build, job or server, whichever is the most specific one with a page.
 *
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.OpenInBrowser")
public class CIOpenInBrowserAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CIOpenInBrowserAction() {
        super(
            ContinuousIntegrationImplLocalize.actionOpenInBrowserText(),
            ContinuousIntegrationImplLocalize.actionOpenInBrowserDescription(),
            PlatformIconGroup.generalWeb()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        String url = getUrl(e);
        if (url != null) {
            BrowserUtil.browse(url);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(getUrl(e) != null);
    }

    private static @Nullable String getUrl(AnActionEvent e) {
        CIBuild build = e.getData(CIDataKeys.BUILD);
        if (build != null && build.webUrl() != null) {
            return build.webUrl();
        }
        CIJob job = e.getData(CIDataKeys.JOB);
        if (job != null && job.webUrl() != null) {
            return job.webUrl();
        }
        CIServer server = e.getData(CIDataKeys.SERVER);
        return server == null ? null : server.getWebUrl();
    }
}
