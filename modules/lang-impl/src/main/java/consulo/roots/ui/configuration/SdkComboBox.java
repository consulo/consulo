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
package consulo.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkListConfigurable;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.ui.ColoredListCellRendererWrapper;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import com.intellij.util.NullableFunction;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.DeprecationInfo;
import consulo.annotations.Exported;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.bundle.SdkUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @author VISTALL
 * @since May 18, 2005
 */
public class SdkComboBox extends ComboBoxWithWidePopup {
  @Nullable
  private final Condition<SdkTypeId> myFilter;
  @Nullable
  private final Condition<SdkTypeId> myCreationFilter;

  public SdkComboBox(@NotNull final ProjectSdksModel sdksModel) {
    this(sdksModel, null, false);
  }

  public SdkComboBox(@NotNull ProjectSdksModel sdksModel,
                     @Nullable Condition<SdkTypeId> filter,
                     boolean withNoneItem) {
    this(sdksModel, filter, filter, withNoneItem);
  }

  public SdkComboBox(@NotNull ProjectSdksModel sdksModel,
                     @Nullable Condition<SdkTypeId> filter,
                     @Nullable String nullItemName) {
    this(sdksModel, filter, filter, nullItemName, null);
  }

  public SdkComboBox(@NotNull ProjectSdksModel sdksModel,
                     @Nullable Condition<SdkTypeId> filter,
                     @Nullable Condition<SdkTypeId> creationFilter,
                     boolean withNoneItem) {
    this(sdksModel, filter, creationFilter, withNoneItem ? ProjectBundle.message("sdk.combo.box.item") : null, null);
  }

  public SdkComboBox(@NotNull ProjectSdksModel sdksModel,
                     @Nullable Condition<SdkTypeId> filter,
                     @Nullable Condition<SdkTypeId> creationFilter,
                     @Nullable String nullItemName) {
    this(sdksModel, filter, creationFilter, nullItemName, null);
  }

