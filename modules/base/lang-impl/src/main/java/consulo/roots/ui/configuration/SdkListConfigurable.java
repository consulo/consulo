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
package consulo.roots.ui.configuration;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.projectRoots.impl.UnknownSdkType;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.TextConfigurable;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Condition;
import com.intellij.util.Consumer;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.ide.settings.impl.SettingsSdksModel;
import consulo.ide.settings.impl.ShowSdksSettingsUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * Version {@link com.intellij.openapi.roots.ui.configuration.projectRoot.SdkListConfigurable} without {@link com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext}
 */
@Singleton
public class SdkListConfigurable extends BaseStructureConfigurableNoDaemon implements Configurable.NoScroll, Configurable.NoMargin {
  public static final Condition<SdkTypeId> ADD_SDK_FILTER = sdkTypeId -> sdkTypeId instanceof SdkType && ((SdkType)sdkTypeId).supportsUserAdd();

  private static final UnknownSdkType ourUnknownSdkType = UnknownSdkType.getInstance("UNKNOWN_BUNDLE");
  private final SettingsSdksModel mySdksModel;
  private final SdkModel.Listener myListener = new SdkModel.Listener() {
    @Override
    public void sdkChanged(Sdk sdk, String previousName) {
      updateName();
    }

    @Override
    public void sdkHomeSelected(Sdk sdk, String newSdkHome) {
      updateName();
    }

    private void updateName() {
      final TreePath path = myTree.getSelectionPath();
      if (path != null) {
        final NamedConfigurable configurable = ((MyNode)path.getLastPathComponent()).getConfigurable();
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
      final Object o = getSelectedObject();
      if (o instanceof SdkImpl) {
        final SdkImpl selected = (SdkImpl)o;
        String defaultNewName = SdkConfigurationUtil.createUniqueSdkName(selected.getName(), mySdksModel.getSdks());
        final String newName = Messages.showInputDialog("Enter bundle name:", "Copy Bundle", null, defaultNewName, new NonEmptyInputValidator() {
          @Override
          public boolean checkInput(String inputString) {
            return super.checkInput(inputString) && mySdksModel.findSdk(inputString) == null;
          }
        });
        if (newName == null) return;

        SdkImpl sdk = selected.clone();
        sdk.setName(newName);
        sdk.setPredefined(false);

        mySdksModel.doAdd(sdk, sdk1 -> addSdkNode(sdk1, true));
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
        e.getPresentation().setEnabled(selectedObject instanceof SdkImpl && !(((SdkImpl)selectedObject).getSdkType() instanceof UnknownSdkType));
      }
    }
  }

  @Inject
  public SdkListConfigurable(ShowSettingsUtil showSettingsUtil) {
    super(DefaultProjectFactory.getInstance().getDefaultProject());

    ShowSdksSettingsUtil sdksSettingsUtil = (ShowSdksSettingsUtil)showSettingsUtil;

    mySdksModel = sdksSettingsUtil.getSdksModel();
    mySdksModel.addListener(myListener);
  }

  @Override
  protected String getComponentStateKey() {
    return "JdkListConfigurable.UI";
  }

  @Override
  protected void processRemovedItems() {
  }

  @Override
  protected boolean wasObjectStored(final Object editableObject) {
    return false;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ProjectBundle.message("global.bundles.display.name");
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return myCurrentConfigurable != null ? myCurrentConfigurable.getHelpTopic() : "reference.settingsdialog.project.structure.jdk";
  }

  @Override
  @Nonnull
  @NonNls
  public String getId() {
    return "jdk.list";
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return null;
  }

  @Override
  protected void loadTree() {
    final Map<SdkType, List<Sdk>> map = new HashMap<>();

    for (Sdk sdk : mySdksModel.getModifiedSdksMap().values()) {
      SdkType sdkType = (SdkType)sdk.getSdkType();
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
        final SdkConfigurable configurable = new SdkConfigurable((SdkImpl)sdk, mySdksModel, TREE_UPDATER, myHistory, true);

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

  public boolean addSdkNode(final Sdk sdk, final boolean selectInTree) {
    if (!myUiDisposed) {
      MyNode newSdkNode = new MyNode(new SdkConfigurable((SdkImpl)sdk, mySdksModel, TREE_UPDATER, myHistory, true));

      final MyNode groupNode = MasterDetailsComponent.findNodeByObject(myRoot, sdk.getSdkType());
      if (groupNode != null) {
        addNode(newSdkNode, groupNode);
      }
      else {
        final MyNode sdkGroupNode = createSdkGroupNode((SdkType)sdk.getSdkType());

        addNode(sdkGroupNode, myRoot);
        addNode(newSdkNode, sdkGroupNode);
      }

      if (selectInTree) {
        selectNodeInTree(newSdkNode);
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean canBeRemoved(Object[] editableObjects) {
    for (Object editableObject : editableObjects) {
      if (editableObject instanceof Sdk && ((Sdk)editableObject).isPredefined()) {
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
          myRoot.remove((MutableTreeNode)childAt);
          ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
        }
      }
    }
  }

  @Override
  public void dispose() {
    mySdksModel.removeListener(myListener);
    mySdksModel.disposeUIResources();
  }

  public SettingsSdksModel getSdksModel() {
    return mySdksModel;
  }

  @Override
  public void reset() {
    mySdksModel.reset();

    super.reset();

    myTree.setRootVisible(false);
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    boolean modifiedSdks = false;
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final TreeNode groupNode = myRoot.getChildAt(i);

      for (int k = 0; k < groupNode.getChildCount(); k++) {
        final MyNode sdkNode = (MyNode)groupNode.getChildAt(k);
        final NamedConfigurable configurable = sdkNode.getConfigurable();
        if (configurable.isModified()) {
          configurable.apply();
          modifiedSdks = true;
        }
      }
    }

    if (mySdksModel.isModified() || modifiedSdks) mySdksModel.apply(this);
  }

  @Override
  public boolean isModified() {
    return super.isModified() || mySdksModel.isModified();
  }

  public static SdkListConfigurable getInstance(Project project) {
    return ServiceManager.getService(project, SdkListConfigurable.class);
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
        DefaultActionGroup group = new DefaultActionGroup(ProjectBundle.message("add.action.name"), true);
        mySdksModel.createAddActions(group, myTree, new Consumer<Sdk>() {
          @Override
          public void consume(final Sdk sdk) {
            addSdkNode(sdk, true);
          }
        }, ADD_SDK_FILTER);
        return group.getChildren(null);
      }
    };
  }

  @Override
  protected void removeSdk(final Sdk jdk) {
    mySdksModel.removeSdk(jdk);
  }

  @Override
  @Nullable
  protected String getEmptySelectionString() {
    return ProjectBundle.message("global.bundles.empty.text");
  }
}
