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
package consulo.web.internal.wm;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.navigationBar.NavBarService;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarItemVm;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.navigationBar.model.NavBarVmListener;
import consulo.navigationBar.impl.internal.NavBarVmImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.web.ui.impl.internal.WebFocusManagerImpl;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebFocusTracker;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Navigation bar, rendered with the vaadin breadcrumbs component and driven by {@link NavBarVmImpl}, mirroring the
 * awt {@code NavBarUIController} / {@code NewNavBarPanel} pair.
 *
 * @author VISTALL
 */
public class WebNavigationBar implements Disposable {
    public static class Vaadin extends Breadcrumbs implements FromVaadinComponentWrapper {
        private consulo.ui.Component myComponent;

        public Vaadin() {
            // the default ROUTER mode rebuilds the crumbs from the vaadin route on every navigation, which wipes
            // the items pushed from the nav bar model
            super(Mode.MANUAL);

            addClassName(CRUMBS_CLASS_NAME);
        }

        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return myComponent;
        }

        public void setComponent(consulo.ui.Component component) {
            myComponent = component;
        }
    }

    private static class BarComponent extends VaadinComponentDelegate<Vaadin> {
        @Override
        public Vaadin createVaadinComponent() {
            Vaadin vaadin = new Vaadin();
            vaadin.setComponent(this);
            return vaadin;
        }
    }

    /**
     * Group of the toolbar the awt nav bar carries on its east side, see
     * {@code NavBarRootPaneExtensionImpl#toggleRunPanel}.
     */
    private static final String TOOLBAR_GROUP_ID = "NavBarToolBar";

    public static final String ROW_CLASS_NAME = "web-navigation-bar";

    public static final String CRUMBS_CLASS_NAME = "web-navigation-bar-crumbs";

    private final Project myProject;
    private final consulo.ui.Component myContextComponent;
    private final BarComponent myComponent = new BarComponent();

    // the row is a dock so that the empty center takes the free width and keeps the toolbar flush right
    private final DockLayout myRowLayout = DockLayout.create(0);

    private final @Nullable UnifiedActionToolbarImpl myToolbar;

    private @Nullable NavBarVmImpl myVm;

    @RequiredUIAccess
    public WebNavigationBar(Project project, consulo.ui.Component contextComponent) {
        myProject = project;
        myContextComponent = contextComponent;

        UIAccess uiAccess = UIAccess.current();

        TargetVaadin.to(myRowLayout).addClassName(ROW_CLASS_NAME);

        myRowLayout.left(myComponent);

        myToolbar = createToolbar();
        if (myToolbar != null) {
            // the actions have to be updated against the scope the user last worked in, the same context the bar
            // itself reads - ActionToolbar can only be pointed at a component, and the browser has no focus owner
            myToolbar.setDataContextSupplier(this::createDataContext);

            myRowLayout.right(myToolbar.getUIComponent());
        }

        navBarService().defaultModel().whenCompleteAsync((item, throwable) -> {
            if (throwable != null || item == null || myVm != null) {
                return;
            }

            NavBarVmImpl vm = new NavBarVmImpl(List.of(item));
            vm.addListener(new NavBarVmListener() {
                @Override
                public void itemsChanged(List<? extends NavBarItemVm> items) {
                    rebuild(items);
                }

                @Override
                public void activationRequested(NavBarVmItem activatedItem) {
                    navBarService().navigate(activatedItem).whenCompleteAsync((ignored, error) -> update(), uiAccess);
                }
            });

            myVm = vm;

            rebuild(vm.getItems());

            update();
        }, uiAccess);

        navBarService().subscribeActivity(this, this::update);

        // subscribeActivity does not cover editor selection, and without this the bar keeps the model it computed
        // at frame show, when no file was open yet
        myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(FileEditorManagerEvent event) {
                update();
            }

            @Override
            public void fileOpened(FileEditorManager source, VirtualFile file) {
                update();
            }
        });

        // the model goes down to the element under the caret, so the bar has to follow the caret and not only the
        // opened file - otherwise it never shows the class or the method
        EditorFactory.getInstance().getEventMulticaster().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                update();
            }
        }, this);

        // moving between the project view and the editor changes nothing in the project model, only which context
        // the bar has to read - the tree selection itself arrives in the same client message as the focus switch
        Disposer.register(this, WebFocusManagerImpl.ourInstance.addListener(this::update));
    }

    /**
     * The bar outlives the project it was built for - the frame is taken down after the project is disposed, and the
     * listeners it is driven from keep firing until it is. Asking a disposed project for a service throws, and on a
     * browser frontend that throw lands in the request rather than on a screen: every round trip answers 500 and the
     * ui stops on the first one.
     */
    private boolean isObsolete() {
        return myProject.isDisposed();
    }

    private NavBarService navBarService() {
        return myProject.getInstance(NavBarService.class);
    }

    private static @Nullable UnifiedActionToolbarImpl createToolbar() {
        AnAction group = CustomActionsSchema.getInstance().getCorrectedAction(TOOLBAR_GROUP_ID);

        return group instanceof ActionGroup actionGroup
            ? new UnifiedActionToolbarImpl(ActionPlaces.NAVIGATION_BAR_TOOLBAR, actionGroup, ActionToolbar.Style.HORIZONTAL)
            : null;
    }

    /**
     * The context has to come from the scope the user last worked in, not from the bar itself - the bar knows
     * nothing about the open editor or the project view selection, so its own context always produced an empty
     * model.
     * <p/>
     * The nav bar item is produced by a UiDataRule, and only the pre cached context runs the rules - a plain
     * component context would answer null for the key and the bar would stay on the project root.
     */
    private DataContext createDataContext() {
        return DataManager.getInstance()
            .createAsyncDataContext(WebFocusTracker.createDataContext(myContextComponent));
    }

    /**
     * Recomputes the model from the current context. The model arrives asynchronously, so the bar keeps showing the
     * previous items until it does.
     */
    public void update() {
        if (isObsolete()) {
            return;
        }

        UIAccess uiAccess = myProject.getUIAccess();

        if (myToolbar != null) {
            uiAccess.giveIfNeed(myToolbar::updateActionsAsync);
        }

        NavBarVmImpl vm = myVm;
        if (vm == null) {
            return;
        }

        DataContext dataContext = createDataContext();

        navBarService().contextModel(dataContext).whenCompleteAsync((model, throwable) -> {
            if (throwable == null && model != null && !model.isEmpty()) {
                vm.contextItemsChanged(model);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void rebuild(List<? extends NavBarItemVm> items) {
        Vaadin vaadin = myComponent.toVaadinComponent();

        vaadin.removeAll();

        for (NavBarItemVm item : items) {
            NavBarItemPresentationData presentation = item.getPresentation();

            String text = presentation.text();
            if (text == null || text.isBlank()) {
                text = presentation.popupText();
            }

            if (text == null || text.isBlank()) {
                continue;
            }

            BreadcrumbsItem crumb = new BreadcrumbsItem(text);
            crumb.setText(text);

            Image icon = presentation.icon();
            if (icon != null) {
                crumb.setPrefixComponent(WebImageConverter.getImage(icon));
            }

            // BreadcrumbsItem only navigates by route, the item activation has to be wired by hand
            crumb.getElement().addEventListener("click", event -> item.activate());

            vaadin.add(crumb);
        }
    }

    public Component getComponent() {
        return myRowLayout;
    }

    @Override
    public void dispose() {
        myVm = null;
    }
}
