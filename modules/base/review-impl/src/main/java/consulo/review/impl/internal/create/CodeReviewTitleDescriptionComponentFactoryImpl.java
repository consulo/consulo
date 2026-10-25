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
package consulo.review.impl.internal.create;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.create.CodeReviewTitleDescriptionComponentFactory;
import consulo.review.create.CodeReviewTitleDescriptionViewModel;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.VerticalAlignment;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class CodeReviewTitleDescriptionComponentFactoryImpl implements CodeReviewTitleDescriptionComponentFactory {
    private static final Space EDITORS_GAP = Space.LARGE;
    private static final int TITLE_FONT_SIZE = 16;
    private static final int MAX_PENDING_ECHOES = 256;
    private static final String MARKDOWN_FILE_TYPE = "Markdown";

    private final EditorBoxBuilderFactory myEditorBoxBuilderFactory;
    private final FileTypeRegistry myFileTypeRegistry;

    @Inject
    public CodeReviewTitleDescriptionComponentFactoryImpl(EditorBoxBuilderFactory editorBoxBuilderFactory, FileTypeRegistry fileTypeRegistry) {
        myEditorBoxBuilderFactory = editorBoxBuilderFactory;
        myFileTypeRegistry = fileTypeRegistry;
    }

    @RequiredUIAccess
    @Override
    public Component createIn(Disposable parentDisposable,
                              CodeReviewTitleDescriptionViewModel vm,
                              Component titleEditor,
                              Component descriptionEditor) {
        DockLayout editors = DockLayout.create(EDITORS_GAP);
        editors.top(titleEditor);
        editors.center(descriptionEditor);

        VerticalLayout loading = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        loading.add(Label.create(ReviewLocalize.reviewPopupDataLoading()));

        DockLayout textPanel = DockLayout.create(Space.NONE);
        ReviewUIUtil.bind(parentDisposable, vm.isTemplateLoading(), templateLoading ->
            textPanel.center(templateLoading ? loading : editors));
        return textPanel;
    }

    @RequiredUIAccess
    @Override
    public Component createTitleEditorIn(Disposable parentDisposable,
                                         Project project,
                                         CodeReviewTitleDescriptionViewModel vm,
                                         LocalizeValue titlePlaceholder) {
        EditorBox editor = myEditorBoxBuilderFactory.create(project)
            .fileType(getMarkdownFileType())
            .placeholder(titlePlaceholder)
            .customize(editorEx -> editorEx.setFontSize(TITLE_FONT_SIZE))
            .build();
        bindText(parentDisposable, editor, vm.getTitleText(), vm::setTitle);
        return editor;
    }

    @RequiredUIAccess
    @Override
    public Component createDescriptionEditorIn(Disposable parentDisposable,
                                               Project project,
                                               CodeReviewTitleDescriptionViewModel vm,
                                               LocalizeValue descriptionPlaceholder) {
        EditorBox editor = myEditorBoxBuilderFactory.create(project)
            .fileType(getMarkdownFileType())
            .multiline()
            .placeholder(descriptionPlaceholder)
            .customize(editorEx -> editorEx.getSettings().setUseSoftWraps(true))
            .build();
        bindText(parentDisposable, editor, vm.getDescriptionText(), vm::setDescription);
        return editor;
    }

    @RequiredUIAccess
    @Override
    public Component createGenerationToolbarOverlay(Component editorPanel, Component descriptionEditor, ActionToolbar toolbar) {
        toolbar.setTargetUIComponent(editorPanel);
        if (descriptionEditor instanceof EditorBox editorBox) {
            editorBox.setSuffixComponent(toolbar.getUIComponent());
        }
        return editorPanel;
    }

    private FileType getMarkdownFileType() {
        FileType markdown = myFileTypeRegistry.findFileTypeByName(MARKDOWN_FILE_TYPE);
        return markdown != null ? markdown : PlainTextFileType.INSTANCE;
    }

    @RequiredUIAccess
    private static void bindText(Disposable parentDisposable, EditorBox editor, ObservableValue<String> text, Consumer<String> setter) {
        Deque<String> typed = new ArrayDeque<>();
        ReviewUIUtil.bind(parentDisposable, text, value -> {
            if (typed.contains(value)) {
                String echoed;
                do {
                    echoed = typed.pollFirst();
                }
                while (echoed != null && !echoed.equals(value));
                return;
            }
            if (!value.equals(StringUtil.notNullize(editor.getValue()))) {
                editor.setValue(value);
            }
        });
        editor.addValueListener(event -> {
            String value = StringUtil.notNullize(event.getValue());
            typed.addLast(value);
            if (typed.size() > MAX_PENDING_ECHOES) {
                typed.removeFirst();
            }
            setter.accept(value);
        });
    }
}
