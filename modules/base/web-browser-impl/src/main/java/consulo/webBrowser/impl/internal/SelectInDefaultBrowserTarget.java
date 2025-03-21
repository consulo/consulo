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
package consulo.webBrowser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import consulo.webBrowser.OpenInBrowserRequest;
import consulo.webBrowser.WebBrowserService;
import consulo.webBrowser.WebBrowserUrlProvider;
import consulo.webBrowser.WebFileFilter;
import consulo.webBrowser.action.BaseOpenInBrowserAction;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public final class SelectInDefaultBrowserTarget implements SelectInTarget {
    private static final Logger LOG = Logger.getInstance(SelectInDefaultBrowserTarget.class);

    @Override
    public boolean canSelect(SelectInContext context) {
        Object selectorInFile = context.getSelectorInFile();
        OpenInBrowserRequest request = selectorInFile instanceof PsiElement ? OpenInBrowserRequest.create((PsiElement) selectorInFile) : null;
        if (request == null) {
            return false;
        }

        WebBrowserUrlProvider urlProvider = WebBrowserService.getInstance().getProvider(request);
        if (urlProvider == null) {
            VirtualFile virtualFile = request.getVirtualFile();
            return virtualFile instanceof HttpVirtualFile || (WebFileFilter.isFileAllowed(request.getFile()) && !(virtualFile instanceof LightVirtualFileBase));
        }
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionText() {
        return WebBrowserLocalize.browserSelectInDefaultName();
    }

    @Override
    public void selectIn(SelectInContext context, boolean requestFocus) {
        PsiElement element = (PsiElement) context.getSelectorInFile();
        LOG.assertTrue(element != null);
        BaseOpenInBrowserAction.open(OpenInBrowserRequest.create(element), false, null);
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.OS_FILE_MANAGER;
    }
}
