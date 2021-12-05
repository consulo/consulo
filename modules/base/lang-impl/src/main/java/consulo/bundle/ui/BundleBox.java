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
import consulo.disposer.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.util.ObjectUtil;
import consulo.annotation.UsedInPlugin;
import consulo.bundle.BundleHolder;
import consulo.bundle.SdkUtil;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.ui.ComboBox;
import consulo.ui.PseudoComponent;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 05-Feb-17
 * <p>
 * Cross-Platform version of {@link consulo.roots.ui.configuration.SdkComboBox}
 */
public class BundleBox implements PseudoComponent {
  /**
   * @return builder with global sdk table
   */
  @Nonnull
  public static BundleBoxBuilder builder(@Nonnull Disposable uiDisposable) {
    return BundleBoxBuilder.create(uiDisposable);
  }

  @Nonnull
  public static BundleBoxBuilder builder(@Nonnull SdkModel sdkModel, @Nonnull Disposable uiDisposable) {
    return BundleBoxBuilder.create(sdkModel, uiDisposable);
  }

  public abstract static class BundleBoxItem {
    @Nullable
    public Sdk getBundle() {
      return null;
    }

    @Nullable
    public String getBundleName() {
      Sdk bundle = getBundle();
      return bundle != null ? bundle.getName() : null;
    }

    public abstract boolean equals(Object o);
  }

  public static class BaseBundleBoxItem extends BundleBoxItem{
     private final Sdk myBundle;

    public BaseBundleBoxItem(Sdk bundle) {
      myBundle = bundle;
    }

    @Nullable
    @Override
    public Sdk getBundle() {
      return myBundle;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BaseBundleBoxItem that = (BaseBundleBoxItem)o;
      return Objects.equals(myBundle, that.myBundle);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myBundle);
    }
  }

  public static class ModuleExtensionBundleBoxItem extends BundleBoxItem {
    private final ModuleExtension<?> myModuleExtension;
    private final MutableModuleInheritableNamedPointer<Sdk> mySdkPointer;

    public ModuleExtensionBundleBoxItem(ModuleExtension<?> moduleExtension, MutableModuleInheritableNamedPointer<Sdk> sdkPointer) {
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ModuleExtensionBundleBoxItem that = (ModuleExtensionBundleBoxItem)o;
      return Objects.equals(myModuleExtension, that.myModuleExtension);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myModuleExtension);
    }
  }

  public static class CustomBundleBoxItem extends BundleBoxItem {
    private final String myKey;
    private final String myPresentableName;
    private final Image myIcon;

    public CustomBundleBoxItem(String key, String presentableName, Image icon) {
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CustomBundleBoxItem that = (CustomBundleBoxItem)o;
      return Objects.equals(myKey, that.myKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myKey);
    }
  }

  public static class InvalidModuleBundleBoxItem extends BundleBoxItem {
    private final String myModuleName;

    public InvalidModuleBundleBoxItem(String moduleName) {
      myModuleName = moduleName;
    }

    @Nonnull
    public String getModuleName() {
      return myModuleName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InvalidModuleBundleBoxItem that = (InvalidModuleBundleBoxItem)o;
      return Objects.equals(myModuleName, that.myModuleName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myModuleName);
    }
  }

  public static class NullBundleBoxItem extends BundleBoxItem {
    public NullBundleBoxItem() {
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof NullBundleBoxItem;
    }
  }

  private static class InvalidBundleBoxItem extends BundleBoxItem {
    private final String mySdkName;

    public InvalidBundleBoxItem(String name) {
      mySdkName = name;
    }

    @Override
    public String getBundleName() {
      return mySdkName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InvalidBundleBoxItem that = (InvalidBundleBoxItem)o;
      return Objects.equals(mySdkName, that.mySdkName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mySdkName);
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
        render.withIcon(AllIcons.Actions.Help);
        render.append(value.getBundleName(), TextAttribute.ERROR);
      }
      else if (value instanceof CustomBundleBoxItem) {
        render.withIcon(((CustomBundleBoxItem)value).getIcon());
        render.append(((CustomBundleBoxItem)value).getPresentableName());
      }
      else if (value instanceof ModuleExtensionBundleBoxItem) {
        ModuleExtensionBundleBoxItem extensionSdkComboBoxItem = (ModuleExtensionBundleBoxItem)value;
        render.withIcon(AllIcons.Nodes.Module);
        render.append(extensionSdkComboBoxItem.getModule().getName(), TextAttribute.REGULAR_BOLD);

        final String sdkName = extensionSdkComboBoxItem.getBundleName();
        if (sdkName != null) {
          render.append(" (" + extensionSdkComboBoxItem.getBundleName() + ")", TextAttribute.GRAYED);
        }
      }
      else if (value instanceof InvalidModuleBundleBoxItem) {
        render.withIcon(AllIcons.Nodes.Module);
        render.append(((InvalidModuleBundleBoxItem)value).getModuleName(), TextAttribute.ERROR_BOLD);
      }
      else if (value == null || value instanceof NullBundleBoxItem) {
        render.withIcon(ObjectUtil.notNull(nullIcon, AllIcons.Ide.EmptyFatalError));
        String name = ObjectUtil.notNull(nullItemName, ProjectBundle.message("sdk.combo.box.item"));
        render.append(name, TextAttribute.REGULAR);
      }
      else {
        Sdk sdk = value.getBundle();
        String sdkName = value.getBundleName();
        assert sdkName != null;
        render.withIcon(sdk == null ? AllIcons.Actions.Help : SdkUtil.getIcon(sdk));
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
  static List<BundleBoxItem> buildItems(@Nonnull BundleHolder holder, @Nullable Predicate<SdkTypeId> filter, boolean withNullItem) {
    List<BundleBoxItem> list = new ArrayList<>();

    List<Sdk> targetSdks = new ArrayList<>();
    holder.forEachBundle(sdk -> {
      if (filter == null || filter.test(sdk.getSdkType())) {
        targetSdks.add(sdk);
      }
    });

    targetSdks.sort((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));

    if (withNullItem) {
      list.add(new NullBundleBoxItem());
    }

    for (Sdk sdk : targetSdks) {
      list.add(new BaseBundleBoxItem(sdk));
    }
    return list;
  }

  @Nonnull
  @RequiredUIAccess
  private static ListModel<BundleBoxItem> model(@Nonnull BundleHolder holder, @Nullable Predicate<SdkTypeId> filter, String nullItemName) {
    return MutableListModel.create(buildItems(holder, filter, nullItemName != null));
  }

  @RequiredUIAccess
  public void addInvalidModuleItem(@Nullable String name) {
    MutableListModel<BundleBoxItem> listModel = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    listModel.add(new InvalidModuleBundleBoxItem(name));
  }

  @RequiredUIAccess
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

  @RequiredUIAccess
  public void addBundleItem(@Nonnull Sdk bundle) {
    MutableListModel<BundleBoxItem> model = (MutableListModel<BundleBoxItem>)myOriginalComboBox.getListModel();

    model.add(new BaseBundleBoxItem(bundle));
  }

  @UsedInPlugin
  @RequiredUIAccess
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
