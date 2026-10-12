/*
 * Copyright 2013-2019 consulo.io
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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.internal.TabSelectionUtil;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebTabbedLayoutImpl extends VaadinComponentDelegate<WebTabbedLayoutImpl.Vaadin> implements TabbedLayout {
    public class Vaadin extends TabSheet implements FromVaadinComponentWrapper {
        public Vaadin() {
            // the default border is drawn inside the sheet, so the content no longer fits and scrolls
            addThemeVariants(TabSheetVariant.AURA_NO_BORDER);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebTabbedLayoutImpl.this;
        }
    }

    private final Map<com.vaadin.flow.component.tabs.Tab, WebTabImpl> myTabs = new LinkedHashMap<>();

    private @Nullable TransferHandler<Tab> myTransferHandler;
    private boolean myMovingTab;
    private boolean myStripDropInstalled;

    private final StaticPosition myTabPosition;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    /**
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    public WebTabbedLayoutImpl(StaticPosition tabPosition) {
        myTabPosition = tabPosition;

        applyTabPosition(tabPosition);

        toVaadinComponent().addSelectedChangeListener(event -> {
            if (myMovingTab) {
                return;
            }

            com.vaadin.flow.component.tabs.Tab selectedTab = event.getSelectedTab();
            if (selectedTab == null) {
                restoreSelection(event.getPreviousTab());
                return;
            }

            WebTabImpl tab = myTabs.get(selectedTab);
            if (tab != null) {
                getListenerDispatcher(TabSelectEvent.class).onEvent(new TabSelectEvent(this, tab));
            }
        });
    }

    /**
     * The sheet lays its strip out above the content and has no setting for another side. Its host is a flex column of
     * two shadow parts - tabs-container and content - so tabs.css moves the strip by a theme name: below the content by
     * the flex order of the strip, at a side by turning the host into a row. A strip at a side also turns the tabs
     * component vertical so the tabs stack in a column.
     */
    private void applyTabPosition(StaticPosition tabPosition) {
        Vaadin sheet = toVaadinComponent();
        switch (tabPosition) {
            case TOP -> {
            }
            case BOTTOM -> sheet.getElement().getThemeList().add("consulo-tabs-bottom");
            case LEFT -> {
                sheet.getElement().getThemeList().add("consulo-tabs-left");
                getTabs(sheet).setOrientation(Tabs.Orientation.VERTICAL);
            }
            case RIGHT -> {
                sheet.getElement().getThemeList().add("consulo-tabs-right");
                getTabs(sheet).setOrientation(Tabs.Orientation.VERTICAL);
            }
            case CENTER -> throw new IllegalArgumentException("CENTER is not a valid tab position");
        }
    }

    /**
     * The sheet keeps its {@link Tabs} private, but creates it in its own constructor and puts it into its "tabs" slot as
     * a child element - before any tab, panel, prefix or suffix is added it is the only child component of the sheet. A
     * sheet without it cannot show the strip at a side, which fails loudly rather than leaving the strip above the content.
     */
    private static Tabs getTabs(Vaadin sheet) {
        return sheet.getChildren()
            .filter(Tabs.class::isInstance)
            .map(Tabs.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("The tab sheet has no tabs component to turn vertical"));
    }

    @Override
    public StaticPosition getTabPosition() {
        return myTabPosition;
    }

    private void restoreSelection(com.vaadin.flow.component.tabs.@Nullable Tab previousTab) {
        Vaadin sheet = toVaadinComponent();
        if (sheet.getTabCount() == 0) {
            return;
        }

        sheet.getUI().ifPresentOrElse(
            ui -> ui.beforeClientResponse(sheet, context -> reselect(previousTab)),
            () -> reselect(previousTab)
        );
    }

    private void reselect(com.vaadin.flow.component.tabs.@Nullable Tab previousTab) {
        Vaadin sheet = toVaadinComponent();
        if (sheet.getSelectedTab() != null || sheet.getTabCount() == 0) {
            return;
        }

        sheet.setSelectedTab(previousTab != null && myTabs.containsKey(previousTab) ? previousTab : sheet.getTabAt(0));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Tab createTab() {
        return new WebTabImpl(this);
    }

    @Override
    public void addStyle(TabbedLayoutStyle style) {
        switch (style) {
            case NO_PADDING -> toVaadinComponent().addThemeVariants(TabSheetVariant.NO_PADDING);
        }
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component) {
        return addTab(tab, component, toVaadinComponent().getTabCount());
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component, int index) {
        WebTabImpl webTab = (WebTabImpl) tab;

        com.vaadin.flow.component.tabs.Tab vaadinTab = new com.vaadin.flow.component.tabs.Tab();
        // the tab is not a VaadinComponentDelegate, so it carries the small variant itself - TabVariant has no
        // constant for it, the sheet only hands its own theme down to the tabs element and not to the tabs
        vaadinTab.getElement().getThemeList().add("small");
        webTab.setVaadinTab(vaadinTab);
        webTab.update();
        com.vaadin.flow.component.Component vaadinContent = TargetVaadin.to(component);
        // the sheet does not stretch its content, without this the editor keeps its intrinsic height
        if (vaadinContent instanceof HasSize hasSize) {
            hasSize.setSizeFull();
        }

        boolean first = toVaadinComponent().getTabCount() == 0;

        int position = Math.max(0, Math.min(index, toVaadinComponent().getTabCount()));

        ComponentUtil.setData(vaadinTab, WebTabImpl.class, webTab);
        toVaadinComponent().add(vaadinTab, vaadinContent, position);
        myTabs.put(vaadinTab, webTab);

        if (getDragAndDropHandler() != null) {
            installTabDragAndDrop(webTab);
        }

        applyEnabled(webTab);

        webTab.setContent(component);

        if (first) {
            webTab.select();
        }
        return tab;
    }

    @Override
    @RequiredUIAccess
    public void moveTab(Tab tab, int index) {
        WebTabImpl webTab = (WebTabImpl) tab;
        com.vaadin.flow.component.tabs.Tab vaadinTab = webTab.getVaadinTab();
        Vaadin sheet = toVaadinComponent();
        if (vaadinTab == null || !myTabs.containsKey(vaadinTab)) {
            return;
        }

        int from = sheet.getIndexOf(vaadinTab);
        int to = Math.max(0, Math.min(index, sheet.getTabCount() - 1));
        if (from == -1 || from == to) {
            return;
        }

        com.vaadin.flow.component.Component content = sheet.getComponent(vaadinTab);
        boolean selected = sheet.getSelectedTab() == vaadinTab;

        myMovingTab = true;
        try {
            sheet.remove(vaadinTab);
            sheet.add(vaadinTab, content, to);
            if (selected) {
                sheet.setSelectedTab(vaadinTab);
            }
        }
        finally {
            myMovingTab = false;
        }
    }

    private List<WebTabImpl> orderedTabs() {
        Vaadin sheet = toVaadinComponent();
        List<WebTabImpl> tabs = new ArrayList<>(sheet.getTabCount());
        for (int i = 0; i < sheet.getTabCount(); i++) {
            WebTabImpl tab = myTabs.get(sheet.getTabAt(i));
            if (tab != null) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Tab> handler) {
        myTransferHandler = handler;

        boolean enabled = getDragAndDropHandler() != null;
        for (WebTabImpl tab : orderedTabs()) {
            if (enabled) {
                installTabDragAndDrop(tab);
            }
            else {
                tab.getVaadinTab().getElement().removeAttribute("draggable");
            }
        }

        if (enabled) {
            installStripDrop();
        }
    }

    @Override
    public @Nullable TransferHandler<Tab> getTransferHandler() {
        return myTransferHandler;
    }

    private @Nullable DragAndDropTransferHandler<Tab> getDragAndDropHandler() {
        return myTransferHandler instanceof DragAndDropTransferHandler<Tab> handler ? handler : null;
    }

    private boolean isHorizontal() {
        return myTabPosition == StaticPosition.TOP || myTabPosition == StaticPosition.BOTTOM;
    }

    private String beforeExpression() {
        return isHorizontal()
            ? "event.clientX < element.getBoundingClientRect().left + element.getBoundingClientRect().width / 2"
            : "event.clientY < element.getBoundingClientRect().top + element.getBoundingClientRect().height / 2";
    }

    private void installTabDragAndDrop(WebTabImpl webTab) {
        Element element = webTab.getVaadinTab().getElement();
        element.setAttribute("draggable", "true");

        if (webTab.isDragAndDropInstalled()) {
            return;
        }
        webTab.setDragAndDropInstalled(true);

        String before = beforeExpression();

        element.addEventListener("dragstart", event -> onDragStart(webTab))
            .addEventData("event.dataTransfer.setData('" + WebDragSession.DRAG_TYPE + "', '')")
            .addEventData("event.dataTransfer.effectAllowed = 'copyMove'");

        element.addEventListener("dragend", event -> onDragEnd(webTab));

        element.addEventListener("drop", event -> onDrop(webTab, event.getEventData().path(before).asBoolean(false)))
            .addEventData(before)
            .addEventData("event.preventDefault()")
            .addEventData("event.stopPropagation()");

        String script = "if (this.__consuloTabDrop) { return; } this.__consuloTabDrop = true;"
            + "const clear = () => { this.removeAttribute('consulo-drop-before'); this.removeAttribute('consulo-drop-after'); };"
            + "this.addEventListener('dragover', e => {"
            + "  if (!e.dataTransfer.types.includes('" + WebDragSession.DRAG_TYPE + "')) { return; }"
            + "  e.preventDefault(); e.stopPropagation();"
            + "  const element = this; const event = e; const before = " + before + ";"
            + "  this.toggleAttribute('consulo-drop-before', before); this.toggleAttribute('consulo-drop-after', !before);"
            + "});"
            + "this.addEventListener('dragleave', clear); this.addEventListener('drop', clear); this.addEventListener('dragend', clear);";

        element.executeJs(script);
        element.addAttachListener(event -> element.executeJs(script));
    }

    private void installStripDrop() {
        if (myStripDropInstalled) {
            return;
        }
        myStripDropInstalled = true;

        Element strip = getTabs(toVaadinComponent()).getElement();

        strip.addEventListener("drop", event -> onStripDrop())
            .addEventData("event.preventDefault()")
            .addEventData("event.stopPropagation()");

        String script = "if (this.__consuloStripDrop) { return; } this.__consuloStripDrop = true;"
            + "const last = () => { const tabs = this.querySelectorAll(':scope > vaadin-tab'); return tabs.length ? tabs[tabs.length - 1] : null; };"
            + "this.addEventListener('dragover', e => {"
            + "  if (!e.dataTransfer.types.includes('" + WebDragSession.DRAG_TYPE + "')) { return; }"
            + "  e.preventDefault(); const tab = last(); if (tab) { tab.toggleAttribute('consulo-drop-after', true); }"
            + "});"
            + "const clear = () => { const tab = last(); if (tab) { tab.removeAttribute('consulo-drop-after'); } };"
            + "this.addEventListener('dragleave', clear); this.addEventListener('drop', clear);";

        strip.executeJs(script);
        strip.addAttachListener(event -> strip.executeJs(script));
    }

    private void onDragStart(WebTabImpl webTab) {
        DragAndDropTransferHandler<Tab> handler = getDragAndDropHandler();
        UI ui = toVaadinComponent().getUI().orElse(null);
        if (handler == null || ui == null) {
            return;
        }

        WebDragSession.start(ui, webTab, handler.createDragTransfer(this, List.of(webTab), true));
    }

    private void onDragEnd(WebTabImpl webTab) {
        toVaadinComponent().getUI().ifPresent(ui -> WebDragSession.end(ui, webTab));
    }

    private void onStripDrop() {
        List<WebTabImpl> tabs = orderedTabs();
        if (!tabs.isEmpty()) {
            performDrop(tabs.get(tabs.size() - 1), false);
        }
    }

    private void onDrop(WebTabImpl target, boolean before) {
        performDrop(target, before);
    }

    private void performDrop(WebTabImpl target, boolean before) {
        DragAndDropTransferHandler<Tab> handler = getDragAndDropHandler();
        UI ui = toVaadinComponent().getUI().orElse(null);
        Object source = ui == null ? null : WebDragSession.getSource(ui);
        if (handler == null || source == null) {
            return;
        }

        List<Tab> items = source instanceof WebTabImpl tab && tab.getTabbedLayout() == this ? List.of(tab) : List.of();
        DragAndDropTransferHandler.DropPosition position = before
            ? DragAndDropTransferHandler.DropPosition.ABOVE
            : DragAndDropTransferHandler.DropPosition.BELOW;

        WebDropContext<Tab> context = new WebDropContext<>(target, position, WebDragSession.getTransfer(ui), items, true);
        if (handler.drop(this, context)) {
            handler.drop(this, context.toPerforming());
        }
    }

    void applyEnabled(WebTabImpl webTab) {
        com.vaadin.flow.component.tabs.Tab vaadinTab = webTab.getVaadinTab();
        if (vaadinTab == null || !myTabs.containsKey(vaadinTab)) {
            return;
        }

        boolean enabled = webTab.isEnabled();
        if (!enabled && toVaadinComponent().getSelectedTab() == vaadinTab) {
            List<WebTabImpl> tabs = orderedTabs();
            int target = TabSelectionUtil.findSelectionOnDisable(tabs.indexOf(webTab), tabs.size(), i -> tabs.get(i).isEnabled());
            if (target != -1) {
                tabs.get(target).select();
            }
        }

        vaadinTab.setEnabled(enabled);
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
        toVaadinComponent().setPrefixComponent(prefixComponent == null ? null : toStripComponent(prefixComponent));
    }

    /**
     * What sits beside the tabs takes the size of its content. A layout is 100% wide by default, which on the strip
     * eats the room of the tabs themselves until they fall behind the overflow arrow.
     */
    private static com.vaadin.flow.component.Component toStripComponent(Component component) {
        com.vaadin.flow.component.Component vaadinComponent = TargetVaadin.to(component);
        vaadinComponent.getElement().getStyle().set("width", "auto");
        return vaadinComponent;
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        toVaadinComponent().setSuffixComponent(suffixComponent == null ? null : toStripComponent(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public void forEachTab(@RequiredUIAccess BiConsumer<Tab, Component> consumer) {
        for (WebTabImpl tab : orderedTabs()) {
            Component content = tab.getContent();
            if (content != null) {
                consumer.accept(tab, content);
            }
        }
    }

    @Override
    public @Nullable Tab getSelectedTab() {
        com.vaadin.flow.component.tabs.Tab selected = toVaadinComponent().getSelectedTab();
        WebTabImpl tab = selected == null ? null : myTabs.get(selected);
        if (tab != null) {
            return tab;
        }
        List<WebTabImpl> tabs = orderedTabs();
        return tabs.isEmpty() ? null : tabs.get(0);
    }

    @Override
    public void removeTab(Tab tab) {
        WebTabImpl webTab = (WebTabImpl) tab;

        com.vaadin.flow.component.tabs.Tab vaadinTab = webTab.getVaadinTab();
        if (vaadinTab != null) {
            // the sheet drops the content of the tab along with it
            toVaadinComponent().remove(vaadinTab);
            myTabs.remove(vaadinTab);
        }
    }
}
