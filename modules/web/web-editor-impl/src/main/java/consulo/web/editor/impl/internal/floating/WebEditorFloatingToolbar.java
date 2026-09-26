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
package consulo.web.editor.impl.internal.floating;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.internal.floating.EditorFloatingToolbarInstaller;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.editor.impl.internal.ArquillEditorElement;

public final class WebEditorFloatingToolbar {
    private WebEditorFloatingToolbar() {
    }

    @RequiredUIAccess
    public static void install(Editor editor, Project project, ArquillEditorElement element, Disposable parentDisposable) {
        Div layer = element.getFloatingToolbarLayer();

        EditorFloatingToolbarInstaller.install(editor, project, parentDisposable, (provider, dataContext, providerDisposable) -> {
            WebFloatingToolbarComponent component = new WebFloatingToolbarComponent(provider, editor);

            Div holder = component.getHolder();
            layer.add(holder);
            Disposer.register(providerDisposable, () -> layer.remove(holder));

            Registration escapeRegistration = element.addFloatingEscapeListener(event -> {
                component.hideImmediately();
                provider.onHiddenByEsc(editor.getDataContext());
            });
            Disposer.register(providerDisposable, escapeRegistration::remove);

            return component;
        });
    }
}
