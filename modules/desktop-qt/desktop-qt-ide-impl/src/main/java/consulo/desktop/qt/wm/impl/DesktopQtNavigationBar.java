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
package consulo.desktop.qt.wm.impl;

import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.DesktopQtFocusManagerImpl;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.localize.LocalizeValue;
import consulo.navigationBar.NavBarService;
import consulo.navigationBar.impl.internal.NavBarVmImpl;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarItemVm;
import consulo.navigationBar.model.NavBarPopupItem;
import consulo.navigationBar.model.NavBarPopupVm;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.navigationBar.model.NavBarVmListener;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.LightPopup;
import consulo.ui.ListBox;
import consulo.ui.Popup;
import consulo.ui.PopupOptions;
import consulo.ui.Space;
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
import consulo.ui.layout.HorizontalLayout;
import consulo.virtualFileSystem.VirtualFile;
import io.qt.widgets.QLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation bar, driven by {@link NavBarVmImpl} and mirroring the awt {@code NavBarUIController} /
 * {@code NewNavBarPanel} pair and the web {@code WebNavigationBar}. Qt has no breadcrumbs widget, so the crumbs
 * are a row of borderless buttons.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtNavigationBar implements Disposable {
    /**
     * Group of the toolbar the awt nav bar carries on its east side, see
     * {@code NavBarRootPaneExtensionImpl#toggleRunPanel}.
     */
    private static final String TOOLBAR_GROUP_ID = "NavBarToolBar";

    private static final LocalizeValue CRUMB_SEPARATOR = LocalizeValue.of("\u203A");

    /**
     * What the awt bar keeps around a crumb, see {@code NavBarUi#navBarItemInsets} - a qt layout is built with no
     * margins at all, so the row sat flush against the frame.
     */
    private static final int ROW_MARGIN_X = 4;
    private static final int ROW_MARGIN_Y = 2;

    private final Project myProject;
    private final Component myContextComponent;

    private final HorizontalLayout myCrumbsLayout = HorizontalLayout.create(Space.NONE);

    // the row is a dock so that the empty center takes the free width and keeps the toolbar flush right
    private final DockLayout myRowLayout = DockLayout.create(Space.NONE);

    private final @Nullable UnifiedActionToolbarImpl myToolbar;

    /**
     * The crumb of every item of the model, by the index the item has in it - an item with nothing to show is left
     * out of the row, and the view model addresses its items by index.
     */
    private final List<@Nullable Button> myCrumbs = new ArrayList<>();

    private @Nullable NavBarVmImpl myVm;

    private @Nullable Popup myPopup;

    @RequiredUIAccess
    public DesktopQtNavigationBar(Project project, Component contextComponent) {
        myProject = project;
        myContextComponent = contextComponent;

        UIAccess uiAccess = UIAccess.current();

        applyRowMargin();

        myRowLayout.left(myCrumbsLayout);

        myToolbar = createToolbar();
        if (myToolbar != null) {
            // the actions have to be updated against the scope the user last worked in, the same context the bar
            // itself reads - ActionToolbar can only be pointed at a component, and the bar is never focused
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
                public void popupChanged(@Nullable NavBarPopupVm<?> popup) {
                    hidePopup();

                    if (popup != null) {
                        showPopup(vm.getSelectedIndex(), popup);
                    }
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
        // the bar has to read
        Disposer.register(this, FocusManager.get().addListener(this::update));
    }

    /**
     * The bar outlives the project it was built for - the frame is taken down after the project is disposed, and the
     * listeners it is driven from keep firing until it is. Asking a disposed project for a service throws.
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
        DataManager dataManager = DataManager.getInstance();

        // the context is walked upwards from the component it is asked for, so the frame root - which is what the
        // bar itself hangs under - only ever reaches the project, and the model then stops at the project root.
        // the scope the user is working in is the one which carries the open file
        Component component = DesktopQtFocusManagerImpl.INSTANCE.getFocusedComponent();

        return dataManager.createAsyncDataContext(
            dataManager.getDataContext(component == null ? myContextComponent : component)
        );
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
        hidePopup();

        myCrumbsLayout.removeAll();
        myCrumbs.clear();

        boolean first = true;

        for (NavBarItemVm item : items) {
            NavBarItemPresentationData presentation = item.getPresentation();

            String text = presentation.text();
            if (text == null || text.isBlank()) {
                text = presentation.popupText();
            }

            if (text == null || text.isBlank()) {
                myCrumbs.add(null);
                continue;
            }

            if (!first) {
                Label separator = Label.create(CRUMB_SEPARATOR);
                separator.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);
                myCrumbsLayout.add(separator);
            }
            first = false;

            Button crumb = Button.create(LocalizeValue.of(text));
            crumb.addStyle(ButtonStyle.BORDERLESS);

            Image icon = presentation.icon();
            if (icon != null) {
                crumb.setIcon(icon);
            }

            // what the awt bar does with a single click, see NavBarItemComponent - the crumb offers what is under
            // it rather than navigating to itself, and choosing from that offer is what navigates
            crumb.addClickListener(event -> {
                item.select();
                item.showPopup();
            });

            myCrumbs.add(crumb);
            myCrumbsLayout.add(crumb);
        }
    }

    /**
     * Offers the children of the crumb which was clicked. Choosing one hands it back to the view model, which
     * either navigates to it or answers with the popup of the level below - so the chain is driven from there and
     * only one popup is ever up.
     */
    @RequiredUIAccess
    private <T extends NavBarPopupItem> void showPopup(int index, NavBarPopupVm<T> popupVm) {
        Button crumb = index >= 0 && index < myCrumbs.size() ? myCrumbs.get(index) : null;

        List<T> items = popupVm.getItems();
        if (crumb == null || items.isEmpty()) {
            popupVm.cancel();
            return;
        }

        ListBox<T> list = ListBox.create(items);
        list.setRender((presentation, item) -> {
            T value = item.getValue();
            if (value == null) {
                return;
            }

            NavBarItemPresentationData data = value.getPresentation();

            presentation.withIcon(data.icon());

            String popupText = data.popupText();
            presentation.append(popupText == null ? data.text() : popupText);
        });

        // the pointer is what the user is choosing with, so the row under it is the one the popup is offering
        list.setSelectOnHover(true);

        int selected = popupVm.getInitialSelectedItemIndex();
        if (selected >= 0 && selected < items.size()) {
            list.setValueByIndex(selected);
        }

        list.addValueListener(event -> {
            T value = event.getValue();
            popupVm.itemsSelected(value == null ? List.of() : List.of(value));
        });

        LightPopup popup = LightPopup.create(PopupOptions.builder().build());
        popup.setContent(list);

        list.addClickListener(event -> {
            T value = list.getValue();
            if (value == null) {
                return;
            }

            popupVm.itemsSelected(List.of(value));

            myPopup = null;
            popup.close();

            // the view model answers this with the next popup, so the one which was chosen from has to be gone
            popupVm.complete();
        });

        // a popup taken down by the user - a click outside, escape - is an abandoned choice, and the chain has to
        // be told so. a popup this closed itself is not, which is what dropping the field above says
        popup.addCloseListener(event -> {
            if (myPopup == popup) {
                myPopup = null;
                popupVm.cancel();
            }
        });

        myPopup = popup;

        popup.showBy(crumb);
    }

    @RequiredUIAccess
    private void hidePopup() {
        Popup popup = myPopup;
        if (popup == null) {
            return;
        }

        myPopup = null;

        if (popup.isVisible()) {
            popup.close();
        }
    }

    /**
     * A qt layout is built with no margins of its own, so the row has to be given the ones the bar is drawn with.
     */
    @RequiredUIAccess
    private void applyRowMargin() {
        if (!(myRowLayout instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.whenBound(widget -> {
            QLayout layout = widget.layout();
            if (layout != null) {
                layout.setContentsMargins(ROW_MARGIN_X, ROW_MARGIN_Y, ROW_MARGIN_X, ROW_MARGIN_Y);
            }
        });
    }

    public Component getComponent() {
        return myRowLayout;
    }

    @Override
    public void dispose() {
        myVm = null;
    }
}
