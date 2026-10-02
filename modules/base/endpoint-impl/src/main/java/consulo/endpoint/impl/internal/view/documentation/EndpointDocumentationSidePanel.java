package consulo.endpoint.impl.internal.view.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointDocumentationProvider;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNavigationSupport;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class EndpointDocumentationSidePanel implements EndpointSidePanel, Disposable {
    private static final long CANCELLATION_CHECK_MILLISECONDS = 50;

    private final Project myProject;
    private final DocumentationManager myDocumentationManager;

    private final DockLayout myRoot;
    private final HtmlView myHtmlView;
    private final Component myHtmlComponent;
    private final Label myEmptyLabel;

    private @Nullable SmartPsiElementPointer<PsiElement> myElementPointer;
    private @Nullable SmartPsiElementPointer<PsiElement> myNavigationPointer;
    private @Nullable Disposable myCustomDisposable;
    private @Nullable Disposable myLinkDisposable;
    private @Nullable Disposable myNavigationDisposable;

    @RequiredUIAccess
    EndpointDocumentationSidePanel(Project project, DocumentationManager documentationManager) {
        myProject = project;
        myDocumentationManager = documentationManager;

        myHtmlView = HtmlView.create();
        myHtmlView.addHyperlinkListener(this::onHyperlink);
        myHtmlView.addDoubleClickListener(event -> navigateToSource());
        myHtmlComponent = ScrollableLayout.create(myHtmlView);
        myEmptyLabel = Label.create(CodeInsightLocalize.noDocumentationFound());

        myRoot = DockLayout.create(Space.NONE);
        myRoot.center(myEmptyLabel);
    }

    @Override
    public LocalizeValue getTitle() {
        return EndpointLocalize.endpointsDetailsDocumentationTitle();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myRoot;
    }

    @Override
    public Coroutine<?, Boolean> isAvailable(List<? extends EndpointListItem> selectedItems) {
        return Coroutine.first(ReadLock.<Object, Boolean>apply(ignored -> isDocumentationAvailable(selectedItems)));
    }

    @Override
    public Coroutine<?, ?> update(List<? extends EndpointListItem> selectedItems) {
        return Coroutine
            .first(ReadLock.<Object, EndpointDocumentationContent>apply(ignored -> prepare(selectedItems)))
            .then(CodeExecution.<EndpointDocumentationContent, EndpointDocumentationContent>apply(this::generate))
            .then(UIAction.<EndpointDocumentationContent, EndpointDocumentationContent>apply(content -> {
                show(content);
                return content;
            }));
    }

    @Override
    public void dispose() {
    }

    @RequiredReadAction
    private static boolean isDocumentationAvailable(List<? extends EndpointListItem> selectedItems) {
        if (selectedItems.size() != 1 || !(selectedItems.get(0) instanceof EndpointElementItem<?, ?> item)) {
            return false;
        }
        return hasDocumentation(item);
    }

    @RequiredReadAction
    private static <G, E> boolean hasDocumentation(EndpointElementItem<G, E> item) {
        if (!item.isValid()) {
            return false;
        }
        EndpointProvider<G, E> provider = item.getProvider();
        return provider instanceof EndpointDocumentationProvider<?, ?, ?>
            || provider.getDocumentationElement(item.getGroup(), item.getEndpoint()) != null;
    }

    @RequiredReadAction
    private static EndpointDocumentationContent prepare(List<? extends EndpointListItem> selectedItems) {
        if (selectedItems.size() != 1 || !(selectedItems.get(0) instanceof EndpointElementItem<?, ?> item)) {
            return EndpointNoDocumentation.INSTANCE;
        }
        return prepare(item);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private static <G, E> EndpointDocumentationContent prepare(EndpointElementItem<G, E> item) {
        if (!item.isValid()) {
            return EndpointNoDocumentation.INSTANCE;
        }

        EndpointProvider<G, E> provider = item.getProvider();
        G group = item.getGroup();
        E endpoint = item.getEndpoint();
        if (provider instanceof EndpointDocumentationProvider<?, ?, ?> documentationProvider) {
            EndpointDocumentationContent custom =
                prepareCustom((EndpointDocumentationProvider<G, E, ?>) documentationProvider, group, endpoint);
            if (custom != null) {
                return custom;
            }
        }

        PsiElement element = provider.getDocumentationElement(group, endpoint);
        if (element == null) {
            return EndpointNoDocumentation.INSTANCE;
        }
        PsiElement navigationElement = provider.getNavigationElement(group, endpoint);
        SmartPsiElementPointer<PsiElement> navigationPointer =
            navigationElement == null ? null : SmartPointerManager.createPointer(navigationElement);
        return new EndpointElementDocumentation(SmartPointerManager.createPointer(element), null, navigationPointer, null);
    }

    @RequiredReadAction
    private static <G, E, R> @Nullable EndpointDocumentationContent prepareCustom(
        EndpointDocumentationProvider<G, E, R> provider,
        G group,
        E endpoint
    ) {
        R request = provider.prepareDocumentationRequest(group, endpoint);
        return request == null ? null : new EndpointCustomDocumentation<>(provider, request);
    }

    private EndpointDocumentationContent generate(EndpointDocumentationContent content, Continuation<?> continuation) {
        if (!(content instanceof EndpointElementDocumentation elementDocumentation) || elementDocumentation.html() != null) {
            return content;
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
            String html = ProgressManager.getInstance().runProcess(() -> generateHtml(elementDocumentation), indicator);
            return elementDocumentation.withHtml(html);
        }
        finally {
            cancellationWatcher.cancel(false);
        }
    }

    private @Nullable String generateHtml(EndpointElementDocumentation content) {
        PsiElement element = ReadAction.compute(() -> content.pointer().getElement());
        if (element == null) {
            return null;
        }

        SmartPsiElementPointer<PsiElement> originalPointer = content.originalPointer();
        PsiElement originalElement = originalPointer == null ? null : ReadAction.compute(originalPointer::getElement);
        return myDocumentationManager.generateDocumentation(element, originalElement, false);
    }

    @RequiredUIAccess
    private void show(EndpointDocumentationContent content) {
        cancelLink();
        disposeCustomComponent();

        if (content instanceof EndpointCustomDocumentation<?> custom) {
            myElementPointer = null;
            myNavigationPointer = null;
            Disposable customDisposable = Disposable.newDisposable("EndpointCustomDocumentation");
            Disposer.register(this, customDisposable);
            myCustomDisposable = customDisposable;
            Component component = custom.createComponent(customDisposable);
            myRoot.center(component == null ? myEmptyLabel : component);
        }
        else if (content instanceof EndpointElementDocumentation elementDocumentation) {
            myElementPointer = elementDocumentation.pointer();
            myNavigationPointer = elementDocumentation.navigationPointer();
            showHtml(elementDocumentation.html());
        }
        else {
            myElementPointer = null;
            myNavigationPointer = null;
            myRoot.center(myEmptyLabel);
        }
    }

    @RequiredUIAccess
    private void showHtml(@Nullable String html) {
        if (html == null || html.isEmpty()) {
            myRoot.center(myEmptyLabel);
            return;
        }
        myHtmlView.render(new HtmlView.RenderData(html));
        myRoot.center(myHtmlComponent);
    }

    @RequiredUIAccess
    private void onHyperlink(HyperlinkEvent event) {
        String description = event.getDescription();
        if (description.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
            navigateToLink(description);
        }
        else if (BrowserUtil.isAbsoluteURL(description)) {
            BrowserUtil.browse(description);
        }
    }

    @RequiredUIAccess
    private void navigateToLink(String url) {
        SmartPsiElementPointer<PsiElement> pointer = myElementPointer;
        if (pointer == null) {
            return;
        }

        SmartPsiElementPointer<PsiElement> navigationPointer = myNavigationPointer;
        cancelLink();
        Disposable linkDisposable = Disposable.newDisposable("EndpointDocumentationLink");
        Disposer.register(this, linkDisposable);
        myLinkDisposable = linkDisposable;

        DisposableCoroutineScope.launchAsync(
            myProject.coroutineContext(),
            linkDisposable,
            () -> Coroutine
                .first(ReadLock.<Object, EndpointDocumentationContent>apply(ignored -> resolveLink(pointer, navigationPointer, url)))
                .then(CodeExecution.<EndpointDocumentationContent, EndpointDocumentationContent>apply(this::generate))
                .then(UIAction.<EndpointDocumentationContent, EndpointDocumentationContent>apply(content -> {
                    if (myLinkDisposable == linkDisposable && content instanceof EndpointElementDocumentation elementDocumentation) {
                        myElementPointer = elementDocumentation.pointer();
                        showHtml(elementDocumentation.html());
                    }
                    return content;
                }))
        );
    }

    @RequiredReadAction
    private static EndpointDocumentationContent resolveLink(
        SmartPsiElementPointer<PsiElement> pointer,
        @Nullable SmartPsiElementPointer<PsiElement> navigationPointer,
        String url
    ) {
        PsiElement context = pointer.getElement();
        if (context == null) {
            return EndpointNoDocumentation.INSTANCE;
        }

        String refText = url.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());
        int separatorPos = refText.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR);
        String link = separatorPos >= 0 ? refText.substring(0, separatorPos) : refText;

        PsiElement target = DocumentationManagerHelper.resolvePsiElementLink(context, link);
        if (target == null) {
            return EndpointNoDocumentation.INSTANCE;
        }
        return new EndpointElementDocumentation(SmartPointerManager.createPointer(target), pointer, navigationPointer, null);
    }

    @RequiredUIAccess
    private void navigateToSource() {
        SmartPsiElementPointer<PsiElement> pointer = myNavigationPointer;
        if (pointer == null) {
            return;
        }

        cancelNavigation();
        Disposable navigationDisposable = Disposable.newDisposable("EndpointDocumentationNavigation");
        Disposer.register(this, navigationDisposable);
        myNavigationDisposable = navigationDisposable;

        DisposableCoroutineScope.launchAsync(
            myProject.coroutineContext(),
            navigationDisposable,
            () -> Coroutine
                .first(ReadLock.<Object, Optional<Navigatable>>apply(ignored -> Optional.ofNullable(findNavigatable(pointer))))
                .then(UIAction.<Optional<Navigatable>, Optional<Navigatable>>apply(navigatable -> {
                    if (myNavigationDisposable == navigationDisposable) {
                        navigatable.ifPresent(it -> it.navigate(true));
                    }
                    return navigatable;
                }))
        );
    }

    @RequiredReadAction
    private static @Nullable Navigatable findNavigatable(SmartPsiElementPointer<PsiElement> pointer) {
        PsiElement element = pointer.getElement();
        if (element == null || !element.isValid()) {
            return null;
        }
        return PsiNavigationSupport.getInstance().getDescriptor(element);
    }

    @RequiredUIAccess
    private void cancelNavigation() {
        Disposable navigationDisposable = myNavigationDisposable;
        if (navigationDisposable != null) {
            myNavigationDisposable = null;
            Disposer.dispose(navigationDisposable);
        }
    }

    @RequiredUIAccess
    private void cancelLink() {
        Disposable linkDisposable = myLinkDisposable;
        if (linkDisposable != null) {
            myLinkDisposable = null;
            Disposer.dispose(linkDisposable);
        }
    }

    @RequiredUIAccess
    private void disposeCustomComponent() {
        Disposable customDisposable = myCustomDisposable;
        if (customDisposable != null) {
            myCustomDisposable = null;
            Disposer.dispose(customDisposable);
        }
    }
}
