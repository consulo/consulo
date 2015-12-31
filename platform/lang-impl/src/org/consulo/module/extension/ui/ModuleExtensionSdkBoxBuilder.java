/*
 * Copyright 2013-2015 must-be.org
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
package org.consulo.module.extension.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import org.consulo.module.extension.MutableModuleExtension;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 15.03.2015
 */
public class ModuleExtensionSdkBoxBuilder<T extends MutableModuleExtension<?>> {
  @NotNull
  public static <T extends MutableModuleExtensionWithSdk<?>> ModuleExtensionSdkBoxBuilder createAndDefine(@NotNull T extension,
                                                                                                          @Nullable Runnable updater) {
    ModuleExtensionSdkBoxBuilder<T> builder = create(extension, updater);
    builder.sdkTypeClass(extension.getSdkTypeClass());
    builder.sdkPointerFunc(new NullableFunction<T, MutableModuleInheritableNamedPointer<Sdk>>() {
      @Override
      public MutableModuleInheritableNamedPointer<Sdk> fun(T dom) {
        return dom.getInheritableSdk();
      }
    });
    return builder;
  }

  @NotNull
  public static <T extends MutableModuleExtension<?>> ModuleExtensionSdkBoxBuilder<T> create(@NotNull T extension, @Nullable Runnable updater) {
    return new ModuleExtensionSdkBoxBuilder<T>(extension).laterUpdater(updater);
  }

  @NotNull
  private NullableFunction<T, MutableModuleInheritableNamedPointer<Sdk>> mySdkPointerFunction;
  @NotNull
  private Condition<SdkTypeId> mySdkFilter = Conditions.alwaysTrue();

  private final T myMutableModuleExtension;

  private String myLabelText = "SDK";

  private Icon myNullItemIcon = null;

  private String myNullItemName = ProjectBundle.message("sdk.combo.box.item");

  private Runnable myLaterUpdater;

  private PairConsumer<Sdk, Sdk> myPostConsumer;

  private ModuleExtensionSdkBoxBuilder(@NotNull T mutableModuleExtension) {
    myMutableModuleExtension = mutableModuleExtension;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> sdkTypeClass(@NotNull final Class<? extends SdkType> clazz) {
    mySdkFilter = new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return clazz.isAssignableFrom(sdkTypeId.getClass());
      }
    };
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> sdkTypes(@NotNull final Set<SdkType> sdkTypes) {
    mySdkFilter = new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return sdkTypes.contains(sdkTypeId);
      }
    };
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> sdkType(@NotNull final SdkType sdkType) {
    return sdkTypes(Collections.singleton(sdkType));
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> sdkPointerFunc(@NotNull NullableFunction<T, MutableModuleInheritableNamedPointer<Sdk>> function) {
    mySdkPointerFunction = function;
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> labelText(@NotNull String labelText) {
    myLabelText = labelText;
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> laterUpdater(@Nullable Runnable runnable) {
    myLaterUpdater = runnable;
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> postConsumer(@NotNull PairConsumer<Sdk, Sdk> consumer) {
    myPostConsumer = consumer;
    return this;
  }

  @NotNull
  public ModuleExtensionSdkBoxBuilder<T> nullItem(@Nullable String name, @Nullable Icon icon) {
    myNullItemName = name;
    myNullItemIcon = icon;
    return this;
  }

  @NotNull
  @RequiredReadAction
  public JComponent build() {
    final ProjectSdksModel projectSdksModel = ProjectStructureConfigurable.getInstance(myMutableModuleExtension.getProject()).getProjectSdksModel();

    final SdkComboBox comboBox = new SdkComboBox(projectSdksModel, mySdkFilter, null, myNullItemName, myNullItemIcon);

    comboBox.insertModuleItems(myMutableModuleExtension, mySdkPointerFunction);

    final MutableModuleInheritableNamedPointer<Sdk> inheritableSdk = mySdkPointerFunction.fun(myMutableModuleExtension);
    assert inheritableSdk != null;
    if (inheritableSdk.isNull()) {
      comboBox.setSelectedNoneSdk();
    }
    else {
      final String sdkInheritModuleName = inheritableSdk.getModuleName();
      if (sdkInheritModuleName != null) {
        final Module sdkInheritModule = inheritableSdk.getModule();
        if (sdkInheritModule == null) {
          comboBox.addInvalidModuleItem(sdkInheritModuleName);
        }
        comboBox.setSelectedModule(sdkInheritModuleName);
      }
      else {
        comboBox.setSelectedSdk(inheritableSdk.getName());
      }
    }

    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          Sdk oldValue = inheritableSdk.get();

          inheritableSdk.set(comboBox.getSelectedModuleName(), comboBox.getSelectedSdkName());

          if (myPostConsumer != null) {
            Sdk sdk = inheritableSdk.get();
            myPostConsumer.consume(oldValue, sdk);
          }

          if (myLaterUpdater != null) {
            SwingUtilities.invokeLater(myLaterUpdater);
          }
        }
      }
    });

    return LabeledComponent.left(comboBox, myLabelText);
  }
}
