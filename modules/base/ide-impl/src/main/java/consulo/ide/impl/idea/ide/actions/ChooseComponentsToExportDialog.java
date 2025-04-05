/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.container.boot.ContainerPathManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ElementsChooser;
import consulo.ui.ex.awt.FieldPanel;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.image.Image;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.*;

public class ChooseComponentsToExportDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(ChooseComponentsToExportDialog.class);

    private final ElementsChooser<ComponentElementProperties> myChooser;
    private final FieldPanel myPathPanel;
    public static final String DEFAULT_PATH =
        FileUtil.toSystemDependentName(ContainerPathManager.get().getConfigPath() + "/" + "settings.zip");
    private final boolean myShowFilePath;
    @Nonnull
    private final LocalizeValue myDescription;

    public ChooseComponentsToExportDialog(
        MultiMap<File, ExportSettingsAction.ExportableItem> fileToComponents,
        boolean showFilePath,
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue description
    ) {
        super(false);
        myDescription = description;
        myShowFilePath = showFilePath;
        Map<ExportSettingsAction.ExportableItem, ComponentElementProperties> componentToContainingListElement = new LinkedHashMap<>();

        for (ExportSettingsAction.ExportableItem component : fileToComponents.values()) {
            if (!addToExistingListElement(component, componentToContainingListElement, fileToComponents)) {
                ComponentElementProperties componentElementProperties = new ComponentElementProperties();
                componentElementProperties.addComponent(component);

                componentToContainingListElement.put(component, componentElementProperties);
            }
        }
        Set<ComponentElementProperties> componentElementProperties = new LinkedHashSet<>(componentToContainingListElement.values());
        myChooser = new ElementsChooser<>(true);
        myChooser.setColorUnmarkedElements(false);
        for (ComponentElementProperties componentElementProperty : componentElementProperties) {
            myChooser.addElement(componentElementProperty, true, componentElementProperty);
        }
        myChooser.sort((o1, o2) -> o1.toString().compareTo(o2.toString()));

        ActionListener browseAction = new ActionListener() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                chooseSettingsFile(
                    myPathPanel.getText(),
                    getWindow(),
                    IdeLocalize.titleExportFileLocation(),
                    IdeLocalize.promptChooseExportSettingsFilePath()
                ).doWhenDone(path -> myPathPanel.setText(FileUtil.toSystemDependentName(path)));
            }
        };

        myPathPanel = new FieldPanel(
            IdeLocalize.editboxExportSettingsTo().get(),
            null,
            browseAction,
            null
        );

        String exportPath = PropertiesComponent.getInstance().getValue("export.settings.path", DEFAULT_PATH);
        myPathPanel.setText(exportPath);
        myPathPanel.setChangeListener(this::updateControls);
        updateControls();

        setTitle(title);
        init();
    }

    private void updateControls() {
        setOKActionEnabled(!StringUtil.isEmptyOrSpaces(myPathPanel.getText()));
    }

    @Nonnull
    @Override
    protected Action[] createLeftSideActions() {
        AbstractAction selectAll = new AbstractAction("Select &All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.setAllElementsMarked(true);
            }
        };
        AbstractAction selectNone = new AbstractAction("Select &None") {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.setAllElementsMarked(false);
            }
        };
        AbstractAction invert = new AbstractAction("&Invert") {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.invertSelection();
            }
        };
        return new Action[]{selectAll, selectNone, invert};
    }

    @Override
    protected void doOKAction() {
        PropertiesComponent.getInstance().setValue("export.settings.path", myPathPanel.getText());
        super.doOKAction();
    }

    private static boolean addToExistingListElement(
        ExportSettingsAction.ExportableItem component,
        Map<ExportSettingsAction.ExportableItem, ComponentElementProperties> componentToContainingListElement,
        MultiMap<File, ExportSettingsAction.ExportableItem> fileToComponents
    ) {
        File[] exportFiles = component.getExportFiles();
        File file = null;
        for (File exportFile : exportFiles) {
            Collection<ExportSettingsAction.ExportableItem> tiedComponents = fileToComponents.get(exportFile);

            for (ExportSettingsAction.ExportableItem tiedComponent : tiedComponents) {
                if (tiedComponent == component) {
                    continue;
                }
                ComponentElementProperties elementProperties = componentToContainingListElement.get(tiedComponent);
                if (elementProperties != null && !FileUtil.filesEqual(exportFile, file)) {
                    LOG.assertTrue(file == null, "Component " + component + " serialize itself into " + file + " and " + exportFile);
                    // found
                    elementProperties.addComponent(component);
                    componentToContainingListElement.put(component, elementProperties);
                    file = exportFile;
                }
            }
        }
        return file != null;
    }

    @Nonnull
    @RequiredUIAccess
    public static AsyncResult<String> chooseSettingsFile(
        String oldPath,
        Component parent,
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue description
    ) {
        FileChooserDescriptor chooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        chooserDescriptor.withDescriptionValue(description);
        chooserDescriptor.setHideIgnored(false);
        chooserDescriptor.withTitleValue(title);

        VirtualFile initialDir;
        if (oldPath != null) {
            File oldFile = new File(oldPath);
            initialDir = LocalFileSystem.getInstance().findFileByIoFile(oldFile);
            if (initialDir == null && oldFile.getParentFile() != null) {
                initialDir = LocalFileSystem.getInstance().findFileByIoFile(oldFile.getParentFile());
            }
        }
        else {
            initialDir = null;
        }
        AsyncResult<String> result = AsyncResult.undefined();
        AsyncResult<VirtualFile[]> fileAsyncResult = FileChooser.chooseFiles(chooserDescriptor, null, parent, initialDir);
        fileAsyncResult.doWhenDone(files -> {
            VirtualFile file = files[0];
            if (file.isDirectory()) {
                result.setDone(file.getPath() + '/' + new File(DEFAULT_PATH).getName());
            }
            else {
                result.setDone(file.getPath());
            }
        });
        fileAsyncResult.doWhenRejected((Runnable)result::setRejected);
        return result;
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPathPanel.getTextField();
    }

    @Override
    protected JComponent createNorthPanel() {
        return new JLabel(myDescription.get());
    }

    @Override
    protected JComponent createCenterPanel() {
        return myChooser;
    }

    @Override
    @RequiredUIAccess
    protected JComponent createSouthPanel() {
        JComponent buttons = super.createSouthPanel();
        if (!myShowFilePath) {
            return buttons;
        }
        JPanel panel = new JPanel(new VerticalFlowLayout());
        panel.add(myPathPanel);
        panel.add(buttons);
        return panel;
    }

    Set<ExportSettingsAction.ExportableItem> getExportableComponents() {
        List<ComponentElementProperties> markedElements = myChooser.getMarkedElements();
        Set<ExportSettingsAction.ExportableItem> components = new HashSet<>();
        for (ComponentElementProperties elementProperties : markedElements) {
            components.addAll(elementProperties.myComponents);
        }
        return components;
    }

    private static class ComponentElementProperties implements ElementsChooser.ElementProperties {
        private final Set<ExportSettingsAction.ExportableItem> myComponents = new HashSet<>();

        private boolean addComponent(ExportSettingsAction.ExportableItem component) {
            return myComponents.add(component);
        }

        @Override
        @Nullable
        public Image getIcon() {
            return null;
        }

        @Override
        @Nullable
        public Color getColor() {
            return null;
        }

        @Override
        public String toString() {
            Set<String> names = new LinkedHashSet<>();

            for (ExportSettingsAction.ExportableItem component : myComponents) {
                names.add(component.getPresentableName());
            }

            return StringUtil.join(names.toArray(new String[names.size()]), ", ");
        }
    }

    File getExportFile() {
        return new File(myPathPanel.getText());
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.ide.actions.ChooseComponentsToExportDialog";
    }
}
