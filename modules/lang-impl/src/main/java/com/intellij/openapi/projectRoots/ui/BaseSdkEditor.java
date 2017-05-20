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
package com.intellij.openapi.projectRoots.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author MYakovlev
 * @since Aug 15, 2002
 */
public abstract class BaseSdkEditor implements Configurable, Place.Navigator {
  public static final Logger LOGGER = Logger.getInstance(BaseSdkEditor.class);

  @NotNull
  protected final Sdk mySdk;
  private final Map<OrderRootType, SdkPathEditor> myPathEditors = new HashMap<OrderRootType, SdkPathEditor>();

  private TextFieldWithBrowseButton myHomeComponent;
  private final Map<SdkType, AdditionalDataConfigurable> myAdditionalDataConfigurables = new HashMap<SdkType, AdditionalDataConfigurable>();
  private final Map<AdditionalDataConfigurable, JComponent> myAdditionalDataComponents =
    new HashMap<AdditionalDataConfigurable, JComponent>();
  private JPanel myAdditionalDataPanel;
  private final SdkModificator myEditedSdkModificator = new EditedSdkModificator();

  // GUI components
  private JPanel myMainPanel;

  @NotNull
  private final SdkModel mySdkModel;
  private JLabel myHomeFieldLabel;
  private String myVersionString;

  private String myInitialName;
  private String myInitialPath;

  protected final Disposable myDisposable = Disposer.newDisposable();

  public BaseSdkEditor(@NotNull SdkModel sdkModel, @NotNull SdkImpl sdk) {
    mySdkModel = sdkModel;
    mySdk = sdk;
    createMainPanel();
    initSdk(sdk);
  }

  private void initSdk(@NotNull Sdk sdk) {
    myInitialName = mySdk.getName();
    myInitialPath = mySdk.getHomePath();

    final AdditionalDataConfigurable additionalDataConfigurable = getAdditionalDataConfigurable();
    if (additionalDataConfigurable != null) {
      additionalDataConfigurable.setSdk(sdk);
    }
    if (myMainPanel != null) {
      reset();
    }
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("sdk.configure.editor.title");
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myMainPanel;
  }

