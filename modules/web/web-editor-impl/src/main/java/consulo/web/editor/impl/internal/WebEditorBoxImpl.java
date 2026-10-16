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

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.dom.Element;
import consulo.codeEditor.EditorFactory;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.project.ProjectManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

class WebEditorBoxImpl extends WebEditorBoxBase<WebEditorBoxImpl.Vaadin> {
    @Tag("vaadin-input-container")
    @StyleSheet("/editorBox/webEditorBox.css")
    public class Vaadin extends com.vaadin.flow.component.Component implements FromVaadinComponentWrapper, HasSize {
        @Override
        public Component toUIComponent() {
            return WebEditorBoxImpl.this;
        }
    }

    private @Nullable Component mySuffixComponent;

    @RequiredUIAccess
    WebEditorBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        super(options, editorFactory, projectManager);

        Vaadin vaadin = toVaadinComponent();
        vaadin.getElement().getClassList().add("consulo-editor-box");
        vaadin.getElement().getClassList().add("aura-surface");
        vaadin.getElement().getClassList().set("consulo-editor-box-multiline", !options.oneLine());
        vaadin.getElement().getClassList().set("consulo-editor-box-editor-font", options.editorFont());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    protected void insertEditorElement(Element container, Element editorElement) {
        container.insertChild(0, editorElement);
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
            suffixElement.setAttribute("slot", "suffix");
            container.appendChild(suffixElement);
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }
}
