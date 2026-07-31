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
package consulo.web.internal.ui.base;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.web.internal.ui.WebFocusManagerImpl;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The browser has no equivalent of the awt focus owner that the data context is built from - vaadin only reports
 * focus for the few components that are focusable by themselves, and the editor and the trees are not among them.
 * <p/>
 * Every component that publishes data marks itself as a focus scope, and one listener on the frame root reports
 * which scope the pointer went down in, or which one took dom focus. That scope is what the data context is taken
 * from, the way {@code BaseDataManager#getDataContextTest} uses the awt focus owner.
 *
 * @author VISTALL
 */
public final class WebFocusTracker {
    private static final String SCOPE_ATTRIBUTE = "consulo-focus-scope";

    /**
     * Resolved in the browser rather than by filtering per scope - a listener on every scope would make each
     * enclosing one report itself as well, and the outermost, handled last, would win.
     */
    private static final String SCOPE_OF_EVENT =
        "(event.target.closest('[" + SCOPE_ATTRIBUTE + "]') || {getAttribute: () => ''}).getAttribute('"
            + SCOPE_ATTRIBUTE + "')";

    private static final String EXCLUDED_KEY = "consulo-focus-scope-excluded";

    /** one report per interaction is enough, and it keeps a drag from flooding the server */
    private static final int DEBOUNCE_MS = 100;

    /**
     * Held on the vaadin ui, not statically - a browser frontend serves several uis at once, and the focus of one
     * of them says nothing about the others.
     */
    private static final String FOCUSED_KEY = "consulo-focus-scope-focused";

    private static final Map<String, Component> ourScopes = new ConcurrentHashMap<>();

    private WebFocusTracker() {
    }

    /**
     * Keeps the component out of the focus scopes. Used for the frame root - it holds the project data provider,
     * which is reachable by walking up the parents anyway, and making it a scope would drop the editor context
     * every time the menu bar or the navigation bar is clicked.
     */
    public static void exclude(Component component) {
        if (component instanceof ToVaadinComponentWrapper wrapper) {
            ComponentUtil.setData(wrapper.toVaadinComponent(), EXCLUDED_KEY, Boolean.TRUE);
        }
    }

    public static void register(VaadinComponentDelegate<?> delegate) {
        com.vaadin.flow.component.Component vaadinComponent = delegate.toVaadinComponent();

        if (Boolean.TRUE.equals(ComponentUtil.getData(vaadinComponent, EXCLUDED_KEY))) {
            return;
        }

        String id = vaadinComponent.getId().orElse(null);
        if (id == null) {
            return;
        }

        ourScopes.put(id, delegate);

        // a session lives as long as its browser tab, the map must not keep growing with every closed frame
        vaadinComponent.addDetachListener(event -> ourScopes.remove(id));

        vaadinComponent.getElement().setAttribute(SCOPE_ATTRIBUTE, id);
    }

    /**
     * Installs the single listener that reports the focused scope. Must be called on the frame root, everything
     * the frame holds is below it and pointer and focus events both bubble.
     */
    public static void installRoot(Component root) {
        if (!(root instanceof ToVaadinComponentWrapper wrapper)) {
            return;
        }

        Element element = wrapper.toVaadinComponent().getElement();

        for (String eventType : new String[]{"mousedown", "focusin"}) {
            element.addEventListener(eventType, event -> setFocusedScope(event.getEventData().path(SCOPE_OF_EVENT).asString("")))
                .addEventData(SCOPE_OF_EVENT)
                .debounce(DEBOUNCE_MS, DebouncePhase.TRAILING);
        }
    }

    private static void setFocusedScope(String id) {
        if (id.isEmpty()) {
            return;
        }

        setFocusedComponent(ourScopes.get(id));
    }

    public static void setFocusedComponent(@Nullable Component component) {
        UI ui = UI.getCurrent();
        if (component == null || ui == null) {
            return;
        }

        // the scopes are keyed by component id across the whole application, a scope of another ui - another
        // project, another browser tab - must never become the focus of this one
        if (component instanceof ToVaadinComponentWrapper wrapper && wrapper.toVaadinComponent().getUI().orElse(null) != ui) {
            return;
        }

        ComponentUtil.setData(ui, FOCUSED_KEY, component);

        // a tree or a grid carries its selection in the same client message as the pointer event and vaadin
        // applies it after the dom listeners, so a listener firing here would still read the previous selection.
        // beforeClientResponse runs once everything of the message is in.
        //
        // fired even when the scope did not change - what the listeners read is the context of the scope, and
        // clicking another row of the same tree changes that context without changing the scope
        ui.beforeClientResponse(ui, context -> WebFocusManagerImpl.ourInstance.fireChanged());
    }

    /**
     * Context of the scope the user last interacted with, falling back to the given component when there was no
     * interaction yet. {@code BaseDataManager#getDataContextTest} cannot be used here - it asks the window manager
     * for an awt focus owner, which the browser frontend never has.
     */
    public static DataContext createDataContext(Component fallback) {
        Component component = getFocusedComponent();

        return DataManager.getInstance().getDataContext(component == null ? fallback : component);
    }

    public static @Nullable Component getFocusedComponent() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return null;
        }

        Component component = (Component)ComponentUtil.getData(ui, FOCUSED_KEY);
        if (component instanceof ToVaadinComponentWrapper wrapper && wrapper.toVaadinComponent().getUI().isEmpty()) {
            // the scope was detached, its data provider would report an editor which is already closed
            ComponentUtil.setData(ui, FOCUSED_KEY, null);
            return null;
        }
        return component;
    }
}
