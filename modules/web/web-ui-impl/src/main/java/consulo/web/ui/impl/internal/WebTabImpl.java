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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.html.Span;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.ui.Component;
import consulo.ui.color.ColorValue;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.web.ui.impl.internal.action.WebActionContextMenu;
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
    private @Nullable String myPopupGroupId;
    private @Nullable String myPopupPlace;

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

        // the fill of a tab means the whole tab, not the run of text. the value is only handed to the
        // stylesheet - tabs.css decides what --consulo-tab-background is drawn as
        ColorValue background = presentation.getBackgroundColor();
        if (background != null) {
            myVaadinTab.getElement().getStyle().set("--consulo-tab-background", WebColors.toCssColor(background));
        }
        else {
            myVaadinTab.getElement().getStyle().remove("--consulo-tab-background");
        }

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
     * The content is what the data context of the tab is read from - the providers which answer for it sit above
     * it in the layout.
     */
    @RequiredUIAccess
    public void setContent(Component content) {
        myContent = content;

        // the vaadin tab the menu hangs on only exists once the tab was rendered into the layout, so the group
        // is remembered when it is given and the menu is built here
        if (myPopupMenu == null && myPopupGroupId != null) {
            myPopupMenu = new WebActionContextMenu(myVaadinTab, myPopupGroupId, myPopupPlace, this::createDataContext);
        }
    }

    @Override
    public void setPopupGroup(String groupId, String place) {
        myPopupGroupId = groupId;
        myPopupPlace = place;
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myContent));
    }
}
