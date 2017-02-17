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
package consulo.ui.bundle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 05-Feb-17
 * <p>
 * Crossplatform version of {@link consulo.roots.ui.configuration.SdkComboBox}
 */
public class BundleComboBox implements PseudoComponent {
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

    public ModuleExtensionSdkComboBoxItem(ModuleExtension<?> moduleExtension, MutableModuleInheritableNamedPointer<Sdk> sdkPointer) {
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

  private static Condition<Sdk> getSdkFilter(@Nullable final Condition<SdkTypeId> filter) {
    return filter == null ? Conditions.<Sdk>alwaysTrue() : sdk -> filter.value(sdk.getSdkType());
  }

  private final ComboBox<SdkComboBoxItem> myOriginalComboBox;
  @Nullable
  private final Condition<SdkTypeId> myFilter;
  @Nullable
  private final Condition<SdkTypeId> myCreationFilter;

  public BundleComboBox(@NotNull final SdkModel sdksModel) {
    this(sdksModel, null, false);
  }

  public BundleComboBox(@NotNull SdkModel sdksModel, @Nullable Condition<SdkTypeId> filter, boolean withNoneItem) {
    this(sdksModel, filter, filter, withNoneItem);
  }

  public BundleComboBox(@NotNull SdkModel sdksModel, @Nullable Condition<SdkTypeId> filter, @Nullable String nullItemName) {
    this(sdksModel, filter, filter, nullItemName, null);
  }

  public BundleComboBox(@NotNull SdkModel sdksModel,
                        @Nullable Condition<SdkTypeId> filter,
                        @Nullable Condition<SdkTypeId> creationFilter,
                        boolean withNoneItem) {
    this(sdksModel, filter, creationFilter, withNoneItem ? ProjectBundle.message("sdk.combo.box.item") : null, null);
  }

  public BundleComboBox(@NotNull SdkModel sdksModel,
                        @Nullable Condition<SdkTypeId> filter,
                        @Nullable Condition<SdkTypeId> creationFilter,
                        @Nullable String nullItemName) {
    this(sdksModel, filter, creationFilter, nullItemName, null);
  }

  public BundleComboBox(@NotNull SdkModel sdksModel,
                        @Nullable Condition<SdkTypeId> filter,
                        @Nullable Condition<SdkTypeId> creationFilter,
                        @Nullable final String nullItemName,
                        @Nullable final Icon nullIcon) {
    myOriginalComboBox = Components.comboBox();
    myFilter = filter;
    myCreationFilter = creationFilter;

    /*myOriginalComboBox.setRender((render, index, value) -> {
      if (value instanceof InvalidSdkComboBoxItem) {
        setIcon(AllIcons.Toolbar.Unknown);
        append(value.getSdkName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
      }
      else if (value instanceof CustomSdkComboBoxItem) {
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
        if (version != null) {
          append(" (", SimpleTextAttributes.GRAY_ATTRIBUTES);
          append(version, SimpleTextAttributes.GRAY_ATTRIBUTES);
          append(")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
      }
    }); */
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public Component getComponent() {
    return myOriginalComboBox;
  }
}
