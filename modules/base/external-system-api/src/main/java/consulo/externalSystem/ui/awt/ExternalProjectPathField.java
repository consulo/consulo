/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.ui.awt;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModel;
import consulo.component.PropertiesComponent;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;
import consulo.externalSystem.internal.ui.ExternalSystemTasksTree;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.ui.ExternalProjectPathLookupElement;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldCompletionProviderDumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Denis Zhdanov
 * @since 24.05.13 19:13
 */
public class ExternalProjectPathField extends Wrapper implements TextAccessor {

    @Nonnull
    private static final String PROJECT_FILE_TO_START_WITH_KEY = "external.system.task.project.file.to.start";

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ProjectSystemId myExternalSystemId;

    private final EditorTextField myEditorTextField;

    public ExternalProjectPathField(@Nonnull Project project,
                                    @Nonnull ProjectSystemId externalSystemId,
                                    @Nonnull FileChooserDescriptor descriptor,
                                    @Nonnull String fileChooserTitle) {
        myExternalSystemId = externalSystemId;
        myProject = project;
        myEditorTextField = createTextField(project, externalSystemId);
        setContent(myEditorTextField);

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        builder.add(DumbAwareAction.create(ExternalSystemBundle.message("run.configuration.tooltip.choose.registered.project",
            externalSystemId.getDisplayName()), externalSystemId.getIcon(), e -> {
            final Ref<JBPopup> popupRef = new Ref<JBPopup>();
            final Tree tree = buildRegisteredProjectsTree(project, externalSystemId);
            tree.setBorder(IdeBorderFactory.createEmptyBorder(8));
            Runnable treeSelectionCallback = new Runnable() {
                @Override
                public void run() {
                    TreePath path = tree.getSelectionPath();
                    if (path != null) {
                        Object lastPathComponent = path.getLastPathComponent();
                        if (lastPathComponent instanceof ExternalSystemNode) {
                            Object e = ((ExternalSystemNode) lastPathComponent).getDescriptor().getElement();
                            if (e instanceof ExternalProjectPojo) {
                                ExternalProjectPojo pojo = (ExternalProjectPojo) e;
                                myEditorTextField.setText(pojo.getPath());
                                Editor editor = myEditorTextField.getEditor();
                                if (editor != null) {
                                    collapseIfPossible(editor, externalSystemId, project);
                                }
                            }
                        }
                    }
                    popupRef.get().closeOk(null);
                }
            };

            ExternalSystemInternalHelper helper = Application.get().getInstance(ExternalSystemInternalHelper.class);
            JBPopup popup = helper.createPopupBuilder(tree)
                .setItemChoosenCallback(treeSelectionCallback)
                .setTitle(ExternalSystemBundle.message("run.configuration.title.choose.registered.project", externalSystemId.getDisplayName()))
                .setResizable(true)
                .setAutoselectOnMouseMove(true)
                .setCloseOnEnter(false)
                .createPopup();
            popupRef.set(popup);
            popup.showUnderneathOf(e.getInputEvent().getComponent());
        }));

        builder.add(new MyBrowseListener(descriptor, fileChooserTitle, project, myEditorTextField));

        ActionToolbar toolbar =
            ActionToolbarFactory.getInstance().createActionToolbar("ExternalProjectPath", builder.build(), ActionToolbar.Style.INPLACE);
        toolbar.setTargetComponent(this);
        toolbar.updateActionsImmediately();

        myEditorTextField.setSuffixComponent(toolbar.getComponent());
    }

    @Nonnull
    private static Tree buildRegisteredProjectsTree(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
        ExternalSystemTasksTreeModel model = new ExternalSystemTasksTreeModel(externalSystemId);
        ExternalSystemTasksTree result = new ExternalSystemTasksTree(model, new HashMap<>(), project, externalSystemId);

        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;
        AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
        Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = settings.getAvailableProjects();
        List<ExternalProjectPojo> rootProjects = new ArrayList<>(projects.keySet());
        ContainerUtil.sort(rootProjects);
        for (ExternalProjectPojo rootProject : rootProjects) {
            model.ensureSubProjectsStructure(rootProject, projects.get(rootProject));
        }
        return result;
    }