  public SdkComboBox(@NotNull ProjectSdksModel sdksModel,
                     @Nullable Condition<SdkTypeId> filter,
                     @Nullable Condition<SdkTypeId> creationFilter,
                     @Nullable final String nullItemName,
                     @Nullable final Icon nullIcon) {
    super(new SdkComboBoxModel(sdksModel, getSdkFilter(filter), nullItemName));
    myFilter = filter;
    myCreationFilter = creationFilter;
    setRenderer(new ColoredListCellRendererWrapper<SdkComboBoxItem>() {
      @Override
      public void doCustomize(JList list, SdkComboBoxItem value, int index, boolean selected, boolean hasFocus) {
        if (SdkComboBox.this.isEnabled()) {
          setIcon(EmptyIcon.ICON_16);    // to fix vertical size
          if (value instanceof InvalidSdkComboBoxItem) {
            setIcon(AllIcons.Toolbar.Unknown);
            append(value.getSdkName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
          }
          else if(value instanceof CustomSdkComboBoxItem) {
            setIcon(((CustomSdkComboBoxItem)value).getIcon());
            append(((CustomSdkComboBoxItem)value).getPresentableName());
          }
          else if (value instanceof ModuleExtensionSdkComboBoxItem) {
            ModuleExtensionSdkComboBoxItem extensionSdkComboBoxItem = (ModuleExtensionSdkComboBoxItem)value;
            setIcon(AllIcons.Nodes.Module);
            append(extensionSdkComboBoxItem.getModule().getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

            final String sdkName = extensionSdkComboBoxItem.getSdkName();
            if (sdkName != null) {
              append(" (" + extensionSdkComboBoxItem.getSdkName() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
          }
          else if (value instanceof InvalidModuleComboBoxItem) {
            setIcon(AllIcons.Nodes.Module);
            append(((InvalidModuleComboBoxItem)value).getModuleName(), SimpleTextAttributes.ERROR_BOLD_ATTRIBUTES);
          }
          else if (value == null || value instanceof NullSdkComboBoxItem) {
            setIcon(ObjectUtil.notNull(nullIcon, AllIcons.Ide.EmptyFatalError));
            String name = ObjectUtil.notNull(nullItemName, ProjectBundle.message("sdk.combo.box.item"));
            append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
          else {
            Sdk sdk = value.getSdk();
            String sdkName = value.getSdkName();
            assert sdkName != null;
            setIcon(sdk == null ? AllIcons.Toolbar.Unknown : SdkUtil.getIcon(sdk));
            append(sdkName, sdk == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
            String version = sdk == null ? null : sdk.getVersionString();
            if(version != null) {
              append(" (", SimpleTextAttributes.GRAY_ATTRIBUTES);
              append(version, SimpleTextAttributes.GRAY_ATTRIBUTES);
              append(")", SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
          }
        }
      }
    });
  }

  @Override
  public Dimension getPreferredSize() {
    final Rectangle rec = ScreenUtil.getScreenRectangle(0, 0);
    final Dimension size = super.getPreferredSize();
    final int maxWidth = rec.width / 4;
    if (size.width > maxWidth) {
      size.width = maxWidth;
    }
    return size;
  }

  @Override
  public Dimension getMinimumSize() {
    final Dimension minSize = super.getMinimumSize();
    final Dimension prefSize = getPreferredSize();
    if (minSize.width > prefSize.width) {
      minSize.width = prefSize.width;
    }
    return minSize;
  }

  @Deprecated
  @DeprecationInfo(value = "Use #setSetupButton() without 'moduleJdkSetup' parameter", until = "1.0")
  public void setSetupButton(final JButton setUpButton,
                             @Nullable final Project project,
                             final ProjectSdksModel sdksModel,
                             final SdkComboBoxItem firstItem,
                             @Nullable final Condition<Sdk> additionalSetup,
                             final boolean moduleJdkSetup) {
    setSetupButton(setUpButton, project, sdksModel, firstItem, additionalSetup);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #setSetupButton() without 'actionGroupTitle' parameter", until = "1.0")
  public void setSetupButton(final JButton setUpButton,
                             @Nullable final Project project,
                             final ProjectSdksModel sdksModel,
                             final SdkComboBoxItem firstItem,
                             @Nullable final Condition<Sdk> additionalSetup,
                             final String actionGroupTitle) {
    setSetupButton(setUpButton, project, sdksModel, firstItem, additionalSetup);
  }

  public void setSetupButton(@NotNull final JButton setUpButton,
                             @Nullable final Project project,
                             @NotNull final ProjectSdksModel sdksModel,
                             @Nullable final SdkComboBoxItem firstItem,
                             @Nullable final Condition<Sdk> additionalSetup) {
    setUpButton.addActionListener(new ActionListener() {
      @Override
      @RequiredDispatchThread
      public void actionPerformed(ActionEvent e) {
        DefaultActionGroup group = new DefaultActionGroup();
        sdksModel.createAddActions(group, SdkComboBox.this, new Consumer<Sdk>() {
          @Override
          public void consume(final Sdk sdk) {
            if (project != null) {
              final SdkListConfigurable configurable = SdkListConfigurable.getInstance(project);
              configurable.addSdkNode(sdk, false);
            }
            reloadModel(new SdkComboBoxItem(sdk), project);
            setSelectedSdk(sdk); //restore selection
            if (additionalSetup != null) {
              if (additionalSetup.value(sdk)) { //leave old selection
                setSelectedSdk(firstItem.getSdk());
              }
            }
          }
        }, myCreationFilter);
        final DataContext dataContext = DataManager.getInstance().getDataContext(SdkComboBox.this);
        if (group.getChildrenCount() > 1) {
          JBPopupFactory.getInstance().createActionGroupPopup(ProjectBundle.message("set.up.jdk.title"), group, dataContext,
                                                              JBPopupFactory.ActionSelectionAid.MNEMONICS, false)
                  .showUnderneathOf(setUpButton);
        }
        else {
          final AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, new Presentation(""),
                                                        ActionManager.getInstance(), 0);
          group.getChildren(event)[0].actionPerformed(event);
        }
      }
    });
  }

  public void setEditButton(final JButton editButton, final Project project, @NotNull final Computable<Sdk> retrieveSdk) {
    editButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Sdk sdk = retrieveSdk.compute();
        if (sdk != null) {
          ProjectStructureConfigurable.getInstance(project).select(sdk, true);
        }
      }
    });
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final SdkComboBoxItem selectedItem = getSelectedItem();
        editButton.setEnabled(!(selectedItem instanceof InvalidSdkComboBoxItem) &&
                              selectedItem != null &&
                              selectedItem.getSdk() != null);
      }
    });
  }

  @Override
  public SdkComboBoxItem getSelectedItem() {
    return (SdkComboBoxItem)super.getSelectedItem();
  }

  @Exported
  public void insertCustomSdkItem(@NotNull String key, @NotNull String presentableName, @NotNull Icon icon) {
    CustomSdkComboBoxItem sdkComboBoxItem = new CustomSdkComboBoxItem(key, presentableName, icon);
    int itemCount = getItemCount();
    if(itemCount > 0) {
      int index = 0;
      for (int i = 0; i < itemCount; i++) {
        Object itemAt = getItemAt(i);

        if(itemAt instanceof NullSdkComboBoxItem || itemAt instanceof CustomSdkComboBoxItem) {
          index ++;
        }
      }
      SdkComboBoxModel model = (SdkComboBoxModel)getModel();
      model.insertElementAt(sdkComboBoxItem, index);
    }
    else {
      addItem(sdkComboBoxItem);
    }
  }

  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public <T extends MutableModuleExtension<?>> void insertModuleItems(@NotNull T moduleExtension,
                                                                      @NotNull NullableFunction<T, MutableModuleInheritableNamedPointer<Sdk>> sdkPointerFunction) {

    for (Module module : ModuleManager.getInstance(moduleExtension.getModule().getProject()).getModules()) {
      // dont add self module
      if (module == moduleExtension.getModule()) {
        continue;
      }

      ModuleExtension extension = ModuleUtilCore.getExtension(module, moduleExtension.getId());
      if (extension == null) {
        continue;
      }
      MutableModuleInheritableNamedPointer<Sdk> sdkPointer = sdkPointerFunction.fun((T)extension);
      if (sdkPointer != null) {
        // recursive depend
        if (sdkPointer.getModule() == moduleExtension.getModule()) {
          continue;
        }
        addItem(new ModuleExtensionSdkComboBoxItem(extension, sdkPointer));
      }
    }
  }

  @Nullable
  public Sdk getSelectedSdk() {
    final SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
    return selectedItem != null ? selectedItem.getSdk() : null;
  }

  @Nullable
  public String getSelectedSdkName() {
    final SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
    if (selectedItem != null) {
      return selectedItem.getSdkName();
    }
    return null;
  }

  @Nullable
  public String getSelectedModuleName() {
    final SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
    if (selectedItem instanceof ModuleExtensionSdkComboBoxItem) {
      return ((ModuleExtensionSdkComboBoxItem)selectedItem).getModule().getName();
    }
    else if (selectedItem instanceof InvalidModuleComboBoxItem) {
      return ((InvalidModuleComboBoxItem)selectedItem).getModuleName();
    }
    return null;
  }

  public void setSelectedSdk(Sdk sdk) {
    final int index = indexOf(sdk);
    if (index >= 0) {
      setSelectedIndex(index);
    }
  }

  public void setSelectedModule(@NotNull String name) {
    final int index = indexOfModuleItems(name);
    if (index >= 0) {
      setSelectedIndex(index);
    }
  }

  public void setInvalidSdk(String name) {
    removeInvalidElement();
    addItem(new InvalidSdkComboBoxItem(name));
    setSelectedIndex(getModel().getSize() - 1);
  }

  public void addInvalidModuleItem(String name) {
    addItem(new InvalidModuleComboBoxItem(name));
  }

  public void setSelectedNoneSdk() {
    if (getItemCount() > 0 && getItemAt(0) instanceof NullSdkComboBoxItem) {
      setSelectedIndex(0);
    }
  }

  public void setSelectedSdk(@Nullable String name) {
    if (name != null) {
      final SdkComboBoxModel model = (SdkComboBoxModel)getModel();

      int itemCount = getItemCount();
      for (int i = 0; i < itemCount; i++) {
        SdkComboBoxItem itemAt = model.getElementAt(i);
        String sdkName = itemAt.getSdkName();
        if(name.equals(sdkName))  {
          setSelectedItem(itemAt);
          return;
        }
      }

      setInvalidSdk(name);
    }
    else {
      if (getItemCount() > 0 && getItemAt(0) instanceof NullSdkComboBoxItem) {
        setSelectedIndex(0);
      }
      else {
        setInvalidSdk("null");
      }
    }
  }

  private int indexOf(Sdk sdk) {
    final SdkComboBoxModel model = (SdkComboBoxModel)getModel();
    final int count = model.getSize();
    for (int idx = 0; idx < count; idx++) {
      final SdkComboBoxItem elementAt = model.getElementAt(idx);
      if (sdk == null) {
        if (elementAt instanceof NullSdkComboBoxItem || elementAt instanceof ModuleExtensionSdkComboBoxItem) {
          return idx;
        }
      }
      else {
        Sdk elementAtSdk = elementAt.getSdk();
        if (elementAtSdk != null && sdk.getName().equals(elementAtSdk.getName())) {
          return idx;
        }
      }
    }
    return -1;
  }

  private int indexOfModuleItems(String moduleName) {
    final SdkComboBoxModel model = (SdkComboBoxModel)getModel();
    final int count = model.getSize();
    for (int idx = 0; idx < count; idx++) {
      final SdkComboBoxItem elementAt = model.getElementAt(idx);
      if (elementAt instanceof ModuleExtensionSdkComboBoxItem) {
        final String name = ((ModuleExtensionSdkComboBoxItem)elementAt).getModule().getName();
        if (name.equals(moduleName)) {
          return idx;
        }
      }
      else if (elementAt instanceof InvalidModuleComboBoxItem) {
        if (((InvalidModuleComboBoxItem)elementAt).getModuleName().equals(moduleName)) {
          return idx;
        }
      }
    }
    return -1;
  }

  private void removeInvalidElement() {
    final SdkComboBoxModel model = (SdkComboBoxModel)getModel();
    final int count = model.getSize();
    for (int idx = 0; idx < count; idx++) {
      final SdkComboBoxItem elementAt = model.getElementAt(idx);
      if (elementAt instanceof InvalidSdkComboBoxItem) {
        removeItemAt(idx);
        break;
      }
    }
  }

  public void reloadModel(SdkComboBoxItem firstItem, @Nullable Project project) {
    final DefaultComboBoxModel model = ((DefaultComboBoxModel)getModel());
    if (project == null) {
      model.addElement(firstItem);
      return;
    }
    model.removeAllElements();
    model.addElement(firstItem);
    final ProjectSdksModel projectSdksModel = ProjectStructureConfigurable.getInstance(project).getProjectSdksModel();
    List<Sdk> sdks = new ArrayList<Sdk>(projectSdksModel.getProjectSdks().values());
    if (myFilter != null) {
      sdks = ContainerUtil.filter(sdks, getSdkFilter(myFilter));
    }
    Collections.sort(sdks, new Comparator<Sdk>() {
      @Override
      public int compare(final Sdk o1, final Sdk o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    });
    for (Sdk sdk : sdks) {
      model.addElement(new SdkComboBoxItem(sdk));
    }
  }

  private static class SdkComboBoxModel extends DefaultComboBoxModel {
    public SdkComboBoxModel(@NotNull SdkModel sdksModel,
                            @Nullable Condition<Sdk> sdkFilter,
                            @Nullable String noneItemName) {
      Sdk[] sdks = sdksModel.getSdks();
      if (sdkFilter != null) {
        final List<Sdk> filtered = ContainerUtil.filter(sdks, sdkFilter);
        sdks = filtered.toArray(new Sdk[filtered.size()]);
      }
      Arrays.sort(sdks, new Comparator<Sdk>() {
        @Override
        public int compare(final Sdk s1, final Sdk s2) {
          return s1.getName().compareToIgnoreCase(s2.getName());
        }
      });

      if (noneItemName != null) {
        addElement(new NullSdkComboBoxItem());
      }

      for (Sdk sdk : sdks) {
        addElement(new SdkComboBoxItem(sdk));
      }
    }

    // implements javax.swing.ListModel
    @Override
    public SdkComboBoxItem getElementAt(int index) {
      return (SdkComboBoxItem)super.getElementAt(index);
    }
  }

  private static Condition<Sdk> getSdkFilter(@Nullable final Condition<SdkTypeId> filter) {
    return filter == null ? Conditions.<Sdk>alwaysTrue() : new Condition<Sdk>() {
      @Override
      public boolean value(Sdk sdk) {
        return filter.value(sdk.getSdkType());
      }
    };
  }

  public static class SdkComboBoxItem {
    private final Sdk mySdk;

    public SdkComboBoxItem(@Nullable Sdk sdk) {
      mySdk = sdk;
    }

    public Sdk getSdk() {
      return mySdk;
    }

    @Nullable
    public String getSdkName() {
      return mySdk != null ? mySdk.getName() : null;
    }
  }

  public static class ModuleExtensionSdkComboBoxItem extends SdkComboBoxItem {
    private final ModuleExtension<?> myModuleExtension;
    private final MutableModuleInheritableNamedPointer<Sdk> mySdkPointer;

    public ModuleExtensionSdkComboBoxItem(ModuleExtension<?> moduleExtension,
                                          MutableModuleInheritableNamedPointer<Sdk> sdkPointer) {
      super(null);
      myModuleExtension = moduleExtension;
      mySdkPointer = sdkPointer;
    }

    @Override
    public Sdk getSdk() {
      return mySdkPointer.get();
    }

    @Nullable
    @Override
    public String getSdkName() {
      return mySdkPointer.getName();
    }

    @NotNull
    public Module getModule() {
      return myModuleExtension.getModule();
    }
  }

  public static class CustomSdkComboBoxItem extends SdkComboBoxItem {
    private final String myKey;
    private final String myPresentableName;
    private final Icon myIcon;

    public CustomSdkComboBoxItem(String key, String presentableName, Icon icon) {
      super(null);
      myKey = key;
      myPresentableName = presentableName;
      myIcon = icon;
    }

    @NotNull
    public Icon getIcon() {
      return myIcon;
    }

    @NotNull
    public String getPresentableName() {
      return myPresentableName;
    }

    @Nullable
    @Override
    public String getSdkName() {
      return myKey;
    }
  }

  public static class InvalidModuleComboBoxItem extends SdkComboBoxItem {
    private final String myModuleName;

    public InvalidModuleComboBoxItem(String moduleName) {
      super(null);
      myModuleName = moduleName;
    }

    @NotNull
    public String getModuleName() {
      return myModuleName;
    }
  }

  public static class NullSdkComboBoxItem extends SdkComboBoxItem {
    public NullSdkComboBoxItem() {
      super(null);
    }
  }

  private static class InvalidSdkComboBoxItem extends SdkComboBoxItem {
    private final String mySdkName;

    public InvalidSdkComboBoxItem(String name) {
      super(null);
      mySdkName = name;
    }

    @Override
    public String getSdkName() {
      return mySdkName;
    }
  }
}
