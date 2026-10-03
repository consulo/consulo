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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBoxStyle;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.MultiSelectComboBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.vaadin.WebListComponentBase;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class WebMultiSelectComboBoxImpl<E> extends WebListComponentBase<E, WebMultiSelectComboBoxImpl<E>.Vaadin>
    implements MultiSelectComboBox<E> {
    @StyleSheet("/multiSelectComboBox/webMultiSelectComboBox.css")
    public class Vaadin extends com.vaadin.flow.component.combobox.MultiSelectComboBox<E> implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMultiSelectComboBoxImpl.this;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);

            getElement().executeJs(INSTALL_OVERLAY_WIDTH);
        }
    }

    private static final String INSTALL_OVERLAY_WIDTH = """
        const host = this;
        if (host.__consuloOverlayWidth) {
            return;
        }
        host.__consuloOverlayWidth = true;

        const property = '--vaadin-multi-select-combo-box-overlay-width';
        let fitted = 0;

        const fit = () => {
            const overlay = host.$ && host.$.overlay;
            const content = overlay && overlay.$ && overlay.$.overlay;
            const field = host._inputField;
            if (!host.opened || !content || !field) {
                return;
            }

            let widest = 0;
            let chrome = 0;
            for (const item of host.querySelectorAll('vaadin-multi-select-combo-box-item')) {
                if (item.hidden) {
                    continue;
                }
                chrome = content.offsetWidth - item.offsetWidth;
                const width = item.style.width;
                item.style.width = 'max-content';
                widest = Math.max(widest, item.getBoundingClientRect().width);
                item.style.width = width;
            }

            const wanted = Math.ceil(Math.max(field.offsetWidth, widest + chrome, fitted));
            if (wanted !== fitted) {
                fitted = wanted;
                host.style.setProperty(property, wanted + 'px');
                overlay._updatePosition();
            }
        };
        const schedule = () => requestAnimationFrame(fit);
        const observer = new MutationObserver(schedule);

        host.addEventListener('opened-changed', event => {
            observer.disconnect();
            if (!event.detail.value) {
                return;
            }

            fitted = 0;
            host.style.removeProperty(property);
            if (host._scroller) {
                observer.observe(host._scroller, {childList: true, subtree: true, characterData: true});
            }
            schedule();
        });
        """;

    private static final String COMBO_BOX_CLASS = "consulo-multi-select-combo-box";
    private static final String SUMMARY_CLASS = "consulo-multi-select-summary";
    private static final String PLACEHOLDER_CLASS = "consulo-multi-select-placeholder";
    private static final String SUMMARY_PROPERTY = "--consulo-multi-select-summary";

    private boolean mySuppressEvents;
    private @Nullable List<E> myLastValue;
    private @Nullable Function<List<E>, LocalizeValue> mySummaryRenderer;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();

    public WebMultiSelectComboBoxImpl(FlatDataModel<E> model) {
        super(model);

        toVaadinComponent().addValueChangeListener(event -> {
            updateSummary();

            if (!mySuppressEvents) {
                fireIfChanged();
            }
        });

        setRender(TextItemRender.defaultRender());
    }

    @Override
    public Vaadin createVaadinComponent() {
        Vaadin component = new Vaadin();
        component.addClassName(COMBO_BOX_CLASS);
        component.setAutoExpand(com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode.HORIZONTAL);
        return component;
    }

    @Override
    @RequiredUIAccess
    protected void pushItems() {
        Vaadin component = toVaadinComponent();
        Set<E> previous = component.getValue();
        List<E> items = ContainerUtil.collect(myModel.iterator());

        Function<E, String> converter = mySpeedSearchConverter;
        if (converter == null) {
            component.setItems(items);
        }
        else {
            component.setItems((item, filter) -> StringUtil.containsIgnoreCase(converter.apply(item), filter), items);
        }

        Set<E> retained = new LinkedHashSet<>();
        for (E item : items) {
            if (previous.contains(item)) {
                retained.add(item);
            }
        }

        if (!retained.equals(component.getValue())) {
            applySelection(retained);
        }

        updateSummary();

        fireIfChanged();
    }

    @Override
    public void addStyle(ComboBoxStyle style) {
        WebComboBoxStyleUtil.apply(toVaadinComponent(), style);
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;

        Vaadin component = toVaadinComponent();
        component.setItemLabelGenerator(item -> textOf(render, item));
        component.setRenderer(createRenderer(render, this::isSelected));
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        toVaadinComponent().setRenderer(createRenderer(render, this::isSelected));
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
        super.setSpeedSearchConverter(converter);

        pushItems();
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;

        toVaadinComponent().setPlaceholder(text.getNullIfEmpty());
        updateSummary();
    }

    @Override
    public void setSummaryRenderer(Function<List<E>, LocalizeValue> renderer) {
        mySummaryRenderer = renderer;

        toVaadinComponent().addClassName(SUMMARY_CLASS);
        updateSummary();
    }

    private void updateSummary() {
        Function<List<E>, LocalizeValue> renderer = mySummaryRenderer;
        if (renderer == null) {
            return;
        }

        Vaadin component = toVaadinComponent();
        List<E> value = getValue();
        boolean placeholder = value.isEmpty();
        String text = placeholder ? myPlaceholder.get() : renderer.apply(value).get();

        component.setClassName(PLACEHOLDER_CLASS, placeholder);
        if (text.isEmpty()) {
            component.getStyle().remove(SUMMARY_PROPERTY);
        }
        else {
            component.getStyle().set(SUMMARY_PROPERTY, toCssString(text));
        }
    }

    private static String toCssString(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"', '\\' -> builder.append('\\').append(c);
                case '\n', '\r' -> builder.append("\\A ");
                default -> builder.append(c);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    @Override
    public List<E> getValue() {
        Set<E> selection = toVaadinComponent().getValue();
        if (selection.isEmpty()) {
            return List.of();
        }

        List<E> values = new ArrayList<>(selection.size());
        for (E item : myModel) {
            if (selection.contains(item)) {
                values.add(item);
            }
        }
        return values;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        Set<E> selection = new LinkedHashSet<>();
        if (value != null && !value.isEmpty()) {
            Set<E> wanted = new HashSet<>(value);
            for (E item : myModel) {
                if (wanted.contains(item)) {
                    selection.add(item);
                }
            }
        }

        applySelection(selection);

        if (fireListeners) {
            fireIfChanged();
        }
        else {
            myLastValue = getValue();
        }
    }

    private void applySelection(Set<E> selection) {
        mySuppressEvents = true;
        try {
            toVaadinComponent().setValue(selection);
        }
        finally {
            mySuppressEvents = false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireIfChanged() {
        List<E> value = getValue();
        List<E> lastValue = myLastValue;
        if (value.equals(lastValue == null ? List.of() : lastValue)) {
            return;
        }

        myLastValue = value;

        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
    }

    private boolean isSelected(@Nullable E item) {
        return item != null && toVaadinComponent().getValue().contains(item);
    }

    private static <E> String textOf(TextItemRender<E> render, @Nullable E item) {
        WebItemPresentationImpl presentation = new WebItemPresentationImpl();
        render.render(presentation, RenderItem.of(item, false));

        StringBuilder builder = new StringBuilder();
        for (WebItemPresentationImpl.Fragment fragment : presentation.getFragments()) {
            builder.append(fragment.text());
        }
        return builder.toString();
    }
}
