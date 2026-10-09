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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNavigationSupport;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DocumentationEditSourceAction extends DumbAwareAction implements AnActionWithSyncUpdate, HintManager.ActionToIgnore {
    private final Supplier<@Nullable DocumentationBrowser> myBrowser;
    private final Runnable myAfterNavigate;

    DocumentationEditSourceAction(Supplier<@Nullable DocumentationBrowser> browser, Runnable afterNavigate) {
        super(CodeInsightLocalize.javadocActionEditSource(), LocalizeValue.empty(), PlatformIconGroup.actionsEditsource());
        myBrowser = browser;
        myAfterNavigate = afterNavigate;
    }

    @Override
    public void update(AnActionEvent e) {
        DocumentationBrowser browser = myBrowser.get();
        DocumentationPage page = browser == null ? null : browser.getPage();
        e.getPresentation().setEnabled(page != null && page.editableSource());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DocumentationBrowser browser = myBrowser.get();
        DocumentationPage page = browser == null ? null : browser.getPage();
        SmartPsiElementPointer<? extends PsiElement> element = page == null ? null : page.element();
        if (element == null) {
            return;
        }

        CoroutineScope.launchAsync(
            browser.getProject().coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, Optional<Navigatable>>apply(ignored -> Optional.ofNullable(findNavigatable(element))))
                .then(UIAction.<Optional<Navigatable>, Optional<Navigatable>>apply(navigatable -> {
                    navigatable.ifPresent(it -> {
                        it.navigate(true);
                        myAfterNavigate.run();
                    });
                    return navigatable;
                }))
        );
    }

    @RequiredReadAction
    private static @Nullable Navigatable findNavigatable(SmartPsiElementPointer<? extends PsiElement> pointer) {
        PsiElement element = pointer.getElement();
        if (element == null || !element.isValid()) {
            return null;
        }

        Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(element);
        if (descriptor != null) {
            return descriptor;
        }
        return element instanceof Navigatable navigatable ? navigatable : null;
    }
}
