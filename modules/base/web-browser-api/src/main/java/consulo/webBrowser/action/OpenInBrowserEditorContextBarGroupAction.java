// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.webBrowser.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.platform.Platform;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.webBrowser.WebBrowserManager;

@ActionImpl(id = "OpenInBrowserEditorContextBarGroupAction")
public final class OpenInBrowserEditorContextBarGroupAction extends OpenInBrowserBaseGroupAction implements AnActionWithSyncUpdate {
    public OpenInBrowserEditorContextBarGroupAction() {
        super(false);
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        WebBrowserManager browserManager = WebBrowserManager.getInstance();
        boolean hasBrowsers = Platform.current().isInBrowser() || !browserManager.getActiveBrowsers().isEmpty();
        boolean enabled = browserManager.isShowBrowserHover()
            && hasBrowsers
            && editor != null
            && editor.getEditorKind() != EditorKind.DIFF;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
