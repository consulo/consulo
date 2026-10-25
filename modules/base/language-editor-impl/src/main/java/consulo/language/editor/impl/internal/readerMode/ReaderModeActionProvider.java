// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.markup.InspectionWidgetActionProvider;
import consulo.document.FileDocumentManager;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AsyncToggleAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.CustomUIComponentAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ReaderModeActionProvider implements InspectionWidgetActionProvider {
    @RequiredUIAccess
    @Override
    public @Nullable AnAction createAction(Editor editor) {
        Project project = editor.getProject();
        if (project == null || project.isDefault()) {
            return null;
        }
        return new ReaderModeActionGroup(editor);
    }

    private static class ReaderModeActionGroup extends DefaultActionGroup implements DumbAware, AnActionWithAsyncUpdate {
        ReaderModeActionGroup(Editor editor) {
            super(new ReaderModeAction(editor), AnSeparator.create());
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            return ActionSafeReadLock.run(e, presentation -> {
                presentation.setEnabledAndVisible(false);
                Project project = e.getData(Project.KEY);
                if (project == null || !project.isInitialized()) {
                    return;
                }
                Editor textEditor = e.getData(Editor.KEY);
                if (textEditor == null) {
                    return;
                }
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(textEditor.getDocument());
                VirtualFile file = psiFile == null ? null : psiFile.getVirtualFile();
                presentation.setEnabledAndVisible(file != null && ReaderModeSettings.matchMode(project, file, textEditor));
            }).toCoroutine();
        }
    }

    private static class ReaderModeAction extends AsyncToggleAction implements DumbAware, CustomUIComponentAction {
        private final Editor myEditor;

        ReaderModeAction(Editor editor) {
            super(LocalizeValue.localizeTODO("Reading mode"));
            myEditor = editor;
        }

        @RequiredUIAccess
        @Override
        public Component createCustomComponent(Presentation presentation, String place) {
            Button button = Button.create(presentation.getTextValue(), event -> {
                Project project = myEditor.getProject();
                if (project != null) {
                    toggle(project);
                }
            });
            button.addStyle(ButtonStyle.TOOLBAR);
            button.setFocusable(false);
            updateButton(button, presentation);
            return button;
        }

        @RequiredUIAccess
        private static void updateButton(Button button, Presentation presentation) {
            button.setText(presentation.getTextValue());
            button.setIcon(presentation.getIcon());
            button.setToolTipText(presentation.getDescription());
        }

        @Override
        public CoroutineStep<Object, Boolean> isSelectedAsync(AnActionEvent e) {
            return CodeExecution.apply(in -> true);
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }
            toggle(project);
        }

        @RequiredUIAccess
        private static void toggle(Project project) {
            ReaderModeSettings settings = ReaderModeSettings.getInstance(project);
            settings.setEnabled(!settings.isEnabled());

            applyToAllEditors(project);
        }

        @RequiredUIAccess
        private static void applyToAllEditors(Project project) {
            for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                if (editor.getProject() != project) {
                    continue;
                }
                VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
                ReaderModeSettings.applyReaderMode(project, editor, file, true, true);
            }
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            return Coroutine
                .first(CodeExecution.<Object, Object>apply(in -> {
                    Toggleable.setSelected(presentation, true);

                    Project project = e.getData(Project.KEY);
                    if (project == null) {
                        return null;
                    }

                    if (ReaderModeSettings.getInstance(project).isEnabled()) {
                        presentation.setText(LocalizeValue.localizeTODO("Reading mode"));
                        presentation.setDescription(LocalizeValue.localizeTODO("Exit reading mode"));
                        presentation.setIcon(null);
                    }
                    else {
                        presentation.setText(LocalizeValue.empty());
                        presentation.setDescription(LocalizeValue.localizeTODO("Enter reading mode"));
                        presentation.setIcon(PlatformIconGroup.generalReadermode());
                    }
                    return null;
                }))
                .then(UIAction.apply(in -> {
                    if (presentation.getClientProperty(COMPONENT_KEY) instanceof Button button) {
                        updateButton(button, presentation);
                    }
                    return null;
                }));
        }
    }
}
