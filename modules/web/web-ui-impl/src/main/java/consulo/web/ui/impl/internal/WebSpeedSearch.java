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

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.SpeedSearchSupport;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class WebSpeedSearch<Item> {
    private static final String ENABLED_ATTRIBUTE = "consulo-speed-search";
    private static final String EVENT = "consulo-speed-search";
    private static final String ACTION_DATA = "event.detail.action";
    private static final String TEXT_DATA = "event.detail.text";

    private static final String INSTALL = """
        const host = this;
        const run = () => window.consuloSpeedSearch.install(host);
        if (window.consuloSpeedSearch) {
            run();
            return;
        }

        let script = document.getElementById('consulo-speed-search-script');
        if (!script) {
            script = document.createElement('script');
            script.id = 'consulo-speed-search-script';
            script.src = '/speedSearch/speedSearch.js';
            document.head.appendChild(script);
        }
        script.addEventListener('load', run, { once: true });
        """;

    private final Element myHost;
    private final SpeedSearchSupport<Item> mySupport;
    private final Supplier<List<Item>> myItems;
    private final Supplier<@Nullable Item> mySelected;
    private final Consumer<Item> mySelect;

    public WebSpeedSearch(Element host,
                          boolean camelCaseWords,
                          Supplier<List<Item>> items,
                          Supplier<@Nullable Item> selected,
                          Consumer<Item> select) {
        myHost = host;
        mySupport = new SpeedSearchSupport<>(camelCaseWords);
        myItems = items;
        mySelected = selected;
        mySelect = select;

        host.addEventListener(EVENT, this::onEvent)
            .addEventData(ACTION_DATA)
            .addEventData(TEXT_DATA);

        host.addAttachListener(event -> {
            if (mySupport.isEnabled()) {
                host.executeJs(INSTALL);
            }
        });
    }

    public void setConverter(@Nullable Function<Item, String> converter) {
        boolean wasEnabled = mySupport.isEnabled();
        mySupport.setConverter(converter);

        if (converter != null) {
            myHost.setAttribute(ENABLED_ATTRIBUTE, true);
            if (!wasEnabled) {
                myHost.executeJs(INSTALL);
            }
        }
        else if (wasEnabled) {
            myHost.removeAttribute(ENABLED_ATTRIBUTE);
            myHost.executeJs("if (this.$consuloSpeedSearch) { this.$consuloSpeedSearch.close(); }");
        }
    }

    public @Nullable String getText() {
        return mySupport.getText();
    }

    @RequiredUIAccess
    private void onEvent(DomEvent event) {
        if (!mySupport.isEnabled()) {
            return;
        }

        String action = event.getEventData().path(ACTION_DATA).asString("");
        String text = event.getEventData().path(TEXT_DATA).asString("");
        if (action.equals("close")) {
            mySupport.setText(null);
            return;
        }

        mySupport.setText(text);

        List<Item> items = myItems.get();
        Item selected = mySelected.get();
        int selectedIndex = SpeedSearchSupport.indexOf(items, selected);

        Item found = switch (action) {
            case "next" -> mySupport.findNext(items, selectedIndex);
            case "previous" -> mySupport.findPrevious(items, selectedIndex);
            case "first" -> mySupport.findFirst(items);
            case "last" -> mySupport.findLast(items);
            default -> mySupport.find(items, selectedIndex);
        };

        if (found != null && !Objects.equals(found, selected)) {
            mySelect.accept(found);
        }

        myHost.executeJs("if (this.$consuloSpeedSearch) { this.$consuloSpeedSearch.found($0, $1); }", text, found != null);
    }
}
