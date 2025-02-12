/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CommonBundle;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.internal.FullContentConfigurable;
import consulo.content.bundle.*;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.SdkProjectStructureElement;
import consulo.ide.impl.idea.openapi.ui.NonEmptyInputValidator;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

@ExtensionImpl
public class SdkListConfigurable extends BaseStructureConfigurable implements ApplicationConfigurable, FullContentConfigurable {
    public static final String ID = "sdks.list";

    public static final Predicate<SdkTypeId> ADD_SDK_FILTER = sdkTypeId -> sdkTypeId instanceof SdkType && ((SdkType) sdkTypeId).supportsUserAdd();

    private static final UnknownSdkType ourUnknownSdkType = UnknownSdkType.getInstance("UNKNOWN_BUNDLE");
    private final SdkModel.Listener myListener = new SdkModel.Listener() {
        @RequiredUIAccess
        @Override
        public void sdkAdded(Sdk sdk) {
        }

        @RequiredUIAccess
        @Override
        public void sdkRemove(Sdk sdk) {
        }

        @RequiredUIAccess
        @Override
        public void sdkChanged(Sdk sdk, String previousName) {
            updateName();
        }

        @RequiredUIAccess
        @Override
        public void sdkHomeSelected(Sdk sdk, String newSdkHome) {
            updateName();
        }

        private void updateName() {
            final TreePath path = myTree.getSelectionPath();
            if (path != null) {
                final MasterDetailsConfigurable configurable = ((MyNode) path.getLastPathComponent()).getConfigurable();
                if (configurable != null && configurable instanceof SdkConfigurable) {
                    configurable.updateName();
                }
            }
        }
    };

    private class CopySdkAction extends AnAction {
        private CopySdkAction() {
            super(CommonBundle.message("button.copy"), CommonBundle.message("button.copy"), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(final AnActionEvent e) {
            SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

            final Object o = getSelectedObject();
            if (o instanceof SdkImpl) {
                final SdkImpl selected = (SdkImpl) o;
                String defaultNewName = SdkUtil.createUniqueSdkName(selected.getName(), sdksModel.getSdks());
                final String newName = Messages.showInputDialog("Enter bundle name:", "Copy Bundle", null, defaultNewName, new NonEmptyInputValidator() {
                    @Override
                    public boolean checkInput(String inputString) {
                        return super.checkInput(inputString) && sdksModel.findSdk(inputString) == null;
                    }
                });
                if (newName == null) {
                    return;
                }

                SdkImpl sdk = selected.clone();
                sdk.setName(newName);
                sdk.setPredefined(false);

                sdksModel.doAdd(sdk, sdk1 -> addSdkNode(sdk1));
            }
        }

        @RequiredUIAccess
        @Override
        public void update(final AnActionEvent e) {
            if (myTree.getSelectionPaths() == null || myTree.getSelectionPaths().length != 1) {
                e.getPresentation().setEnabled(false);
            }
            else {
                Object selectedObject = getSelectedObject();
                e.getPresentation().setEnabled(selectedObject instanceof SdkImpl && !(((SdkImpl) selectedObject).getSdkType() instanceof UnknownSdkType));
            }
        }
    }

    private final ShowSettingsUtil myShowSettingsUtil;

    private Disposable myListenerDisposable;

    @Inject
    public SdkListConfigurable(ShowSettingsUtil showSettingsUtil) {
        super(() -> null);
        myShowSettingsUtil = showSettingsUtil;
    }

    @Override
    protected String getComponentStateKey() {
        return "JdkListConfigurable.UI";
    }

    @Override
    public void setBannerComponent(JComponent bannerComponent) {
        myNorthPanel.add(bannerComponent, BorderLayout.NORTH);
    }

    @Override
    protected void processRemovedItems() {
    }

    @Override
    protected boolean wasObjectStored(final Object editableObject) {
        return false;
    }

    @Nonnull
    @Override
    @Nls
    public String getDisplayName() {
        return ProjectBundle.message("global.bundles.display.name");
    }

    @Override
    @Nullable
    public String getHelpTopic() {
        return myCurrentConfigurable != null ? myCurrentConfigurable.getHelpTopic() : null;
    }

    @Override
    @Nonnull
    public String getId() {
        return ID;
    }

    @Override
    protected void loadTree() {
        if (myListenerDisposable != null) {
            myListenerDisposable.dispose();
            myListenerDisposable = null;
        }

        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
        myListenerDisposable = Disposable.newDisposable();
        sdksModel.addListener(myListener, myListenerDisposable);

        final Map<SdkType, List<Sdk>> map = new HashMap<>();

        for (Sdk sdk : sdksModel.getModifiedSdksMap().values()) {
            SdkType sdkType = (SdkType) sdk.getSdkType();
            if (sdkType instanceof UnknownSdkType) {
                sdkType = ourUnknownSdkType;
            }

            List<Sdk> list = map.get(sdkType);
            if (list == null) {
                map.put(sdkType, list = new ArrayList<>());
            }

            list.add(sdk);
        }

        for (Map.Entry<SdkType, List<Sdk>> entry : map.entrySet()) {
            final SdkType key = entry.getKey();
            final List<Sdk> value = entry.getValue();

            final MyNode groupNode = createSdkGroupNode(key);
            groupNode.setAllowsChildren(true);
            addNode(groupNode, myRoot);

            for (Sdk sdk : value) {
                final SdkConfigurable configurable = new SdkConfigurable((SdkImpl) sdk, sdksModel, TREE_UPDATER);

                addNode(new MyNode(configurable), groupNode);
            }
        }
    }

    @Override
    protected void initSelection() {
        super.initSelection();
        TreeUtil.expandAll(getTree());
    }

    @Nonnull
    private static MyNode createSdkGroupNode(SdkType key) {
        return new MyNode(new TextConfigurable<>(key, key.getPresentableName(), "", "", key.getGroupIcon()), true);
    }

    @Nonnull
    @Override
    protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
        final List<ProjectStructureElement> result = new ArrayList<>();
        for (Sdk sdk : sdksModel.getModifiedSdksMap().values()) {
            result.add(new SdkProjectStructureElement(sdk));
        }
        return result;
    }

