/*
 * Copyright 2013-2017 consulo.io
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
package consulo.bundle.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.util.ObjectUtil;
import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.bundle.BundleHolder;
import consulo.bundle.SdkUtil;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.ui.ComboBox;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.TextAttribute;
import consulo.ui.image.Image;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 05-Feb-17
 * <p>
 * Cross-Platform version of {@link consulo.roots.ui.configuration.SdkComboBox}
 */
public class BundleBox implements PseudoComponent {
  public static class BundleBoxItem {
    private final Sdk myBundle;

    public BundleBoxItem(@Nullable Sdk bundle) {
      myBundle = bundle;
    }

    @Nullable
    public Sdk getBundle() {
      return myBundle;
    }

    @Nullable
    public String getBundleName() {
      return myBundle != null ? myBundle.getName() : null;
    }
  }

  public static class ModuleExtensionBundleBoxItem extends BundleBoxItem {
    private final ModuleExtension<?> myModuleExtension;
    private final MutableModuleInheritableNamedPointer<Sdk> mySdkPointer;

    public ModuleExtensionBundleBoxItem(ModuleExtension<?> moduleExtension, MutableModuleInheritableNamedPointer<Sdk> sdkPointer) {
      super(null);
      myModuleExtension = moduleExtension;
      mySdkPointer = sdkPointer;
    }

    @Override
    public Sdk getBundle() {
      return mySdkPointer.get();
    }

    @Nullable
    @Override
    public String getBundleName() {
      return mySdkPointer.getName();
    }

    @Nonnull
    public Module getModule() {
      return myModuleExtension.getModule();
    }
  }

  public static class CustomBundleBoxItem extends BundleBoxItem {
    private final String myKey;
    private final String myPresentableName;
    private final Image myIcon;

    public CustomBundleBoxItem(String key, String presentableName, Image icon) {
      super(null);
      myKey = key;
      myPresentableName = presentableName;
      myIcon = icon;
    }

    @Nonnull
    public Image getIcon() {
      return myIcon;
    }

    @Nonnull
    public String getPresentableName() {
      return myPresentableName;
    }

    @Nullable
    @Override
    public String getBundleName() {
      return myKey;
    }
  }

  public static class InvalidModuleBundleBoxItem extends BundleBoxItem {
    private final String myModuleName;

    public InvalidModuleBundleBoxItem(String moduleName) {
      super(null);
      myModuleName = moduleName;
    }

    @Nonnull
    public String getModuleName() {
      return myModuleName;
    }
  }

  public static class NullBundleBoxItem extends BundleBoxItem {
    public NullBundleBoxItem() {
      super(null);
    }
  }

  private static class InvalidBundleBoxItem extends BundleBoxItem {
    private final String mySdkName;

    public InvalidBundleBoxItem(String name) {
      super(null);
      mySdkName = name;
    }

    @Override
    public String getBundleName() {
      return mySdkName;
    }
  }

  private final ComboBox<BundleBoxItem> myOriginalComboBox;

  public BundleBox() {
    this(BundleHolder.EMPTY, null, false);
  }

  public BundleBox(@Nonnull BundleHolder bundleHolder) {
    this(bundleHolder, null, false);
  }

  public BundleBox(@Nonnull BundleHolder bundleHolder, @Nullable Predicate<SdkTypeId> filter, boolean withNoneItem) {
    this(bundleHolder, filter, withNoneItem ? ProjectBundle.message("sdk.combo.box.item") : null, null);
  }

  public BundleBox(@Nonnull BundleHolder bundleHolder, @Nullable Predicate<SdkTypeId> filter, @Nullable String nullItemName) {
    this(bundleHolder, filter, nullItemName, null);
  }

