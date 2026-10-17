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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.MemoryIndicator;
import consulo.ui.style.ComponentColors;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.SimpleComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class WebMemoryIndicatorImpl extends VaadinComponentDelegate<WebMemoryIndicatorImpl.Vaadin> implements MemoryIndicator {
    @Tag("div")
    @StyleSheet("/memoryIndicator/webMemoryIndicator.css")
    public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMemoryIndicatorImpl.this;
        }
    }

    private final Div myAllocated = new Div();
    private final Div myUsed = new Div();
    private final Span myText = new Span();

    public WebMemoryIndicatorImpl(LocalizeValue widestText) {
        Vaadin vaadin = getVaadinComponent();
        vaadin.addClassName("consulo-memory-indicator");
        vaadin.getStyle().set("border-color", WebColors.toStyleCssColor(ComponentColors.BORDER));
        vaadin.getStyle().set("--consulo-memory-indicator-hover", WebColors.toStyleCssColor(ComponentColors.HOVER_BACKGROUND));

        myAllocated.addClassName("consulo-memory-indicator-allocated");
        myAllocated.getStyle().set("background-color", WebColors.toStyleCssColor(ComponentColors.MEMORY_INDICATOR_ALLOCATED_BACKGROUND));

        myUsed.addClassName("consulo-memory-indicator-used");
        myUsed.getStyle().set("background-color", WebColors.toStyleCssColor(ComponentColors.MEMORY_INDICATOR_USED_BACKGROUND));

        Span widest = new Span(widestText.get());
        widest.addClassName("consulo-memory-indicator-widest");

        myText.addClassName("consulo-memory-indicator-text");

        vaadin.getElement().appendChild(myAllocated.getElement(), myUsed.getElement(), widest.getElement(), myText.getElement());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Override
    public void setUsage(long used, long allocated, long max, LocalizeValue text) {
        myAllocated.getStyle().set("width", percent(allocated, max));
        myUsed.getStyle().set("width", percent(used, max));
        myText.setText(text.get());
    }

    private static String percent(long value, long max) {
        if (max <= 0) {
            return "0";
        }
        return Math.min(value, max) * 100.0 / max + "%";
    }
}
