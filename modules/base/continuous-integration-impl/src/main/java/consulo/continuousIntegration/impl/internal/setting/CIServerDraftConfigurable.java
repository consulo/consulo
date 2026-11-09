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
package consulo.continuousIntegration.impl.internal.setting;

import consulo.configurable.MasterDetailsConfigurable;
import consulo.continuousIntegration.ui.CIServerEditor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Details of one server: the editor of its type, changing the options of the draft in place.
 *
 * @since 2026-11-09
 */
final class CIServerDraftConfigurable implements MasterDetailsConfigurable<CIServerDraft> {
    private final Project myProject;
    private final CIServerDraft myDraft;
    private final Function<CIServerDraft, String> myText;
    private final Runnable myUpdateTree;
    private @Nullable CIServerEditor myEditor;
    private @Nullable Component myComponent;

    CIServerDraftConfigurable(Project project, CIServerDraft draft, Function<CIServerDraft, String> text, Runnable updateTree) {
        myProject = project;
        myDraft = draft;
        myText = text;
        myUpdateTree = updateTree;
    }

    @Override
    public @Nullable Image getIcon() {
        return myDraft.getType().getIcon();
    }

    @Override
    public CIServerDraft getEditableObject() {
        return myDraft;
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return LocalizeValue.of(myText.apply(myDraft));
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myText.apply(myDraft));
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        CIServerEditor editor = myDraft.getType().createEditorUnchecked(myDraft.getOptions(), myProject, options -> myUpdateTree.run());
        Disposer.register(parentDisposable, editor);
        component = editor.createComponent();
        myEditor = editor;
        myComponent = component;
        return component;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        CIServerEditor editor = myEditor;
        return editor == null ? null : editor.getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        CIServerEditor editor = myEditor;
        if (editor != null) {
            Disposer.dispose(editor);
        }
        myEditor = null;
        myComponent = null;
    }
}