    public boolean addSdkNode(final Sdk sdk) {
        if (myUiDisposed) {
            return false;
        }

        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        // todo myContext.getDaemonAnalyzer().queueUpdate(new SdkProjectStructureElement(sdk));

        MyNode newSdkNode = new MyNode(new SdkConfigurable((SdkImpl) sdk, sdksModel, TREE_UPDATER));

        final MyNode groupNode = MasterDetailsComponent.findNodeByObject(myRoot, sdk.getSdkType());
        if (groupNode != null) {
            addNode(newSdkNode, groupNode);
        }
        else {
            final MyNode sdkGroupNode = createSdkGroupNode((SdkType) sdk.getSdkType());

            sdkGroupNode.add(newSdkNode);

            addNode(sdkGroupNode, myRoot);

            TreeUtil.expandAll(myTree);
        }

        selectNodeInTree(newSdkNode);
        return true;
    }

    @Override
    protected boolean canBeRemoved(Object[] editableObjects) {
        for (Object editableObject : editableObjects) {
            if (editableObject instanceof Sdk && ((Sdk) editableObject).isPredefined()) {
                return false;
            }
        }
        return super.canBeRemoved(editableObjects);
    }

    @Override
    protected void onItemDeleted(Object item) {
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            final TreeNode childAt = myRoot.getChildAt(i);
            if (childAt instanceof MyNode) {
                if (childAt.getChildCount() == 0) {
                    ((DefaultTreeModel) myTree.getModel()).removeNodeFromParent((MutableTreeNode) childAt);
                }
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();

        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        if (myListenerDisposable != null) {
            myListenerDisposable.disposeWithTree();
        }

        sdksModel.dispose();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        sdksModel.reset();

        super.reset();

        myTree.setRootVisible(false);
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        boolean modifiedSdks = false;
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            final TreeNode groupNode = myRoot.getChildAt(i);

            for (int k = 0; k < groupNode.getChildCount(); k++) {
                final MyNode sdkNode = (MyNode) groupNode.getChildAt(k);
                final MasterDetailsConfigurable configurable = sdkNode.getConfigurable();
                if (configurable.isModified()) {
                    configurable.apply();
                    modifiedSdks = true;
                }
            }
        }

        if (sdksModel.isModified() || modifiedSdks) {
            sdksModel.apply(this);
        }
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        return super.isModified() || sdksModel.isModified();
    }

    @Nonnull
    @Override
    protected List<? extends AnAction> createCopyActions(boolean fromPopup) {
        return Collections.singletonList(new CopySdkAction());
    }

    @Override
    public AbstractAddGroup createAddAction() {
        return new AbstractAddGroup(ProjectBundle.message("add.action.name")) {
            @Nonnull
            @Override
            public AnAction[] getChildren(@Nullable final AnActionEvent e) {
                SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

                DefaultActionGroup group = new DefaultActionGroup(ProjectBundle.message("add.action.name"), true);
                sdksModel.createAddActions(group, myTree, sdk -> addSdkNode(sdk), ADD_SDK_FILTER);
                return group.getChildren(null);
            }
        };
    }

    @Override
    protected void removeSdk(final Sdk jdk) {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
        sdksModel.removeSdk(jdk);
        // todo myContext.getDaemonAnalyzer().removeElement(new SdkProjectStructureElement(jdk));
    }

    @Override
    @Nullable
    protected String getEmptySelectionString() {
        return ProjectBundle.message("global.bundles.empty.text");
    }
}
