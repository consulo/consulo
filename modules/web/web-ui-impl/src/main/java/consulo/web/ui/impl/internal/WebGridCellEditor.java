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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class WebGridCellEditor<Item, Value> {
    private static final String FOCUS_LEAVES = "!(event.relatedTarget && element.contains(event.relatedTarget))";

    private final TableItemEditor<Item, Value> myEditor;
    private final Item myItem;
    private final ValueComponent<Value> myComponent;
    private final Component myVaadinComponent;
    private final Runnable myEditFinished;

    private boolean myCommitPending;

    WebGridCellEditor(TableItemEditor<Item, Value> editor, Item item, @Nullable Value value, Runnable editFinished) {
        myEditor = editor;
        myItem = item;
        myEditFinished = editFinished;

        myComponent = editor.createComponent(item);
        myComponent.setValue(value, false);
        myComponent.addValueListener(event -> {
            editor.commit(item, event.getValue());
            myCommitPending = true;
        });

        myVaadinComponent = ((ToVaadinComponentWrapper) myComponent).toVaadinComponent();

        Element element = myVaadinComponent.getElement();
        element.addEventListener("change", event -> finishEdit());
        element.addEventListener("focusout", event -> finishEdit()).setFilter(FOCUS_LEAVES);
    }

    Component getVaadinComponent() {
        return myVaadinComponent;
    }

    boolean isEditing(@Nullable TableItemEditor<Item, Value> editor, Item item) {
        return myEditor == editor && myItem == item && myEditor.isEditable(item);
    }

    void showValue(@Nullable Value value) {
        if (!myCommitPending && !Objects.equals(myComponent.getValue(), value)) {
            myComponent.setValue(value, false);
        }
    }

    private void finishEdit() {
        myVaadinComponent.getUI().ifPresent(ui -> ui.beforeClientResponse(ui, context -> {
            if (myCommitPending) {
                myCommitPending = false;
                myEditFinished.run();
            }
        }));
    }
}
