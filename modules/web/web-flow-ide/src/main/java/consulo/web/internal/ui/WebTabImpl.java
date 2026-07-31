/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.html.Span;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.web.internal.ui.action.WebActionContextMenu;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
class WebTabImpl implements Tab {
    private BiConsumer<Tab, TextItemPresentation> myRenderer = (tab, presentation) -> presentation.append(toString());
    private @Nullable BiConsumer<Tab, Component> myCloseHandler;
    private WebTabbedLayoutImpl myTabbedLayout;

    private com.vaadin.flow.component.tabs.Tab myVaadinTab;
    private @Nullable Component myContent;
    private @Nullable WebActionContextMenu myPopupMenu;

    public WebTabImpl(WebTabbedLayoutImpl tabbedLayout) {
        myTabbedLayout = tabbedLayout;
    }

    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
        myCloseHandler = closeHandler;
    }

    @Override
    public void update() {
        WebItemPresentationImpl presentation = new WebItemPresentationImpl();
        myRenderer.accept(this, presentation);

        myVaadinTab.removeAll();
        myVaadinTab.add(presentation.toComponent());

        // vaadin tabs carry no close affordance, the awt tabbed container draws the cross itself as well
        BiConsumer<Tab, Component> closeHandler = myCloseHandler;
        if (closeHandler != null) {
            Span close = new Span("\u00d7");
            close.addClassName("web-tab-close");
            close.getElement()
                .addEventListener("click", event -> closeHandler.accept(this, myContent))
                // otherwise the click selects the tab that is about to go away
                .addEventData("event.stopPropagation()");

            myVaadinTab.add(close);
        }
    }

    public BiConsumer<Tab, TextItemPresentation> getRenderer() {
        return myRenderer;
    }

    @Override
    public void select() {
        myTabbedLayout.toVaadinComponent().setSelectedTab(myVaadinTab);
    }

    public @Nullable BiConsumer<Tab, Component> getCloseHandler() {
        return myCloseHandler;
    }

    public void setVaadinTab(com.vaadin.flow.component.tabs.Tab vaadinTab) {
        myVaadinTab = vaadinTab;
    }

    public com.vaadin.flow.component.tabs.Tab getVaadinTab() {
        return myVaadinTab;
    }

    /**
     * Must be called once the content is already inside the layout - the data context of a tab is the one of its
     * content, and the providers above the content are only reachable after it is added.
     */
    @RequiredUIAccess
    public void setContent(Component content) {
        myContent = content;

        // consulo.ui.Tab has no popup group of its own, an editor tab is recognized by the window its content
        // belongs to, the same data the popup actions themselves work on
        if (myPopupMenu == null && DataManager.getInstance().getDataContext(content).getData(FileEditorWindow.DATA_KEY) != null) {
            myPopupMenu = new WebActionContextMenu(
                myVaadinTab,
                IdeActions.GROUP_EDITOR_TAB_POPUP,
                ActionPlaces.EDITOR_TAB_POPUP,
                this::createDataContext
            );
        }
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myContent));
    }
}
