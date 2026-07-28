// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.markup.InspectionWidgetActionProvider;
import consulo.colorScheme.EditorColorKey;
import consulo.dataContext.DataManager;
import consulo.document.FileDocumentManager;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    private static class ReaderModeAction extends ToggleAction implements DumbAware, CustomComponentAction {
        private static final EditorColorKey FOREGROUND =
            EditorColorKey.createColorKey("ActionButtonImpl.iconTextForeground", TargetAWT.from(UIUtil.getContextHelpForeground()));

        private final Editor myEditor;

        ReaderModeAction(Editor editor) {
            super(LocalizeValue.localizeTODO("Reading mode"));
            myEditor = editor;
        }

        @RequiredUIAccess
        @Override
        public JComponent createCustomComponent(Presentation presentation, String place) {
            return new ReaderModeButton(presentation, place);
        }

        private final class ReaderModeButton extends JLabel {
            private final Presentation myPresentation;
            private final String myPlace;
            private final PropertyChangeListener myPresentationListener;

            private boolean myHovered;

            ReaderModeButton(Presentation presentation, String place) {
                myPresentation = presentation;
                myPlace = place;

                setIconTextGap(JBUIScale.scale(2));
                setForeground(new JBColor(() -> ObjectUtil.notNull(
                    TargetAWT.to(myEditor.getColorsScheme().getColor(FOREGROUND)),
                    ObjectUtil.notNull(TargetAWT.to(FOREGROUND.getDefaultColorValue()), UIUtil.getInactiveTextColor())
                )));

                myPresentationListener = event -> updateFromPresentation();

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        myHovered = true;
                        updateFromPresentation();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        myHovered = false;
                        updateFromPresentation();
                    }

                    @Override
                    @RequiredUIAccess
                    public void mouseClicked(MouseEvent e) {
                        AnActionEvent actionEvent = AnActionEvent.createFromDataContext(
                            myPlace,
                            myPresentation,
                            DataManager.getInstance().getDataContext(ReaderModeButton.this)
                        );
                        actionPerformed(actionEvent);
                    }
                });

                updateFromPresentation();
            }

            @Override
            public void addNotify() {
                super.addNotify();
                myPresentation.addPropertyChangeListener(myPresentationListener);
                updateFromPresentation();
            }

            @Override
            public void removeNotify() {
                myPresentation.removePropertyChangeListener(myPresentationListener);
                super.removeNotify();
            }

            @Override
            public void updateUI() {
                super.updateUI();
                if (!Platform.current().os().isWindows()) {
                    Font font = getFont();
                    if (font != null) {
                        setFont(new FontUIResource(font.deriveFont(font.getStyle(), font.getSize() - JBUIScale.scale(2f))));
                    }
                }
            }

            private void updateFromPresentation() {
                setText(myPresentation.getText());
                Image icon = myPresentation.getIcon();
                Image hoveredIcon = myPresentation.getHoveredIcon();
                setIcon(TargetAWT.to(myHovered && hoveredIcon != null ? hoveredIcon : icon));
                setToolTipText(myPresentation.getDescription().get());
                setBorder(JBUI.Borders.empty(2, 2, 2, icon == PlatformIconGroup.generalReadermode() ? 2 : 7));
                revalidate();
                repaint();
            }
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return true;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }

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
        public void update(AnActionEvent e) {
            super.update(e);

            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }

            Presentation presentation = e.getPresentation();
            if (ReaderModeSettings.getInstance(project).isEnabled()) {
                presentation.setText(LocalizeValue.localizeTODO("Reading mode"));
                presentation.setDescription(LocalizeValue.localizeTODO("Exit reading mode"));
                presentation.setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                presentation.setHoveredIcon(PlatformIconGroup.actionsClosedarkgrey());
            }
            else {
                presentation.setText(LocalizeValue.empty());
                presentation.setDescription(LocalizeValue.localizeTODO("Enter reading mode"));
                presentation.setIcon(PlatformIconGroup.generalReadermode());
                presentation.setHoveredIcon(null);
            }
        }
    }
}
