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
package consulo.task.impl.internal.setting;

import consulo.configurable.MasterDetailsConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.ui.TaskRepositoryEditor;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class TaskRepositoryDraftConfigurable implements MasterDetailsConfigurable<TaskRepositoryDraft> {
    private final Project myProject;
    private final TaskRepositoryDraft myDraft;
    private final Function<TaskRepositoryDraft, String> myText;
    private final Runnable myUpdateTree;

    TaskRepositoryDraftConfigurable(Project project, TaskRepositoryDraft draft, Function<TaskRepositoryDraft, String> text, Runnable updateTree) {
        myProject = project;
        myDraft = draft;
        myText = text;
        myUpdateTree = updateTree;
    }

    @Override
    public @Nullable Image getIcon() {
        return myDraft.type.getIcon();
    }

    @Override
    public TaskRepositoryDraft getEditableObject() {
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
        TaskRepositoryEditor editor = myDraft.type.createEditorUnchecked(myDraft.options, myProject, options -> myUpdateTree.run());
        Disposer.register(parentDisposable, editor);
        return editor.createComponent();
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
}
