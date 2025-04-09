/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.localVcs.UpToDateLineNumberProvider;
import consulo.ide.impl.idea.openapi.vcs.annotate.AnnotationGutterActionProvider;
import consulo.ide.impl.idea.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.AnnotateToggleActionProvider;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.LineAnnotationAspect;
import consulo.versionControlSystem.change.VcsAnnotationLocalChangesListener;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 * @author lesya
 */
public class AnnotateToggleAction extends ToggleAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        AnnotateToggleActionProvider provider = getProvider(e);
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(provider != null && !provider.isSuspended(e));
        if (provider != null) {
            presentation.setTextValue(provider.getActionName(e));
        }
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        AnnotateToggleActionProvider provider = getProvider(e);
        return provider != null && provider.isAnnotated(e);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean selected) {
        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            MyEditorNotificationPanel notificationPanel = ObjectUtil.tryCast(editor.getHeaderComponent(), MyEditorNotificationPanel.class);
            if (notificationPanel != null) {
                notificationPanel.showAnnotations();
                return;
            }
        }

        AnnotateToggleActionProvider provider = getProvider(e);
        if (provider != null) {
            provider.perform(e, selected);
        }
    }

    @RequiredUIAccess
    public static void doAnnotate(
        @Nonnull Editor editor,
        @Nonnull Project project,
        @Nullable VirtualFile currentFile,
        @Nonnull FileAnnotation fileAnnotation,
        @Nonnull AbstractVcs vcs
    ) {
        UpToDateLineNumberProvider upToDateLineNumberProvider = new UpToDateLineNumberProviderImpl(editor.getDocument(), project);
        doAnnotate(editor, project, currentFile, fileAnnotation, vcs, upToDateLineNumberProvider);
    }

    @RequiredUIAccess
    public static void doAnnotate(
        @Nonnull Editor editor,
        @Nonnull Project project,
        @Nullable VirtualFile currentFile,
        @Nonnull FileAnnotation fileAnnotation,
        @Nonnull AbstractVcs vcs,
        @Nonnull UpToDateLineNumberProvider upToDateLineNumbers
    ) {
        doAnnotate(editor, project, currentFile, fileAnnotation, vcs, upToDateLineNumbers, true);
    }

    @RequiredUIAccess
    private static void doAnnotate(
        @Nonnull Editor editor,
        @Nonnull Project project,
        @Nullable VirtualFile currentFile,
        @Nonnull FileAnnotation fileAnnotation,
        @Nonnull AbstractVcs vcs,
        @Nonnull UpToDateLineNumberProvider upToDateLineNumbers,
        boolean warnAboutSuspiciousAnnotations
    ) {
        if (warnAboutSuspiciousAnnotations) {
            int expectedLines = Math.max(upToDateLineNumbers.getLineCount(), 1);
            int actualLines = Math.max(fileAnnotation.getLineCount(), 1);
            if (Math.abs(expectedLines - actualLines) > 1) { // 1 - for different conventions about files ending with line separator
                editor.setHeaderComponent(new MyEditorNotificationPanel(
                    editor,
                    vcs,
                    () -> doAnnotate(editor, project, currentFile, fileAnnotation, vcs, upToDateLineNumbers, false)
                ));
                return;
            }
        }

        Disposable disposable = () -> fileAnnotation.dispose();

        if (fileAnnotation.getFile() != null && fileAnnotation.getFile().isInLocalFileSystem()) {
            VcsAnnotationLocalChangesListener changesListener =
                ProjectLevelVcsManager.getInstance(project).getAnnotationLocalChangesListener();

            changesListener.registerAnnotation(fileAnnotation.getFile(), fileAnnotation);
            Disposer.register(disposable, () -> changesListener.unregisterAnnotation(fileAnnotation.getFile(), fileAnnotation));
        }

        editor.getGutter().closeAllAnnotations();

        fileAnnotation.setCloser(() -> UIUtil.invokeLaterIfNeeded(() -> {
            if (project.isDisposed()) {
                return;
            }
            editor.getGutter().closeAllAnnotations();
        }));

        fileAnnotation.setReloader(newFileAnnotation -> {
            if (editor.getGutter().isAnnotationsShown()) {
                assert Comparing.equal(fileAnnotation.getFile(), newFileAnnotation.getFile());
                doAnnotate(editor, project, currentFile, newFileAnnotation, vcs, upToDateLineNumbers, false);
            }
        });

        EditorGutterComponentEx editorGutter = (EditorGutterComponentEx)editor.getGutter();
        List<AnnotationFieldGutter> gutters = new ArrayList<>();
        AnnotationSourceSwitcher switcher = fileAnnotation.getAnnotationSourceSwitcher();

        AnnotationPresentation presentation = new AnnotationPresentation(fileAnnotation, upToDateLineNumbers, switcher, disposable);
        if (currentFile != null && vcs.getCommittedChangesProvider() != null) {
            presentation.addAction(new ShowDiffFromAnnotation(fileAnnotation, vcs, currentFile));
        }
        presentation.addAction(new CopyRevisionNumberFromAnnotateAction(fileAnnotation));
        presentation.addAction(AnSeparator.getInstance());

        Couple<Map<VcsRevisionNumber, ColorValue>> bgColorMap = computeBgColors(fileAnnotation, editor);
        Map<VcsRevisionNumber, Integer> historyIds = computeLineNumbers(fileAnnotation);

        if (switcher != null) {
            switcher.switchTo(switcher.getDefaultSource());
            LineAnnotationAspect revisionAspect = switcher.getRevisionAspect();
            CurrentRevisionAnnotationFieldGutter currentRevisionGutter =
                new CurrentRevisionAnnotationFieldGutter(fileAnnotation, revisionAspect, presentation, bgColorMap);
            MergeSourceAvailableMarkerGutter mergeSourceGutter =
                new MergeSourceAvailableMarkerGutter(fileAnnotation, presentation, bgColorMap);

            SwitchAnnotationSourceAction switchAction = new SwitchAnnotationSourceAction(switcher, editorGutter);
            presentation.addAction(switchAction);
            switchAction.addSourceSwitchListener(currentRevisionGutter);
            switchAction.addSourceSwitchListener(mergeSourceGutter);

            currentRevisionGutter.accept(switcher.getDefaultSource());
            mergeSourceGutter.accept(switcher.getDefaultSource());

            gutters.add(currentRevisionGutter);
            gutters.add(mergeSourceGutter);
        }

        LineAnnotationAspect[] aspects = fileAnnotation.getAspects();
        for (LineAnnotationAspect aspect : aspects) {
            gutters.add(new AspectAnnotationFieldGutter(fileAnnotation, aspect, presentation, bgColorMap));
        }


        if (historyIds != null) {
            gutters.add(new HistoryIdColumn(fileAnnotation, presentation, bgColorMap, historyIds));
        }
        gutters.add(new HighlightedAdditionalColumn(fileAnnotation, presentation, bgColorMap));
        AnnotateActionGroup actionGroup = new AnnotateActionGroup(gutters, editorGutter, bgColorMap);
        presentation.addAction(actionGroup, 1);
        gutters.add(new ExtraFieldGutter(fileAnnotation, presentation, bgColorMap, actionGroup));

        presentation.addAction(new AnnotateCurrentRevisionAction(fileAnnotation, vcs));
        presentation.addAction(new AnnotatePreviousRevisionAction(fileAnnotation, vcs));
        addActionsFromExtensions(presentation, fileAnnotation);

        for (AnnotationFieldGutter gutter : gutters) {
            AnnotationGutterLineConvertorProxy proxy = new AnnotationGutterLineConvertorProxy(upToDateLineNumbers, gutter);
            if (gutter.isGutterAction()) {
                editor.getGutter().registerTextAnnotation(proxy, proxy);
            }
            else {
                editor.getGutter().registerTextAnnotation(proxy);
            }
        }
    }

    private static void addActionsFromExtensions(@Nonnull AnnotationPresentation presentation, @Nonnull FileAnnotation fileAnnotation) {
        List<AnnotationGutterActionProvider> extensions = AnnotationGutterActionProvider.EP_NAME.getExtensionList();
        if (extensions.size() > 0) {
            presentation.addAction(new AnSeparator());
        }
        for (AnnotationGutterActionProvider provider : extensions) {
            presentation.addAction(provider.createAction(fileAnnotation));
        }
    }

    @Nullable
    private static Map<VcsRevisionNumber, Integer> computeLineNumbers(@Nonnull FileAnnotation fileAnnotation) {
        Map<VcsRevisionNumber, Integer> numbers = new HashMap<>();
        List<VcsFileRevision> fileRevisionList = fileAnnotation.getRevisions();
        if (fileRevisionList != null) {
            int size = fileRevisionList.size();
            for (int i = 0; i < size; i++) {
                VcsFileRevision revision = fileRevisionList.get(i);
                VcsRevisionNumber number = revision.getRevisionNumber();

                numbers.put(number, size - i);
            }
        }
        return numbers.size() < 2 ? null : numbers;
    }

    @Nullable
    private static Couple<Map<VcsRevisionNumber, ColorValue>> computeBgColors(
        @Nonnull FileAnnotation fileAnnotation,
        @Nonnull Editor editor
    ) {
        Map<VcsRevisionNumber, ColorValue> commitOrderColors = new HashMap<>();
        Map<VcsRevisionNumber, ColorValue> commitAuthorColors = new HashMap<>();

        EditorColorsScheme colorScheme = editor.getColorsScheme();
        AnnotationsSettings settings = AnnotationsSettings.getInstance();
        List<ColorValue> authorsColorPalette = settings.getAuthorsColors(colorScheme);
        List<ColorValue> orderedColorPalette = settings.getOrderedColors(colorScheme);

        FileAnnotation.AuthorsMappingProvider authorsMappingProvider = fileAnnotation.getAuthorsMappingProvider();
        if (authorsMappingProvider != null) {
            Map<VcsRevisionNumber, String> authorsMap = authorsMappingProvider.getAuthors();

            Map<String, ColorValue> authorColors = new HashMap<>();
            for (String author : ContainerUtil.sorted(authorsMap.values(), Comparing::compare)) {
                int index = authorColors.size();
                ColorValue color = authorsColorPalette.get(index % authorsColorPalette.size());
                authorColors.put(author, color);
            }

            for (Map.Entry<VcsRevisionNumber, String> entry : authorsMap.entrySet()) {
                VcsRevisionNumber revision = entry.getKey();
                String author = entry.getValue();
                ColorValue color = authorColors.get(author);
                commitAuthorColors.put(revision, color);
            }
        }

        FileAnnotation.RevisionsOrderProvider revisionsOrderProvider = fileAnnotation.getRevisionsOrderProvider();
        if (revisionsOrderProvider != null) {
            List<List<VcsRevisionNumber>> orderedRevisions = revisionsOrderProvider.getOrderedRevisions();

            int revisionsCount = orderedRevisions.size();
            for (int index = 0; index < revisionsCount; index++) {
                ColorValue color = orderedColorPalette.get(orderedColorPalette.size() * index / revisionsCount);

                for (VcsRevisionNumber number : orderedRevisions.get(index)) {
                    commitOrderColors.put(number, color);
                }
            }
        }

        return Couple.of(
            commitOrderColors.size() > 1 ? commitOrderColors : null,
            commitAuthorColors.size() > 1 ? commitAuthorColors : null
        );
    }

    @Nullable
    private static AnnotateToggleActionProvider getProvider(AnActionEvent e) {
        for (AnnotateToggleActionProvider provider : Application.get().getExtensionList(AnnotateToggleActionProvider.class)) {
            if (provider.isEnabled(e)) {
                return provider;
            }
        }
        return null;
    }

    private static class MyEditorNotificationPanel extends EditorNotificationPanel {
        private final Editor myEditor;
        private final Runnable myShowAnnotations;

        @RequiredUIAccess
        public MyEditorNotificationPanel(@Nonnull Editor editor, @Nonnull AbstractVcs vcs, @Nonnull Runnable doShowAnnotations) {
            super(LightColors.RED);
            myEditor = editor;
            myShowAnnotations = doShowAnnotations;

            setText(VcsLocalize.annotationWrongLineNumberNotificationText(vcs.getDisplayName()).get());

            createActionLabel("Display anyway", () -> showAnnotations());
            createActionLabel("Hide", () -> hideNotification())
                .setToolTipText("Hide this notification");
        }

        public void showAnnotations() {
            hideNotification();
            myShowAnnotations.run();
        }

        private void hideNotification() {
            setVisible(false);
            if (myEditor.getHeaderComponent() == this) {
                myEditor.setHeaderComponent(null);
            }
        }
    }
}