  private void createMainPanel() {
    myMainPanel = new JPanel(new GridBagLayout());

    for (OrderRootType type : OrderRootType.getAllTypes()) {
      if (showTabForType(type)) {
        final OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.FACTORY.getByKey(type);
        if(factory == null) {
          LOGGER.error("OrderRootTypeUIFactory is not defined for order root type: " + type);
          continue;
        }
        final SdkPathEditor pathEditor = factory.createPathEditor(mySdk);
        if (pathEditor != null) {
          pathEditor.setAddBaseDir(mySdk.getHomeDirectory());
          myPathEditors.put(type, pathEditor);
        }
      }
    }

    JComponent centerComponent = createCenterComponent();

    myHomeComponent = createHomeComponent();
    myHomeComponent.getTextField().setEditable(false);
    myHomeComponent.getButton().setVisible(!mySdk.isPredefined() && ((SdkType)mySdk.getSdkType()).supportsUserAdd());

    myHomeFieldLabel = new JLabel(getHomeFieldLabelValue());
    myMainPanel.add(myHomeFieldLabel,
                    new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                                           new Insets(2, 10, 2, 2), 0, 0));
    myMainPanel.add(myHomeComponent, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                                                            GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 10), 0, 0));

    myAdditionalDataPanel = new JPanel(new BorderLayout());
    myMainPanel.add(myAdditionalDataPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                                                                  GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));

    myMainPanel.add(centerComponent,
                    new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                                           GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
  }

  @NotNull
  protected abstract JComponent createCenterComponent();

  protected TextFieldWithBrowseButton createHomeComponent() {
    return new TextFieldWithBrowseButton(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSelectHomePath();
      }
    });
  }

  protected boolean showTabForType(OrderRootType type) {
    return ((SdkType)mySdk.getSdkType()).isRootTypeApplicable(type);
  }

  private String getHomeFieldLabelValue() {
    return ProjectBundle.message("sdk.configure.type.home.path", ((SdkType)mySdk.getSdkType()).getPresentableName());
  }

  @NotNull
  public SdkPathEditor getPathEditor(@NotNull OrderRootType rootType) {
    return myPathEditors.get(rootType);
  }

  @Override
  public boolean isModified() {
    boolean isModified = !Comparing.equal(mySdk.getName(), myInitialName);
    isModified =
      isModified || !Comparing.equal(FileUtil.toSystemIndependentName(getHomeValue()), FileUtil.toSystemIndependentName(myInitialPath));
    for (PathEditor pathEditor : myPathEditors.values()) {
      isModified = isModified || pathEditor.isModified();
    }
    final AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
    if (configurable != null) {
      isModified = isModified || configurable.isModified();
    }
    return isModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    if (!Comparing.equal(myInitialName, mySdk.getName())) {
      if (mySdk.getName().isEmpty()) {
        throw new ConfigurationException(ProjectBundle.message("sdk.list.name.required.error"));
      }
    }
    myInitialName = mySdk.getName();
    myInitialPath = mySdk.getHomePath();
    final SdkModificator sdkModificator = mySdk.getSdkModificator();
    SdkType sdkType = (SdkType)mySdk.getSdkType();
    // we can change home path only when user can add sdk via interface
    if(sdkType.supportsUserAdd()) {
      sdkModificator.setHomePath(getHomeValue().replace(File.separatorChar, '/'));
    }
    for (SdkPathEditor pathEditor : myPathEditors.values()) {
      pathEditor.apply(sdkModificator);
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        sdkModificator.commitChanges();
      }
    });
    final AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
    if (configurable != null) {
      configurable.apply();
    }
  }

  @Override
  public void reset() {
    final SdkModificator sdkModificator = mySdk.getSdkModificator();
    for (OrderRootType type : myPathEditors.keySet()) {
      myPathEditors.get(type).reset(sdkModificator);
    }
    sdkModificator.commitChanges();
    setHomePathValue(mySdk.getHomePath().replace('/', File.separatorChar));
    myVersionString = null;
    myHomeFieldLabel.setText(getHomeFieldLabelValue());
    updateAdditionalDataComponent();
    final AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
    if (configurable != null) {
      configurable.reset();
    }
  }

  @Override
  public void disposeUIResources() {
    for (final SdkType sdkType : myAdditionalDataConfigurables.keySet()) {
      final AdditionalDataConfigurable configurable = myAdditionalDataConfigurables.get(sdkType);
      configurable.disposeUIResources();
    }
    myAdditionalDataConfigurables.clear();
    myAdditionalDataComponents.clear();

    Disposer.dispose(myDisposable);
  }

  private String getHomeValue() {
    return myHomeComponent.getText().trim();
  }

  private void clearAllPaths() {
    for (PathEditor editor : myPathEditors.values()) {
      editor.clearList();
    }
  }

  private void setHomePathValue(String absolutePath) {
    myHomeComponent.setText(absolutePath);
    final Color fg;
    if (absolutePath != null && !absolutePath.isEmpty()) {
      final File homeDir = new File(absolutePath);
      boolean homeMustBeDirectory = ((SdkType)mySdk.getSdkType()).getHomeChooserDescriptor().isChooseFolders();
      fg = homeDir.exists() && homeDir.isDirectory() == homeMustBeDirectory ? UIUtil.getFieldForegroundColor() : PathEditor.INVALID_COLOR;
    }
    else {
      fg = UIUtil.getFieldForegroundColor();
    }
    myHomeComponent.getTextField().setForeground(fg);
  }

  private void doSelectHomePath() {
    final SdkType sdkType = (SdkType)mySdk.getSdkType();
    SdkConfigurationUtil.selectSdkHome(sdkType, new Consumer<String>() {
      @Override
      public void consume(final String path) {
        doSetHomePath(path, sdkType);
      }
    });

  }

  private void doSetHomePath(final String homePath, final SdkType sdkType) {
    if (homePath == null) {
      return;
    }
    setHomePathValue(homePath.replace('/', File.separatorChar));

    final String newSdkName = suggestSdkName(homePath);
    ((SdkImpl)mySdk).setName(newSdkName);

    try {
      final Sdk dummySdk = (Sdk)mySdk.clone();
      SdkModificator sdkModificator = dummySdk.getSdkModificator();
      sdkModificator.setHomePath(homePath);
      sdkModificator.removeAllRoots();
      sdkModificator.commitChanges();

      sdkType.setupSdkPaths(dummySdk);

      clearAllPaths();
      myVersionString = dummySdk.getVersionString();
      if (myVersionString == null) {
        Messages
          .showMessageDialog(ProjectBundle.message("sdk.java.corrupt.error", homePath), ProjectBundle.message("sdk.java.corrupt.title"),
                             Messages.getErrorIcon());
      }
      sdkModificator = dummySdk.getSdkModificator();
      for (OrderRootType type : myPathEditors.keySet()) {
        myPathEditors.get(type).addPaths(sdkModificator.getRoots(type));
      }
      mySdkModel.getMulticaster().sdkHomeSelected(dummySdk, homePath);
    }
    catch (CloneNotSupportedException e) {
      BaseSdkEditor.LOGGER.error(e); // should not happen in normal program
    }
  }

  private String suggestSdkName(final String homePath) {
    final String currentName = mySdk.getName();
    final String suggestedName = ((SdkType)mySdk.getSdkType()).suggestSdkName(currentName, homePath);
    if (Comparing.equal(currentName, suggestedName)) return currentName;
    String newSdkName = suggestedName;
    final Set<String> allNames = new HashSet<String>();
    Sdk[] sdks = mySdkModel.getSdks();
    for (Sdk sdk : sdks) {
      allNames.add(sdk.getName());
    }
    int i = 0;
    while (allNames.contains(newSdkName)) {
      newSdkName = suggestedName + " (" + ++i + ")";
    }
    return newSdkName;
  }

  private void updateAdditionalDataComponent() {
    myAdditionalDataPanel.removeAll();
    final AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
    if (configurable != null) {
      JComponent component = myAdditionalDataComponents.get(configurable);
      if (component == null) {
        component = configurable.createComponent();
        myAdditionalDataComponents.put(configurable, component);
      }
      myAdditionalDataPanel.add(component, BorderLayout.CENTER);
    }
  }

  @Nullable
  private AdditionalDataConfigurable getAdditionalDataConfigurable() {
    return initAdditionalDataConfigurable(mySdk);
  }

  @Nullable
  private AdditionalDataConfigurable initAdditionalDataConfigurable(Sdk sdk) {
    final SdkType sdkType = (SdkType)sdk.getSdkType();
    AdditionalDataConfigurable configurable = myAdditionalDataConfigurables.get(sdkType);
    if (configurable == null) {
      configurable = sdkType.createAdditionalDataConfigurable(mySdkModel, myEditedSdkModificator);
      if (configurable != null) {
        myAdditionalDataConfigurables.put(sdkType, configurable);
      }
    }
    return configurable;
  }

  private class EditedSdkModificator implements SdkModificator {
    @Override
    public String getName() {
      return mySdk.getName();
    }

    @Override
    public void setName(String name) {
      ((SdkImpl)mySdk).setName(name);
    }

    @Override
    public String getHomePath() {
      return getHomeValue();
    }

    @Override
    public void setHomePath(String path) {
      doSetHomePath(path, (SdkType)mySdk.getSdkType());
    }

    @Override
    public String getVersionString() {
      return myVersionString != null ? myVersionString : mySdk.getVersionString();
    }

    @Override
    public void setVersionString(String versionString) {
      throw new UnsupportedOperationException(); // not supported for this editor
    }

    @Override
    public SdkAdditionalData getSdkAdditionalData() {
      return mySdk.getSdkAdditionalData();
    }

    @Override
    public void setSdkAdditionalData(SdkAdditionalData data) {
      throw new UnsupportedOperationException(); // not supported for this editor
    }

    @Override
    public VirtualFile[] getRoots(OrderRootType rootType) {
      final PathEditor editor = myPathEditors.get(rootType);
      if (editor == null) {
        throw new IllegalStateException("no editor for root type " + rootType);
      }
      return editor.getRoots();
    }

    @Override
    public void addRoot(@NotNull VirtualFile root, @NotNull OrderRootType rootType) {
      myPathEditors.get(rootType).addPaths(root);
    }

    @Override
    public void removeRoot(@NotNull VirtualFile root, @NotNull OrderRootType rootType) {
      myPathEditors.get(rootType).removePaths(root);
    }

    @Override
    public void removeRoots(OrderRootType rootType) {
      myPathEditors.get(rootType).clearList();
    }

    @Override
    public void removeAllRoots() {
      for (PathEditor editor : myPathEditors.values()) {
        editor.clearList();
      }
    }

    @Override
    public void commitChanges() {
    }

    @Override
    public boolean isWritable() {
      return true;
    }
  }

  @Override
  public ActionCallback navigateTo(@Nullable final Place place, final boolean requestFocus) {
    return new ActionCallback.Done();
  }

  @Override
  public void queryPlace(@NotNull final Place place) {
  }

  @Override
  public void setHistory(@NotNull final History history) {
  }
}
