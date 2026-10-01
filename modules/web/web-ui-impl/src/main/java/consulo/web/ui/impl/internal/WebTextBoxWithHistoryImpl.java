/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author VISTALL
 * @since 2020-08-25
 */
public class WebTextBoxWithHistoryImpl extends WebTextBoxImpl implements TextBoxWithHistory {
    private static final String ITEM_INDEX =
        "(element.items || []).indexOf(event.composedPath().find(node => node.localName === 'vaadin-item'))";

    private static final String INSTALL_POPUP = """
        const field = $0;
        const popup = this;
        field.__consuloHistoryPopup = popup;
        popup.__consuloHistoryField = field;

        if (!popup.__consuloHistoryClosed) {
            popup.__consuloHistoryClosed = true;
            popup.addEventListener('closed', () => {
                const active = document.activeElement;
                if (!active || active === document.body || popup.contains(active)) {
                    popup.__consuloHistoryField.focus();
                }
            });

            popup.addEventListener('focusout', event => {
                const owner = popup.__consuloHistoryField;
                const next = event.relatedTarget;
                if (popup.opened && !(next && (popup.contains(next) || owner.contains(next)))) {
                    popup.opened = false;
                }
            });
        }

        if (!field.__consuloHistoryFocusOut) {
            field.__consuloHistoryFocusOut = true;
            field.addEventListener('focusout', event => {
                const current = field.__consuloHistoryPopup;
                const next = event.relatedTarget;
                if (current && current.opened && !(next && (current.contains(next) || field.contains(next)))) {
                    current.opened = false;
                }
            });
        }
        """;

    private static final String INSTALL_TOGGLE = """
        const field = $0;
        const toggle = this;
        toggle.__consuloHistoryField = field;

        if (!toggle.__consuloHistoryMouseDown) {
            toggle.__consuloHistoryMouseDown = true;
            toggle.addEventListener('mousedown', event => {
                event.preventDefault();
                toggle.__consuloHistoryField.focus();
            });
        }
        """;

    private final Span mySuffixHolder = new Span();
    private final Div myToggle = new Div();

    private final ListBox<Integer> myList = new ListBox<>();
    private final Popover myPopup = new Popover(myList);

    private List<String> myHistory = List.of();

    @RequiredUIAccess
    public WebTextBoxWithHistoryImpl(String text) {
        super(text);

        Vaadin field = toVaadinComponent();

        mySuffixHolder.addClassName("consulo-history-suffix");

        myToggle.addClassName("consulo-history-toggle");
        myToggle.addClickListener(event -> toggleHistory());
        myToggle.addAttachListener(event -> myToggle.getElement().executeJs(INSTALL_TOGGLE, field.getElement()));

        myList.addClassName("consulo-history-list");
        myList.setItemLabelGenerator(index -> index >= 0 && index < myHistory.size() ? myHistory.get(index) : "");
        myList.getElement()
            .addEventListener("click", event -> {
                int index = event.getEventData().path(ITEM_INDEX).asInt(-1);
                if (index >= 0 && index < myHistory.size()) {
                    pick(myHistory.get(index));
                }
            })
            .addEventData(ITEM_INDEX);

        myPopup.getElement().getClassList().add("consulo-history-popup");
        myPopup.setPosition(PopoverPosition.BOTTOM_START);
        myPopup.setOpenOnClick(false);
        myPopup.setOpenOnFocus(false);
        myPopup.setOpenOnHover(false);
        myPopup.setCloseOnEsc(true);
        myPopup.setCloseOnOutsideClick(true);
        myPopup.setModal(false);
        myPopup.setAutofocus(false);
        myPopup.addAttachListener(event -> myPopup.getElement().executeJs(INSTALL_POPUP, field.getElement()));
        myPopup.setTarget(field);

        field.addDetachListener(event -> myPopup.setOpened(false));

        field.getElement()
            .addEventListener("keydown", event -> showHistory(true))
            .setFilter("event.key === 'ArrowDown' && !event.ctrlKey && !event.metaKey && !event.shiftKey");

        updateSuffixSlot();
    }

    @Override
    protected void updateSuffixSlot() {
        mySuffixHolder.removeAll();

        com.vaadin.flow.component.Component suffix = toVaadinOrNull(getSuffixComponent());
        if (suffix != null) {
            mySuffixHolder.add(suffix);
        }
        mySuffixHolder.add(myToggle);

        toVaadinComponent().setSuffixComponent(mySuffixHolder);
    }

    @RequiredUIAccess
    @Override
    public TextBoxWithHistory setHistory(List<String> history) {
        List<String> items = new ArrayList<>(history.size());
        for (String item : history) {
            items.add(StringUtil.notNullize(item));
        }
        myHistory = items;

        myList.setItems(IntStream.range(0, items.size()).boxed().toList());

        if (items.isEmpty()) {
            myPopup.setOpened(false);
        }
        return this;
    }

    @RequiredUIAccess
    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);

        if (!editable) {
            myPopup.setOpened(false);
        }
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);

        if (!value) {
            myPopup.setOpened(false);
        }
    }

    @RequiredUIAccess
    private void toggleHistory() {
        if (myPopup.isOpened()) {
            myPopup.setOpened(false);
            return;
        }

        showHistory(false);
    }

    @RequiredUIAccess
    private void showHistory(boolean focusList) {
        Vaadin field = toVaadinComponent();
        if (myHistory.isEmpty() || field.isReadOnly() || !field.isEnabled()) {
            return;
        }

        int current = myHistory.indexOf(StringUtil.notNullize(getValue()));
        myList.setValue(current >= 0 ? current : null);

        if (!myPopup.isOpened()) {
            if (!myPopup.isAttached()) {
                myPopup.setTarget(field);
            }

            myPopup.getElement().executeJs(
                "this.style.setProperty('--consulo-history-min-width', Math.round($0.getBoundingClientRect().width) + 'px')",
                field.getElement()
            );
            myPopup.setOpened(true);
        }

        if (focusList) {
            myList.getElement().executeJs("requestAnimationFrame(() => requestAnimationFrame(() => this.focus()))");
        }
    }

    @RequiredUIAccess
    private void pick(String entry) {
        myPopup.setOpened(false);

        if (!isEditable() || !isEnabled()) {
            return;
        }

        setValue(entry, true);
    }
}
