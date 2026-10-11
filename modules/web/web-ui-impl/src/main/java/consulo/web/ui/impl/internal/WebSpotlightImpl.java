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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.BaseSpotlight;
import consulo.ui.layout.Layout;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class WebSpotlightImpl extends BaseSpotlight {
    private static final String TARGET_ATTRIBUTE = "data-consulo-spotlight";
    private static final String LIT_ATTRIBUTE = "data-consulo-spotlight-lit";
    private static final String SHAPE_ATTRIBUTE = "data-consulo-spotlight-shape";
    private static final String SECTION_SHAPE = "section";
    private static final String TAB_SHAPE = "tab";

    private static final AtomicLong ourNextId = new AtomicLong();

    private final String myId = "spotlight" + ourNextId.incrementAndGet();

    private final @Nullable Element myHost;

    private final List<Element> myMarked = new ArrayList<>();

    private @Nullable Registration myAttachRegistration;

    @RequiredUIAccess
    public WebSpotlightImpl(Layout<?> layout) {
        super(layout);

        myHost = toElement(layout);

        Element host = myHost;
        if (host == null || isDisposed()) {
            return;
        }

        myAttachRegistration = host.addAttachListener(event -> attachClient(host));

        if (host.getNode().isAttached()) {
            attachClient(host);
        }
    }

    private static @Nullable Element toElement(Component component) {
        return component instanceof ToVaadinComponentWrapper wrapper ? wrapper.toVaadinComponent().getElement() : null;
    }

    private void attachClient(Element host) {
        host.executeJs("window.consuloSpotlight.attach(this, $0)", myId);
    }

    @RequiredUIAccess
    @Override
    protected void onTargetsChanged(List<Component> oldTargets, List<Component> newTargets) {
        Element host = myHost;
        if (host == null) {
            return;
        }

        Map<Element, @Nullable String> shapes = new LinkedHashMap<>();
        for (Component target : newTargets) {
            Element element = toElement(target);
            if (element != null && element != host && !shapes.containsKey(element)) {
                shapes.put(element, target instanceof Layout<?> ? SECTION_SHAPE : null);
            }
        }

        for (Tab tab : getTabTargets()) {
            com.vaadin.flow.component.tabs.Tab vaadinTab = tab instanceof WebTabImpl webTab ? webTab.getVaadinTab() : null;
            if (vaadinTab != null) {
                shapes.putIfAbsent(vaadinTab.getElement(), TAB_SHAPE);
            }
        }

        List<Element> elements = new ArrayList<>(shapes.keySet());

        for (Element element : myMarked) {
            if (!elements.contains(element)) {
                unmark(element);
            }
        }

        for (Element element : elements) {
            if (!myMarked.contains(element)) {
                addToken(element, TARGET_ATTRIBUTE);
            }

            String shape = shapes.get(element);
            if (shape == null) {
                element.removeAttribute(SHAPE_ATTRIBUTE);
            }
            else {
                element.setAttribute(SHAPE_ATTRIBUTE, shape);
            }
        }

        myMarked.clear();
        myMarked.addAll(elements);

        if (!isLit()) {
            removeToken(host, LIT_ATTRIBUTE);
        }
        else {
            addToken(host, LIT_ATTRIBUTE);
        }
    }

    @Override
    protected void onDispose(List<Component> oldTargets) {
        Element host = myHost;
        if (host == null) {
            return;
        }

        for (Element element : myMarked) {
            unmark(element);
        }
        myMarked.clear();

        removeToken(host, LIT_ATTRIBUTE);

        Registration registration = myAttachRegistration;
        myAttachRegistration = null;
        if (registration != null) {
            registration.remove();
        }

        if (host.getNode().isAttached()) {
            host.executeJs("window.consuloSpotlight.detach(this, $0)", myId);
        }
    }

    private void unmark(Element element) {
        removeToken(element, TARGET_ATTRIBUTE);
        if (!element.hasAttribute(TARGET_ATTRIBUTE)) {
            element.removeAttribute(SHAPE_ATTRIBUTE);
        }
    }

    private void addToken(Element element, String attribute) {
        Set<String> tokens = readTokens(element, attribute);
        if (tokens.add(myId)) {
            writeTokens(element, attribute, tokens);
        }
    }

    private void removeToken(Element element, String attribute) {
        Set<String> tokens = readTokens(element, attribute);
        if (tokens.remove(myId)) {
            writeTokens(element, attribute, tokens);
        }
    }

    private static Set<String> readTokens(Element element, String attribute) {
        Set<String> tokens = new LinkedHashSet<>();
        String value = element.getAttribute(attribute);
        if (value != null) {
            for (String token : value.split(" ")) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static void writeTokens(Element element, String attribute, Set<String> tokens) {
        if (tokens.isEmpty()) {
            element.removeAttribute(attribute);
        }
        else {
            element.setAttribute(attribute, String.join(" ", tokens));
        }
    }
}
