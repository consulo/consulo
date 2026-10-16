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
package consulo.web.editor.impl.internal;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.dom.Element;
import consulo.codeEditor.EditorFactory;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.project.ProjectManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.List;

class WebEditorComboBoxImpl extends WebEditorBoxBase<WebEditorComboBoxImpl.Vaadin> implements EditorComboBox {
    @StyleSheet("/editorBox/webEditorBox.css")
    public class Vaadin extends Select<String> implements FromVaadinComponentWrapper {
        @Override
        public Component toUIComponent() {
            return WebEditorComboBoxImpl.this;
        }
    }

    private static final String EDITOR_CLASS = "consulo-editor-combo-box-editor";
    private static final String SUFFIX_CLASS = "consulo-editor-combo-box-suffix";

    private List<String> myItems = List.of();

    private @Nullable Component mySuffixComponent;

    private boolean mySyncing;

    @RequiredUIAccess
    WebEditorComboBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        super(options, editorFactory, projectManager);

        Vaadin vaadin = toVaadinComponent();
        vaadin.getElement().getClassList().add("consulo-editor-combo-box");
        vaadin.getElement().getClassList().set("consulo-editor-box-editor-font", options.editorFont());
        vaadin.setNoVerticalOverlap(true);
        vaadin.addValueChangeListener(event -> {
            if (!mySyncing && event.isFromClient()) {
                itemChosen(event.getValue());
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    private void itemChosen(@Nullable String item) {
        if (item != null) {
            setValue(item);
            mySupport.moveCaretToEnd();
        }
        focus();
    }

    @Override
    protected void insertEditorElement(Element container, Element editorElement) {
        editorElement.setAttribute("slot", "prefix");
        editorElement.getClassList().add(EDITOR_CLASS);
        container.insertChild(0, editorElement);

        editorElement.addEventListener("click", event -> {
        }).addEventData("event.stopPropagation()");
    }

    @RequiredUIAccess
    @Override
    protected void valueChanged() {
        syncSelection();

        super.valueChanged();
    }

    private void syncSelection() {
        String selected = getSelectedItem();
        Vaadin vaadin = toVaadinComponent();
        mySyncing = true;
        try {
            vaadin.setValue(selected);
        }
        finally {
            mySyncing = false;
        }
    }

    @Override
    public List<String> getItems() {
        return myItems;
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<String> items) {
        myItems = List.copyOf(items);

        mySyncing = true;
        try {
            toVaadinComponent().setItems(myItems);
        }
        finally {
            mySyncing = false;
        }

        syncSelection();
    }

    @Override
    public @Nullable String getSelectedItem() {
        String value = getValue();
        return myItems.contains(value) ? value : null;
    }

    @RequiredUIAccess
    @Override
    public void setSelectedItem(@Nullable String item) {
        setValue(item);
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Element container = toVaadinComponent().getElement();

        Component oldSuffix = mySuffixComponent;
        if (oldSuffix != null) {
            container.removeChild(((ToVaadinComponentWrapper) oldSuffix).toVaadinComponent().getElement());
        }

        mySuffixComponent = suffixComponent;

        if (suffixComponent != null) {
            Element suffixElement = ((ToVaadinComponentWrapper) suffixComponent).toVaadinComponent().getElement();
            suffixElement.setAttribute("slot", "prefix");
            suffixElement.getClassList().add(SUFFIX_CLASS);
            suffixElement.addEventListener("click", event -> {
            }).addEventData("event.stopPropagation()");
            container.appendChild(suffixElement);
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }
}