    @Nonnull
    private static EditorTextField createTextField(@Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;
        final AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
        final ExternalSystemUiAware uiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);
        TextFieldCompletionProvider provider = new TextFieldCompletionProviderDumbAware() {
            @Override
            public void addCompletionVariants(@Nonnull String text, int offset, @Nonnull String prefix, @Nonnull CompletionResultSet result) {
                for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : settings.getAvailableProjects().entrySet()) {
                    String rootProjectPath = entry.getKey().getPath();
                    String rootProjectName = uiAware.getProjectRepresentationName(rootProjectPath, null);
                    ExternalProjectPathLookupElement rootProjectElement = new ExternalProjectPathLookupElement(rootProjectName, rootProjectPath);
                    result.addElement(rootProjectElement);
                    for (ExternalProjectPojo subProject : entry.getValue()) {
                        String p = subProject.getPath();
                        if (rootProjectPath.equals(p)) {
                            continue;
                        }
                        String subProjectName = uiAware.getProjectRepresentationName(p, rootProjectPath);
                        ExternalProjectPathLookupElement subProjectElement = new ExternalProjectPathLookupElement(subProjectName, p);
                        result.addElement(subProjectElement);
                    }
                }
                result.stopHere();
            }
        };
        EditorTextField result = provider.createEditor(project, false, editor -> {
            collapseIfPossible(editor, externalSystemId, project);
            editor.getSettings().setShowIntentionBulb(false);
        });
        result.setBorder(UIUtil.getTextFieldBorder());
        result.setOneLineMode(true);
        result.setOpaque(true);
        result.setBackground(UIUtil.getTextFieldBackground());
        return result;
    }

    @Override
    @RequiredUIAccess
    public void setText(final String text) {
        myEditorTextField.setText(text);

        Editor editor = myEditorTextField.getEditor();
        if (editor != null) {
            collapseIfPossible(editor, myExternalSystemId, myProject);
        }
    }

    private static void collapseIfPossible(@Nonnull final Editor editor,
                                           @Nonnull ProjectSystemId externalSystemId,
                                           @Nonnull Project project) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;
        final AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);
        final ExternalSystemUiAware uiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);

        String rawText = editor.getDocument().getText();
        for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : settings.getAvailableProjects().entrySet()) {
            if (entry.getKey().getPath().equals(rawText)) {
                collapse(editor, uiAware.getProjectRepresentationName(entry.getKey().getPath(), null));
                return;
            }
            for (ExternalProjectPojo pojo : entry.getValue()) {
                if (pojo.getPath().equals(rawText)) {
                    collapse(editor, uiAware.getProjectRepresentationName(pojo.getPath(), entry.getKey().getPath()));
                    return;
                }
            }
        }
    }

    private static void collapse(@Nonnull final Editor editor, @Nonnull final String placeholder) {
        final FoldingModel foldingModel = editor.getFoldingModel();
        foldingModel.runBatchFoldingOperation(new Runnable() {
            @Override
            public void run() {
                for (FoldRegion region : foldingModel.getAllFoldRegions()) {
                    foldingModel.removeFoldRegion(region);
                }
                FoldRegion region = foldingModel.addFoldRegion(0, editor.getDocument().getTextLength(), placeholder);
                if (region != null) {
                    region.setExpanded(false);
                }
            }
        });
    }

    @Override
    public String getText() {
        return myEditorTextField.getText();
    }

    private static class MyBrowseListener extends DumbAwareAction {

        @Nonnull
        private final FileChooserDescriptor myDescriptor;
        @Nonnull
        private final Project myProject;
        private final EditorTextField myPathField;

        MyBrowseListener(@Nonnull final FileChooserDescriptor descriptor,
                         @Nonnull final String fileChooserTitle,
                         @Nonnull final Project project,
                         @Nonnull EditorTextField pathField) {
            super(fileChooserTitle, null, PlatformIconGroup.nodesFolder());
            descriptor.setTitle(fileChooserTitle);
            myDescriptor = descriptor;
            myProject = project;
            myPathField = pathField;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            PropertiesComponent component = ProjectPropertiesComponent.getInstance(myProject);
            String pathToStart = myPathField.getText();
            if (StringUtil.isEmpty(pathToStart)) {
                pathToStart = component.getValue(PROJECT_FILE_TO_START_WITH_KEY);
            }
            VirtualFile fileToStart = null;
            if (!StringUtil.isEmpty(pathToStart)) {
                fileToStart = LocalFileSystem.getInstance().findFileByPath(pathToStart);
            }

            FileChooser.chooseFile(myDescriptor, myProject, fileToStart).doWhenDone(file -> {
                String path = ExternalSystemApiUtil.getLocalFileSystemPath(file);
                myPathField.setText(path);
                component.setValue(PROJECT_FILE_TO_START_WITH_KEY, path);
            });
        }
    }

    public static class MyPathAndProjectButtonPanel extends JPanel {

        @Nonnull
        private final EditorTextField myTextField;
        @Nonnull
        private final FixedSizeButton myRegisteredProjectsButton;

        public MyPathAndProjectButtonPanel(@Nonnull EditorTextField textField,
                                           @Nonnull FixedSizeButton registeredProjectsButton) {
            super(new GridBagLayout());
            myTextField = textField;
            myRegisteredProjectsButton = registeredProjectsButton;
            add(myTextField, new GridBag().weightx(1).fillCellHorizontally());
            add(myRegisteredProjectsButton, new GridBag().insets(0, 3, 0, 0));
        }

        @Nonnull
        public EditorTextField getTextField() {
            return myTextField;
        }
    }
}
