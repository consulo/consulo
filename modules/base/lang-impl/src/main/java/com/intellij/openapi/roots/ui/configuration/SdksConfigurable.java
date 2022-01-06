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

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 16-Aug-2006
 * Time: 16:56:21
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkListConfigurable;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.MasterDetailsStateService;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Conditions;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.Convertor;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.ide.settings.impl.SettingsSdksModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.preferences.MasterDetailsConfigurable;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

public class SdksConfigurable extends MasterDetailsComponent {

  private final SettingsSdksModel mySdksModel;
  private final Project myProject;
  @NonNls
  private static final String SPLITTER_PROPORTION = "project.jdk.splitter";

  public SdksConfigurable(Project project) {
    this(project, ((ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance()).getSdksModel());
  }

  public SdksConfigurable(Project project, SettingsSdksModel sdksModel) {
    myProject = project;
    mySdksModel = sdksModel;
    initTree();
  }

  @Override
  protected String getComponentStateKey() {
    return "SDKs.UI";
  }

  @Override
  protected MasterDetailsStateService getStateService() {
    return MasterDetailsStateService.getInstance(myProject);
  }

  @Override
  protected void initTree() {
    super.initTree();
    new TreeSpeedSearch(myTree, new Convertor<TreePath, String>() {
      @Override
      public String convert(final TreePath treePath) {
        return ((MyNode)treePath.getLastPathComponent()).getDisplayName();
      }
    }, true);

    myTree.setRootVisible(false);
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    super.reset();

    mySdksModel.reset();

    myRoot.removeAllChildren();
    final Map<Sdk, Sdk> sdks = mySdksModel.getModifiedSdksMap();
    for (Sdk sdk : sdks.keySet()) {
      final SdkConfigurable configurable = new SdkConfigurable((SdkImpl)sdks.get(sdk), mySdksModel, TREE_UPDATER);
      addNode(new MyNode(configurable), myRoot);
    }

    final String value = PropertiesComponent.getInstance().getValue(SPLITTER_PROPORTION);
    if (value != null) {
      try {
        final Splitter splitter = extractSplitter();
        if (splitter != null) {
          (splitter).setProportion(Float.parseFloat(value));
        }
      }
      catch (NumberFormatException e) {
        //do not set proportion
      }
    }
  }

  @Nullable
  private Splitter extractSplitter() {
    final Component[] components = myWholePanel.getComponents();
    if (components.length == 1 && components[0] instanceof Splitter) {
      return (Splitter)components[0];
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    boolean modifiedJdks = false;
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final Configurable configurable = ((MyNode)myRoot.getChildAt(i)).getConfigurable();
      if (configurable.isModified()) {
        configurable.apply();
        modifiedJdks = true;
      }
    }

    if (mySdksModel.isModified() || modifiedJdks) mySdksModel.apply(this);
  }


  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return super.isModified() || mySdksModel.isModified();
  }


  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    final Splitter splitter = extractSplitter();
    if (splitter != null) {
      PropertiesComponent.getInstance().setValue(SPLITTER_PROPORTION, String.valueOf(splitter.getProportion()));
    }
    mySdksModel.disposeUIResources();
    super.disposeUIResources();
  }

  @Override
  @Nullable
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    final ArrayList<AnAction> actions = new ArrayList<AnAction>();
    DefaultActionGroup group = new DefaultActionGroup(ProjectBundle.message("add.action.name"), true);
    group.getTemplatePresentation().setIcon(IconUtil.getAddIcon());
    mySdksModel.createAddActions(group, myTree, projectJdk -> {
      addNode(new MyNode(new SdkConfigurable(((SdkImpl)projectJdk), mySdksModel, TREE_UPDATER), false), myRoot);
      selectNodeInTree(findNodeByObject(myRoot, projectJdk));
    }, SdkListConfigurable.ADD_SDK_FILTER);
    actions.add(new MyActionGroupWrapper(group));
    actions.add(new MyDeleteAction(forAll(Conditions.alwaysTrue())));
    return actions;
  }

  @Override
  protected void processRemovedItems() {
    final Set<Sdk> sdks = new HashSet<Sdk>();
    for (int i = 0; i < myRoot.getChildCount(); i++) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)myRoot.getChildAt(i);
      final MasterDetailsConfigurable namedConfigurable = (MasterDetailsConfigurable)node.getUserObject();
      sdks.add(((SdkConfigurable)namedConfigurable).getEditableObject());
    }
    final HashMap<Sdk, Sdk> map = new HashMap<Sdk, Sdk>(mySdksModel.getModifiedSdksMap());
    for (Sdk sdk : map.values()) {
      if (!sdks.contains(sdk)) {
        mySdksModel.removeSdk(sdk);
      }
    }
  }

  @Override
  protected boolean wasObjectStored(Object editableObject) {
    //noinspection RedundantCast
    return mySdksModel.getModifiedSdksMap().containsKey((Sdk)editableObject);
  }

  @Nullable
  public Sdk getSelectedSdk() {
    return (Sdk)getSelectedObject();
  }

  public void selectSdk(final Sdk sdk) {
    selectNodeInTree(sdk);
  }

  @Override
  @Nullable
  public String getDisplayName() {
    return null;
  }

  @Override
  @Nullable
  protected String getEmptySelectionString() {
    return "Select an SDK to view or edit its details here";
  }
}