  public BundleBox(@Nonnull BundleHolder bundleHolder, @Nullable Predicate<SdkTypeId> filter, @Nullable final String nullItemName, @Nullable final Image nullIcon) {
    myOriginalComboBox = ComboBox.create(model(bundleHolder, filter, nullItemName));

    myOriginalComboBox.setRender((render, index, value) -> {
      if (value instanceof InvalidBundleBoxItem) {
        render.setIcon(AllIcons.Toolbar.Unknown);
        render.append(value.getBundleName(), TextAttribute.ERROR);
      }
      else if (value instanceof CustomBundleBoxItem) {
        render.setIcon(((CustomBundleBoxItem)value).getIcon());
        render.append(((CustomBundleBoxItem)value).getPresentableName());
      }
      else if (value instanceof ModuleExtensionBundleBoxItem) {
        ModuleExtensionBundleBoxItem extensionSdkComboBoxItem = (ModuleExtensionBundleBoxItem)value;
        render.setIcon(AllIcons.Nodes.Module);
        render.append(extensionSdkComboBoxItem.getModule().getName(), TextAttribute.REGULAR_BOLD);

        final String sdkName = extensionSdkComboBoxItem.getBundleName();
        if (sdkName != null) {
          render.append(" (" + extensionSdkComboBoxItem.getBundleName() + ")", TextAttribute.GRAYED);
        }
      }
      else if (value instanceof InvalidModuleBundleBoxItem) {
        render.setIcon(AllIcons.Nodes.Module);
        render.append(((InvalidModuleBundleBoxItem)value).getModuleName(), TextAttribute.ERROR_BOLD);
      }
      else if (value == null || value instanceof NullBundleBoxItem) {
        render.setIcon(ObjectUtil.notNull(nullIcon, AllIcons.Ide.EmptyFatalError));
        String name = ObjectUtil.notNull(nullItemName, ProjectBundle.message("sdk.combo.box.item"));
        render.append(name, TextAttribute.REGULAR);
      }
      else {
        Sdk sdk = value.getBundle();
        String sdkName = value.getBundleName();
        assert sdkName != null;
        render.setIcon(sdk == null ? AllIcons.Toolbar.Unknown : SdkUtil.getIcon(sdk));
        render.append(sdkName, sdk == null ? TextAttribute.ERROR : TextAttribute.REGULAR);
        String version = sdk == null ? null : sdk.getVersionString();
        if (version != null) {
          render.append(" (", TextAttribute.GRAY);
          render.append(version, TextAttribute.GRAY);
          render.append(")", TextAttribute.GRAY);
        }
      }
    });
  }

  @Nonnull
  private static ListModel<BundleBoxItem> model(@Nonnull BundleHolder holder, @Nullable Predicate<SdkTypeId> filter, String nullItemName) {
    List<BundleBoxItem> list = new ArrayList<>();

    Sdk[] sdks = holder.getBundles();
    if (filter != null) {
      List<Sdk> filtered = Arrays.stream(sdks).filter(sdk -> filter.test(sdk.getSdkType())).collect(Collectors.toList());
      sdks = filtered.toArray(new Sdk[filtered.size()]);
    }

    Arrays.sort(sdks, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));

    if (nullItemName != null) {
      list.add(new NullBundleBoxItem());
    }

    for (Sdk sdk : sdks) {
      list.add(new BundleBoxItem(sdk));
    }

