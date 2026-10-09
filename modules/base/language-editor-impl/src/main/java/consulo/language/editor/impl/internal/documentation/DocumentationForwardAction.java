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
package consulo.language.editor.impl.internal.documentation;

import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DocumentationForwardAction extends DumbAwareAction implements AnActionWithSyncUpdate, HintManager.ActionToIgnore {
    private final Supplier<@Nullable DocumentationBrowser> myBrowser;

    DocumentationForwardAction(Supplier<@Nullable DocumentationBrowser> browser) {
        super(CodeInsightLocalize.javadocActionForward(), LocalizeValue.empty(), PlatformIconGroup.actionsForward());
        myBrowser = browser;
    }

    @Override
    public void update(AnActionEvent e) {
        DocumentationBrowser browser = myBrowser.get();
        e.getPresentation().setEnabled(browser != null && browser.canGoForward());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DocumentationBrowser browser = myBrowser.get();
        if (browser != null) {
            browser.goForward();
        }
    }
}
