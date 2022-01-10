/*
 * Copyright 2013-2016 consulo.io
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
package consulo.extension.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import consulo.annotation.UsedInPlugin;
import consulo.bundle.ui.BundleBox;
import consulo.bundle.ui.BundleBoxBuilder;
import consulo.disposer.Disposable;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.util.LabeledComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 15.03.2015
 */
public class ModuleExtensionBundleBoxBuilder<T extends MutableModuleExtension<?>> {
  @Nonnull
  public static <T extends MutableModuleExtensionWithSdk<?>> ModuleExtensionBundleBoxBuilder createAndDefine(@Nonnull T extension, @Nonnull Disposable uiDisposable, @Nullable Runnable updater) {
    ModuleExtensionBundleBoxBuilder<T> builder = create(extension, uiDisposable, updater);
    builder.sdkTypeClass(extension.getSdkTypeClass());
    builder.sdkPointerFunc(dom -> dom.getInheritableSdk());
    return builder;
  }

  @Nonnull
  public static <T extends MutableModuleExtension<?>> ModuleExtensionBundleBoxBuilder<T> create(@Nonnull T extension, @Nonnull Disposable uiDisposable, @Nullable Runnable updater) {
    return new ModuleExtensionBundleBoxBuilder<>(extension).laterUpdater(updater).uiDisposable(uiDisposable);
  }

  @Nonnull
  private Function<T, MutableModuleInheritableNamedPointer<Sdk>> mySdkPointerFunction;
  @Nonnull
  private Predicate<SdkTypeId> mySdkFilter = sdkTypeId -> true;

  private final T myMutableModuleExtension;

  private String myLabelText = "SDK";

  private Image myNullItemIcon = null;

  private String myNullItemName = ProjectBundle.message("sdk.combo.box.item");

  private Runnable myLaterUpdater;

  private BiConsumer<Sdk, Sdk> myPostConsumer;

  private Disposable myUIDisposable;

  private ModuleExtensionBundleBoxBuilder(@Nonnull T mutableModuleExtension) {
    myMutableModuleExtension = mutableModuleExtension;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> sdkTypeClass(@Nonnull final Class<? extends SdkTypeId> clazz) {
    mySdkFilter = sdkTypeId -> clazz.isAssignableFrom(sdkTypeId.getClass());
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> sdkTypes(@Nonnull final Set<SdkType> sdkTypes) {
    mySdkFilter = sdkTypes::contains;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> sdkType(@Nonnull final SdkType sdkType) {
    return sdkTypes(Collections.singleton(sdkType));
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> sdkPointerFunc(@Nonnull Function<T, MutableModuleInheritableNamedPointer<Sdk>> function) {
    mySdkPointerFunction = function;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> labelText(@Nonnull String labelText) {
    myLabelText = labelText;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> laterUpdater(@Nullable Runnable runnable) {
    myLaterUpdater = runnable;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> postConsumer(@Nonnull BiConsumer<Sdk, Sdk> consumer) {
    myPostConsumer = consumer;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> nullItem(@Nullable String name, @Nullable Image icon) {
    myNullItemName = name;
    myNullItemIcon = icon;
    return this;
  }

  @Nonnull
  @UsedInPlugin
  public ModuleExtensionBundleBoxBuilder<T> uiDisposable(@Nonnull Disposable disposable) {
    myUIDisposable = disposable;
    return this;
  }

  @Nonnull
  @RequiredUIAccess
  public Component build() {
    BundleBoxBuilder builder = BundleBox.builder(myUIDisposable);
    builder.withNoneItem(myNullItemName);
    if (myNullItemIcon != null) {
      builder.withNoneItemImage(myNullItemIcon);
    }

    builder.withSdkTypeFilter(mySdkFilter);

    final BundleBox box = builder.build();

    box.addModuleExtensionItems(myMutableModuleExtension, mySdkPointerFunction);

    final MutableModuleInheritableNamedPointer<Sdk> inheritableSdk = mySdkPointerFunction.apply(myMutableModuleExtension);
    assert inheritableSdk != null;
    if (inheritableSdk.isNull()) {
      box.setSelectedNoneBundle();
    }
    else {
      final String sdkInheritModuleName = inheritableSdk.getModuleName();
      if (sdkInheritModuleName != null) {
        final Module sdkInheritModule = inheritableSdk.getModule();
        if (sdkInheritModule == null) {
          box.addInvalidModuleItem(sdkInheritModuleName);
        }
        box.setSelectedModule(sdkInheritModuleName);
      }
      else {
        box.setSelectedBundle(inheritableSdk.getName());
      }
    }

    box.getComponent().addValueListener(event -> {
      Sdk oldValue = inheritableSdk.get();

      inheritableSdk.set(box.getSelectedModuleName(), box.getSelectedBundleName());

      if (myPostConsumer != null) {
        Sdk sdk = inheritableSdk.get();
        myPostConsumer.accept(oldValue, sdk);
      }

      if (myLaterUpdater != null) {
        UIAccess.current().give(myLaterUpdater);
      }
    });

    return LabeledComponents.leftFilled(myLabelText, box);
  }
}
