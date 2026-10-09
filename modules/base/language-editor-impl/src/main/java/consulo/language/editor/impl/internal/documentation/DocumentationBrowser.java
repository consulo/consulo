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
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.documentation.CompositeDocumentationProvider;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationHandler;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationBrowser implements Disposable {
    private static final Logger LOG = Logger.getInstance(DocumentationBrowser.class);

    private static final long CANCELLATION_CHECK_MILLISECONDS = 50;

    private final Project myProject;

    private final Deque<DocumentationPage> myBackStack = new ArrayDeque<>();
    private final Deque<DocumentationPage> myForwardStack = new ArrayDeque<>();

    private final List<Consumer<DocumentationPage>> myPageListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> myFragmentListeners = new CopyOnWriteArrayList<>();

    private volatile @Nullable DocumentationPage myPage;
    private volatile boolean myCanGoBack;
    private volatile boolean myCanGoForward;

    private @Nullable Disposable myLoading;

    public DocumentationBrowser(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    public @Nullable DocumentationPage getPage() {
        return myPage;
    }

    public boolean canGoBack() {
        return myCanGoBack;
    }

    public boolean canGoForward() {
        return myCanGoForward;
    }

    public Disposable addPageListener(Consumer<DocumentationPage> listener) {
        myPageListeners.add(listener);
        return () -> myPageListeners.remove(listener);
    }

    public Disposable addFragmentListener(Consumer<String> listener) {
        myFragmentListeners.add(listener);
        return () -> myFragmentListeners.remove(listener);
    }

    @RequiredUIAccess
    public void reset(DocumentationSource source) {
        load(source, true);
    }

    @RequiredUIAccess
    public void showPage(DocumentationPage page) {
        cancelLoading();
        myBackStack.clear();
        myForwardStack.clear();
        setPage(page);
    }

    @RequiredUIAccess
    public void navigate(String href, DataContext dataContext) {
        if (href.startsWith("#")) {
            String fragment = href.substring(1);
            for (Consumer<String> listener : myFragmentListeners) {
                listener.accept(fragment);
            }
            return;
        }

        DocumentationPage page = myPage;
        SmartPsiElementPointer<? extends PsiElement> element = page == null ? null : page.element();

        if (DocumentationLinks.EXTERNAL_DOC.equals(href)) {
            if (element != null) {
                ExternalDocumentationOpener.open(myProject, element, null, page.externalUrl(), dataContext);
            }
            return;
        }

        if (element == null) {
            if (BrowserUtil.isAbsoluteURL(href)) {
                BrowserUtil.browse(href);
            }
            return;
        }

        load(new DocumentationLinkSource(element, href), false);
    }

    @RequiredUIAccess
    public void goBack() {
        DocumentationPage current = myPage;
        if (myBackStack.isEmpty() || current == null) {
            return;
        }

        cancelLoading();
        myForwardStack.push(current);
        setPage(myBackStack.pop());
    }

    @RequiredUIAccess
    public void goForward() {
        DocumentationPage current = myPage;
        if (myForwardStack.isEmpty() || current == null) {
            return;
        }

        cancelLoading();
        myBackStack.push(current);
        setPage(myForwardStack.pop());
    }

    @RequiredUIAccess
    private void load(DocumentationSource source, boolean resetHistory) {
        cancelLoading();

        Disposable loading = Disposable.newDisposable("DocumentationBrowser");
        Disposer.register(this, loading);
        myLoading = loading;

        DisposableCoroutineScope.launchAsync(
            myProject.coroutineContext(),
            loading,
            () -> Coroutine
                .first(ReadLock.<Object, DocumentationJob>apply(ignored -> prepare(source)))
                .then(CodeExecution.<DocumentationJob, DocumentationJob>apply(this::generate))
                .then(ReadLock.<DocumentationJob, DocumentationJob>apply(DocumentationBrowser::buildPage))
                .then(UIAction.<DocumentationJob, DocumentationJob>apply(job -> {
                    if (myLoading == loading) {
                        myLoading = null;
                        apply(job, resetHistory);
                        Disposer.dispose(loading);
                    }
                    return job;
                }))
        );
    }

    @RequiredUIAccess
    private void cancelLoading() {
        Disposable loading = myLoading;
        if (loading != null) {
            myLoading = null;
            Disposer.dispose(loading);
        }
    }

    @RequiredReadAction
    private DocumentationJob prepare(DocumentationSource source) {
        if (source instanceof DocumentationElementSource elementSource) {
            return prepareElement(elementSource);
        }
        if (source instanceof DocumentationLinkSource linkSource) {
            return prepareLink(linkSource);
        }
        return DocumentationJob.nothing();
    }

    @RequiredReadAction
    private DocumentationJob prepareElement(DocumentationElementSource source) {
        PsiElement element = source.element().getElement();
        if (element == null || !element.isValid()) {
            return DocumentationJob.message(CodeInsightLocalize.noDocumentationFound().get());
        }

        SmartPsiElementPointer<? extends PsiElement> originalPointer = source.originalElement();
        PsiElement originalElement = originalPointer == null ? null : originalPointer.getElement();
        if (originalElement != null) {
            DocumentationManagerHelper.storeOriginalElement(myProject, originalElement, element);
        }

        ElementDocumentationCollector collector =
            new ElementDocumentationCollector(myProject, element, originalElement, source.anchor(), false);
        return DocumentationJob.collect(collector, source.documentation());
    }

    @RequiredReadAction
    private DocumentationJob prepareLink(DocumentationLinkSource source) {
        PsiElement context = source.context().getElement();
        if (context == null || !context.isValid()) {
            return DocumentationJob.nothing();
        }

        String href = source.href();
        if (DocumentationLinks.isPsiElementLink(href)) {
            PsiElement target = DocumentationManagerHelper.resolvePsiElementLink(context, DocumentationLinks.getPsiElementReference(href));
            if (target == null) {
                return DocumentationJob.nothing();
            }

            String anchor = DocumentationLinks.getPsiElementAnchor(href);
            return DocumentationJob.collect(new ElementDocumentationCollector(myProject, target, null, anchor, false), null);
        }

        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(context);
        if (provider instanceof CompositeDocumentationProvider composite) {
            for (DocumentationProvider p : composite.getAllProviders()) {
                if (!(p instanceof ExternalDocumentationHandler handler)) {
                    continue;
                }

                if (handler.canFetchDocumentationLink(href)) {
                    String anchor = handler.extractRefFromLink(href);
                    return DocumentationJob.collect(new DocumentationCollector(context, href, anchor, p) {
                        @Override
                        public @Nullable String getDocumentation() {
                            return handler.fetchExternalDocumentation(href, context);
                        }
                    }, null);
                }

                if (handler.handleExternalLink(context.getManager(), href, context)) {
                    return DocumentationJob.nothing();
                }
            }
        }

        if (BrowserUtil.isAbsoluteURL(href)) {
            return DocumentationJob.browse(href);
        }
        return DocumentationJob.message(CodeInsightLocalize.javadocErrorResolvingUrl(href).get());
    }

    private DocumentationJob generate(DocumentationJob job, Continuation<?> continuation) {
        DocumentationCollector collector = job.myCollector;
        if (collector == null || job.myDocumentation != null) {
            return job;
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        ScheduledFuture<?> cancellationWatcher = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            () -> {
                if (continuation.isCancelled() || continuation.scope().isCancelled()) {
                    indicator.cancel();
                }
            },
            CANCELLATION_CHECK_MILLISECONDS,
            CANCELLATION_CHECK_MILLISECONDS,
            TimeUnit.MILLISECONDS
        );

        try {
            String documentation = ProgressManager.getInstance().runProcess(() -> collect(collector), indicator);
            if (documentation == null) {
                job.myMessage = CodeInsightLocalize.noDocumentationFound().get();
            }
            else {
                job.myDocumentation = documentation;
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (IndexNotReadyException e) {
            job.myMessage = CodeInsightLocalize.documentationMessageDocumentationIsNotAvailable().get();
        }
        catch (RuntimeException e) {
            LOG.warn("Failed to generate documentation for " + collector.getElement(), e);
            job.myMessage = CodeInsightLocalize.javadocExternalFetchErrorMessage().get();
        }
        finally {
            cancellationWatcher.cancel(false);
        }
        return job;
    }

    private static @Nullable String collect(DocumentationCollector collector) {
        try {
            return collector.getDocumentation();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @RequiredReadAction
    private static DocumentationJob buildPage(DocumentationJob job) {
        String message = job.myMessage;
        if (message != null) {
            job.myPage = DocumentationPageBuilder.message(message);
            return job;
        }

        DocumentationCollector collector = job.myCollector;
        String documentation = job.myDocumentation;
        if (collector == null || documentation == null || documentation.isEmpty()) {
            return job;
        }

        PsiElement element = collector.getElement();
        if (!element.isValid()) {
            return job;
        }

        DocumentationProvider provider = collector.getProvider();
        if (provider == null) {
            PsiElement originalElement = collector instanceof ElementDocumentationCollector elementCollector
                ? elementCollector.getOriginalElement()
                : null;
            provider = DocumentationManagerHelper.getProviderFromElement(element, originalElement);
        }

        job.myPage = DocumentationPageBuilder.build(element, documentation, collector.getEffectiveUrl(), collector.getRef(), provider);
        return job;
    }

    @RequiredUIAccess
    private void apply(DocumentationJob job, boolean resetHistory) {
        String browseUrl = job.myBrowseUrl;
        if (browseUrl != null) {
            BrowserUtil.browse(browseUrl);
        }

        DocumentationPage page = job.myPage;
        if (page == null) {
            return;
        }

        if (resetHistory) {
            myBackStack.clear();
            myForwardStack.clear();
        }
        else {
            DocumentationPage current = myPage;
            if (current != null) {
                myBackStack.push(current);
            }
            myForwardStack.clear();
        }

        setPage(page);
    }

    @RequiredUIAccess
    private void setPage(DocumentationPage page) {
        myPage = page;
        myCanGoBack = !myBackStack.isEmpty();
        myCanGoForward = !myForwardStack.isEmpty();

        for (Consumer<DocumentationPage> listener : myPageListeners) {
            listener.accept(page);
        }
    }

    @Override
    public void dispose() {
        myPageListeners.clear();
        myFragmentListeners.clear();
        myBackStack.clear();
        myForwardStack.clear();
        myLoading = null;
    }
}
