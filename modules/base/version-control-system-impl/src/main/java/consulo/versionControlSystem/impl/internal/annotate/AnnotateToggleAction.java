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
package consulo.versionControlSystem.impl.internal.annotate;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.extension.ExtensionPoint;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.UpToDateLineNumberProvider;
import consulo.versionControlSystem.action.AnnotateToggleActionProvider;
import consulo.versionControlSystem.annotate.AnnotationGutterActionProvider;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.LineAnnotationAspect;
import consulo.versionControlSystem.change.VcsAnnotationLocalChangesListener;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.internal.UpToDateLineNumberProviderImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 * @author lesya
 */
@ActionImpl(id = "Annotate")
public class AnnotateToggleAction extends ToggleAction implements DumbAware {
    private static final Key<Runnable> ERROR_NOTIFICATION = Key.create("AnnotateToggleAction.EditorNotify");

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
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean selected) {
        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            Runnable runnable = ClientProperty.get(editor.getHeaderComponent(), ERROR_NOTIFICATION);
            if (runnable != null) {
                runnable.run();
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
                EditorNotificationBuilderFactory factory = project.getApplication().getInstance(EditorNotificationBuilderFactory.class);

                Runnable closeRunnable = () -> {
                    JComponent headerComponent = editor.getHeaderComponent();
                    if (headerComponent == null) {
                        return;
                    }

                    Runnable showAction = ClientProperty.get(headerComponent, ERROR_NOTIFICATION);
                    if (showAction != null) {
                        headerComponent.setVisible(false);
                        editor.setHeaderComponent(null);
                    }
                };

                @RequiredUIAccess Runnable showAnnotation = () -> {
                    closeRunnable.run();

                    doAnnotate(editor, project, currentFile, fileAnnotation, vcs, upToDateLineNumbers, false);
                };

                EditorNotificationBuilderEx builder = (EditorNotificationBuilderEx) factory.newBuilder();
                builder.withType(NotificationType.ERROR);
                builder.withText(VcsLocalize.annotationWrongLineNumberNotificationText(vcs.getDisplayName()));
                builder.withAction(LocalizeValue.localizeTODO("Display anyway"), event -> showAnnotation.run());

                builder.withAction(LocalizeValue.localizeTODO("Hide"), LocalizeValue.localizeTODO("Hide this notification"), event -> closeRunnable.run());

                JComponent component = builder.getComponent();
                ClientProperty.put(component, ERROR_NOTIFICATION, showAnnotation);
                editor.setHeaderComponent(component);
                return;
            }
        }

        Disposable disposable = fileAnnotation::dispose;

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

        EditorGutterComponentEx editorGutter = (EditorGutterComponentEx) editor.getGutter();
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
        ExtensionPoint<AnnotationGutterActionProvider> extensionPoint =
            Application.get().getExtensionPoint(AnnotationGutterActionProvider.class);
        if (extensionPoint.hasAnyExtensions()) {
            presentation.addAction(AnSeparator.create());
        }
        extensionPoint.forEach(provider -> presentation.addAction(provider.createAction(fileAnnotation)));
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
    private static AnnotateToggleActionProvider getProvider(@Nonnull AnActionEvent e) {
        for (AnnotateToggleActionProvider provider : Application.get().getExtensionList(AnnotateToggleActionProvider.class)) {
            if (provider.isEnabled(e)) {
                return provider;
            }
        }
        return null;
    }
}
