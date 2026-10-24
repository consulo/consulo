// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.create;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;

/**
 * The title and the description of a review being created. The editors are editor boxes bound to the view model -
 * markdown when a markdown language is installed - and live until {@code parentDisposable} goes away.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewTitleDescriptionComponentFactory {
    /**
     * Both editors, the title above the description, or a loading text while the template is loading.
     */
    @RequiredUIAccess
    Component createIn(Disposable parentDisposable, CodeReviewTitleDescriptionViewModel vm, Component titleEditor, Component descriptionEditor);

    @RequiredUIAccess
    Component createTitleEditorIn(Disposable parentDisposable,
                                  Project project,
                                  CodeReviewTitleDescriptionViewModel vm,
                                  LocalizeValue titlePlaceholder);

    @RequiredUIAccess
    Component createDescriptionEditorIn(Disposable parentDisposable,
                                        Project project,
                                        CodeReviewTitleDescriptionViewModel vm,
                                        LocalizeValue descriptionPlaceholder);

    /**
     * Shows the toolbar - the generation of the title and the description, for example - at the end of the description
     * editor.
     *
     * @return the editor panel
     */
    @RequiredUIAccess
    Component createGenerationToolbarOverlay(Component editorPanel, Component descriptionEditor, ActionToolbar toolbar);
}