    return MutableListModel.create(list);
  }

  public void addInvalidModuleItem(@Nullable String name) {
    MutableListModel<BundleBoxItem> listModel = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    listModel.add(new InvalidModuleBundleBoxItem(name));
  }

  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public <T extends MutableModuleExtension<?>> void addModuleExtensionItems(@Nonnull T moduleExtension, @Nonnull Function<T, MutableModuleInheritableNamedPointer<Sdk>> sdkPointerFunction) {
    MutableListModel<BundleBoxItem> listModel = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    for (Module module : ModuleManager.getInstance(moduleExtension.getModule().getProject()).getModules()) {
      // dont add self module
      if (module == moduleExtension.getModule()) {
        continue;
      }

      ModuleExtension extension = ModuleUtilCore.getExtension(module, moduleExtension.getId());
      if (extension == null) {
        continue;
      }
      MutableModuleInheritableNamedPointer<Sdk> sdkPointer = sdkPointerFunction.apply((T)extension);
      if (sdkPointer != null) {
        // recursive depend
        if (sdkPointer.getModule() == moduleExtension.getModule()) {
          continue;
        }

        listModel.add(new ModuleExtensionBundleBoxItem(extension, sdkPointer));
      }
    }
  }

  public void addBundleItem(@Nonnull Sdk bundle) {
    MutableListModel<BundleBoxItem> model = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    model.add(new BundleBoxItem(bundle));
  }

  @UsedInPlugin
  public void addCustomBundleItem(@Nonnull String key, @Nonnull String presentableName, @Nonnull Image icon) {
    CustomBundleBoxItem item = new CustomBundleBoxItem(key, presentableName, icon);

    MutableListModel<BundleBoxItem> model = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    int itemCount = model.getSize();
    if (itemCount > 0) {
      int index = 0;
      for (int i = 0; i < itemCount; i++) {
        Object itemAt = model.get(i);

        if (itemAt instanceof NullBundleBoxItem || itemAt instanceof CustomBundleBoxItem) {
          index++;
        }
      }
      model.add(item, index);
    }
    else {
      model.add(item);
    }
  }

  public void setSelectedModule(@Nonnull String name) {
    final int index = indexOfModuleItems(name);
    if (index >= 0) {
      myOriginalComboBox.setValueByIndex(index);
    }
  }

  @RequiredUIAccess
  public void setSelectedBundle(@Nullable String name) {
    ListModel<BundleBoxItem> model = myOriginalComboBox.getListModel();
    if (name != null) {
      int itemCount = model.getSize();
      for (int i = 0; i < itemCount; i++) {
        BundleBoxItem itemAt = model.get(i);
        String sdkName = itemAt.getBundleName();
        if (name.equals(sdkName)) {
          myOriginalComboBox.setValue(itemAt);
          return;
        }
      }

      setInvalidBundle(name);
    }
    else {
      if (model.getSize() > 0 && model.get(0) instanceof NullBundleBoxItem) {
        myOriginalComboBox.setValueByIndex(0);
      }
      else {
        setInvalidBundle("null");
      }
    }
  }

  @RequiredUIAccess
  public void setInvalidBundle(@Nonnull String name) {
    removeInvalidElement();
    MutableListModel<BundleBoxItem> model = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();
    InvalidBundleBoxItem item = new InvalidBundleBoxItem(name);
    model.add(item);

    myOriginalComboBox.setValue(item);
  }

  private void removeInvalidElement() {
    MutableListModel<BundleBoxItem> model = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();
    final int count = model.getSize();

    for (int idx = 0; idx < count; idx++) {
      final BundleBoxItem elementAt = model.get(idx);
      if (elementAt instanceof InvalidBundleBoxItem) {
        model.remove(elementAt);
        break;
      }
    }
  }

  private int indexOfModuleItems(String moduleName) {
    ListModel<BundleBoxItem> model = myOriginalComboBox.getListModel();
    final int count = model.getSize();
    for (int idx = 0; idx < count; idx++) {
      final BundleBoxItem elementAt = model.get(idx);
      if (elementAt instanceof ModuleExtensionBundleBoxItem) {
        final String name = ((ModuleExtensionBundleBoxItem)elementAt).getModule().getName();
        if (name.equals(moduleName)) {
          return idx;
        }
      }
      else if (elementAt instanceof InvalidModuleBundleBoxItem) {
        if (((InvalidModuleBundleBoxItem)elementAt).getModuleName().equals(moduleName)) {
          return idx;
        }
      }
    }
    return -1;
  }


  public void setSelectedNoneBundle() {
    ListModel<BundleBoxItem> listModel = myOriginalComboBox.getListModel();
    if (listModel.getSize() > 0 && listModel.get(0) instanceof NullBundleBoxItem) {
      myOriginalComboBox.setValueByIndex(0);
    }
  }

  @Nullable
  public String getSelectedBundleName() {
    BundleBoxItem value = myOriginalComboBox.getValue();
    if (value != null) {
      return value.getBundleName();
    }
    return null;
  }

  @Nullable
  public String getSelectedModuleName() {
    BundleBoxItem value = myOriginalComboBox.getValue();
    if (value instanceof ModuleExtensionBundleBoxItem) {
      return ((ModuleExtensionBundleBoxItem)value).getModule().getName();
    }
    else if (value instanceof InvalidModuleBundleBoxItem) {
      return ((InvalidModuleBundleBoxItem)value).getModuleName();
    }
    return null;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ComboBox<BundleBoxItem> getComponent() {
    return myOriginalComboBox;
  }
}
