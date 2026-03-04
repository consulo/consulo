// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.documentation;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.codeInsight.navigation.DocPreviewUtil;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiQualifiedNamedElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.TimeUnit;

/**
 * @author gregsh
 */
public class QuickDocUtil {
    /**
     * Repeatedly tries to run given task in read action without blocking write actions (for this to work effectively the action should invoke
     * {@link ProgressManager#checkCanceled()} or {@link ProgressIndicator#checkCanceled()} often enough).
     *
     * @param action              task to run
     * @param timeout             timeout in milliseconds
     * @param pauseBetweenRetries pause between retries in milliseconds
     * @param progressIndicator   optional progress indicator, which can be used to cancel the action externally
     * @return {@code true} if the action succeeded to run without interruptions, {@code false} otherwise
     */
    public static boolean runInReadActionWithWriteActionPriorityWithRetries(
        @Nonnull Runnable action,
        long timeout,
        long pauseBetweenRetries,
        @Nullable ProgressIndicator progressIndicator
    ) {
        Application application = Application.get();
        long deadline = System.currentTimeMillis() + timeout;
        boolean result;
        while (!(result = tryReadAction(application, action))
            && (progressIndicator == null || !progressIndicator.isCanceled())
            && System.currentTimeMillis() < deadline) {
            try {
                TimeUnit.MILLISECONDS.sleep(pauseBetweenRetries);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return result;
    }

    /**
     * Same as {@link #runInReadActionWithWriteActionPriorityWithRetries(Runnable, long, long, ProgressIndicator)} using current thread's
     * progress indicator ({@link ProgressIndicatorProvider#getGlobalProgressIndicator()}).
     */
    public static boolean runInReadActionWithWriteActionPriorityWithRetries(
        @Nonnull Runnable action,
        long timeout,
        long pauseBetweenRetries
    ) {
        return runInReadActionWithWriteActionPriorityWithRetries(
            action,
            timeout,
            pauseBetweenRetries,
            ProgressIndicatorProvider.getGlobalProgressIndicator()
        );
    }

    private static boolean tryReadAction(@Nonnull Application application, @Nonnull Runnable action) {
        try {
            return application.tryRunReadAction(action);
        }
        catch (ProcessCanceledException e) {
            return false;
        }
    }

    @Contract("_, _, _, null -> null")
    public static String inferLinkFromFullDocumentation(
        @Nonnull DocumentationProvider provider,
        PsiElement element,
        PsiElement originalElement,
        @Nullable String navigationInfo
    ) {
        if (navigationInfo != null) {
            String fqn =
                element instanceof PsiQualifiedNamedElement qualifiedNamedElement ? qualifiedNamedElement.getQualifiedName() : null;
            String fullText = provider.generateDoc(element, originalElement);
            return HintUtil.prepareHintText(DocPreviewUtil.buildPreview(navigationInfo, fqn, fullText), HintUtil.getInformationHint());
        }
        return null;
    }
}
