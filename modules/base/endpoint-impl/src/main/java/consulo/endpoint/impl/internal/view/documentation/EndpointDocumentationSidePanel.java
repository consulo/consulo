package consulo.endpoint.impl.internal.view.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointDocumentationProvider;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.internal.DocumentationView;
import consulo.language.editor.internal.DocumentationViewFactory;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNavigationSupport;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

final class EndpointDocumentationSidePanel implements EndpointSidePanel, Disposable {
    private final Project myProject;

    private final DockLayout myRoot;
    private final DocumentationView myDocumentationView;
    private final Label myEmptyLabel;

    private @Nullable SmartPsiElementPointer<PsiElement> myNavigationPointer;
    private @Nullable Disposable myCustomDisposable;
    private @Nullable Disposable myNavigationDisposable;

    @RequiredUIAccess
    EndpointDocumentationSidePanel(Project project, DocumentationViewFactory documentationViewFactory) {
        myProject = project;

        myDocumentationView = documentationViewFactory.create();
        Disposer.register(this, myDocumentationView);
        myDocumentationView.addDoubleClickListener(this::navigateToSource);

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
        return new EndpointElementDocumentation(SmartPointerManager.createPointer(element), navigationPointer);
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

    @RequiredUIAccess
    private void show(EndpointDocumentationContent content) {
        disposeCustomComponent();

        if (content instanceof EndpointCustomDocumentation<?> custom) {
            myNavigationPointer = null;
            Disposable customDisposable = Disposable.newDisposable("EndpointCustomDocumentation");
            Disposer.register(this, customDisposable);
            myCustomDisposable = customDisposable;
            Component component = custom.createComponent(customDisposable);
            myRoot.center(component == null ? myEmptyLabel : component);
        }
        else if (content instanceof EndpointElementDocumentation elementDocumentation) {
            myNavigationPointer = elementDocumentation.navigationPointer();
            myDocumentationView.showElement(elementDocumentation.pointer(), null);
            myRoot.center(myDocumentationView.getComponent());
        }
        else {
            myNavigationPointer = null;
            myDocumentationView.clear();
            myRoot.center(myEmptyLabel);
        }
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
    private void disposeCustomComponent() {
        Disposable customDisposable = myCustomDisposable;
        if (customDisposable != null) {
            myCustomDisposable = null;
            Disposer.dispose(customDisposable);
        }
    }
}
