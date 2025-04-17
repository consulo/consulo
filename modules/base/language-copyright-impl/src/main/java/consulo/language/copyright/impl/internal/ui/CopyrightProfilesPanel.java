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

package consulo.language.copyright.impl.internal.ui;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.impl.internal.ExternalOptionHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.lang.function.Conditions;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopyrightProfilesPanel extends MasterDetailsComponent implements SearchableConfigurable, Configurable.NoScroll, Configurable.NoMargin {
    private final Project myProject;
    private final CopyrightManager myManager;
    private final AtomicBoolean myInitialized = new AtomicBoolean(false);

    private Runnable myUpdate;

    public CopyrightProfilesPanel(Project project, Provider<MasterDetailsStateService> masterDetailsStateService) {
        super(masterDetailsStateService);
        myProject = project;
        myManager = CopyrightManager.getInstance(project);
        initTree();
    }

    public void setUpdate(Runnable update) {
        myUpdate = update;
    }

    @Override
    protected String getComponentStateKey() {
        return "Copyright.UI";
    }

    @Override
    protected void processRemovedItems() {
        Map<String, CopyrightProfile> profiles = getAllProfiles();
        List<CopyrightProfile> deleted = new ArrayList<>();
        for (CopyrightProfile profile : myManager.getCopyrights()) {
            if (!profiles.containsValue(profile)) {
                deleted.add(profile);
            }
        }
        for (CopyrightProfile profile : deleted) {
            myManager.removeCopyright(profile);
        }
    }

    @Override
    protected boolean wasObjectStored(Object o) {
        return myManager.getCopyrights().contains(o);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Copyright Profiles";
    }

    protected void reloadAvailableProfiles() {
        if (myUpdate != null) {
            myUpdate.run();
        }
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        Set<String> profiles = new HashSet<>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode)myRoot.getChildAt(i);
            String profileName = ((CopyrightConfigurable)node.getConfigurable()).getEditableObject().getName();
            if (profiles.contains(profileName)) {
                selectNodeInTree(profileName);
                throw new ConfigurationException("Duplicate copyright profile name: \'" + profileName + "\'");
            }
            profiles.add(profileName);
        }
        super.apply();
    }

    public Map<String, CopyrightProfile> getAllProfiles() {
        Map<String, CopyrightProfile> profiles = new HashMap<>();
        if (!myInitialized.get()) {
            for (CopyrightProfile profile : myManager.getCopyrights()) {
                profiles.put(profile.getName(), profile);
            }
        }
        else {
            for (int i = 0; i < myRoot.getChildCount(); i++) {
                MyNode node = (MyNode)myRoot.getChildAt(i);
                CopyrightProfile copyrightProfile = ((CopyrightConfigurable)node.getConfigurable()).getEditableObject();
                profiles.put(copyrightProfile.getName(), copyrightProfile);
            }
        }
        return profiles;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        myInitialized.set(false);
    }

    @Nullable
    @Override
    protected ArrayList<AnAction> createActions(boolean fromPopup) {
        ArrayList<AnAction> result = new ArrayList<AnAction>();
        result.add(new AnAction("Add", "Add", PlatformIconGroup.generalAdd()) {
            {
                registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent event) {
                String name = askForProfileName("Create Copyright Profile", "");
                if (name == null) {
                    return;
                }
                CopyrightProfile copyrightProfile = new CopyrightProfile(name);
                addProfileNode(copyrightProfile);
            }
        });
        result.add(new MyDeleteAction(forAll(Conditions.alwaysTrue())));
        result.add(new AnAction("Copy", "Copy", PlatformIconGroup.actionsCopy()) {
            {
                registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)), myTree);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent event) {
                String profileName = askForProfileName("Copy Copyright Profile", "");
                if (profileName == null) {
                    return;
                }
                CopyrightProfile clone = new CopyrightProfile();
                clone.copyFrom((CopyrightProfile)getSelectedObject());
                clone.setName(profileName);
                addProfileNode(clone);
            }

            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent event) {
                super.update(event);
                event.getPresentation().setEnabled(getSelectedObject() != null);
            }
        });
        result.add(new AnAction("Import", "Import", PlatformIconGroup.actionsImport()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent event) {
                FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
                descriptor.withFileFilter(file -> "xml".equals(file.getExtension()));
                descriptor.setTitle("Choose file containing copyright notice");
                VirtualFile file = IdeaFileChooser.chooseFile(descriptor, myProject, null);
                if (file == null) {
                    return;
                }

                List<CopyrightProfile> copyrightProfiles = ExternalOptionHelper.loadOptions(VirtualFileUtil.virtualToIoFile(file));
                if (copyrightProfiles == null) {
                    return;
                }
                if (!copyrightProfiles.isEmpty()) {
                    if (copyrightProfiles.size() == 1) {
                        importProfile(copyrightProfiles.get(0));
                    }
                    else {
                        JBPopupFactory.getInstance()
                            .createListPopup(new BaseListPopupStep<CopyrightProfile>("Choose profile to import", copyrightProfiles) {
                                @Override
                                public PopupStep onChosen(CopyrightProfile selectedValue, boolean finalChoice) {
                                    return doFinalStep(() -> importProfile(selectedValue));
                                }

                                @Nonnull
                                @Override
                                public String getTextFor(CopyrightProfile value) {
                                    return value.getName();
                                }
                            }).showUnderneathOf(myNorthPanel);
                    }
                }
                else {
                    Messages.showWarningDialog(myProject, "The selected file does not contain any copyright settings.", "Import Failure");
                }
            }

            @RequiredUIAccess
            private void importProfile(CopyrightProfile copyrightProfile) {
                String profileName = askForProfileName("Import copyright profile", copyrightProfile.getName());
                if (profileName == null) {
                    return;
                }
                copyrightProfile.setName(profileName);
                addProfileNode(copyrightProfile);
                Messages.showInfoMessage(myProject, "The copyright settings have been successfully imported.", "Import Complete");
            }
        });
        return result;
    }


    @Nullable
    @RequiredUIAccess
    private String askForProfileName(String title, String initialName) {
        return Messages.showInputDialog(
            "New copyright profile name:",
            title,
            UIUtil.getQuestionIcon(),
            initialName,
            new InputValidator() {
                @Override
                @RequiredUIAccess
                public boolean checkInput(String s) {
                    return !getAllProfiles().containsKey(s) && s.length() > 0;
                }

                @Override
                @RequiredUIAccess
                public boolean canClose(String s) {
                    return checkInput(s);
                }
            }
        );
    }

    private void addProfileNode(CopyrightProfile copyrightProfile) {
        CopyrightConfigurable copyrightConfigurable = new CopyrightConfigurable(myProject, copyrightProfile, TREE_UPDATER);
        copyrightConfigurable.setModified(true);
        MyNode node = new MyNode(copyrightConfigurable);
        addNode(node, myRoot);
        selectNodeInTree(node);
        reloadAvailableProfiles();
    }

    @Override
    @RequiredUIAccess
    protected void removePaths(TreePath... paths) {
        super.removePaths(paths);
        reloadAvailableProfiles();
    }

    private void reloadTree() {
        myRoot.removeAllChildren();
        Collection<CopyrightProfile> collection = myManager.getCopyrights();
        for (CopyrightProfile profile : collection) {
            CopyrightProfile clone = new CopyrightProfile();
            clone.copyFrom(profile);
            addNode(new MyNode(new CopyrightConfigurable(myProject, clone, TREE_UPDATER)), myRoot);
        }
        myInitialized.set(true);
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        reloadTree();
        super.reset();
    }

    @Override
    protected String getEmptySelectionString() {
        return "Select a profile to view or edit its details here";
    }

    public void addItemsChangeListener(Runnable runnable) {
        addItemsChangeListener(new ItemsChangeListener() {
            @Override
            public void itemChanged(@Nullable Object deletedItem) {
                SwingUtilities.invokeLater(runnable);
            }

            @Override
            public void itemsExternallyChanged(UnnamedConfigurable configurable) {
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

    @Nonnull
    @Override
    public String getId() {
        return "copyright.profiles";
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }
}
