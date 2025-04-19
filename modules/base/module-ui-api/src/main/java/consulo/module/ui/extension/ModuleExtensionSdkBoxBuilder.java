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
package consulo.module.ui.extension;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.content.bundle.*;
import consulo.module.Module;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.module.ui.awt.SdkComboBox;
import consulo.project.ProjectBundle;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.image.Image;
import consulo.util.lang.function.Predicates;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2015-03-15
 */
public class ModuleExtensionSdkBoxBuilder<T extends MutableModuleExtension<?>> {
    @Nonnull
    public static <T extends MutableModuleExtensionWithSdk<?>> ModuleExtensionSdkBoxBuilder createAndDefine(
        @Nonnull T extension,
        @Nullable Runnable updater
    ) {
        ModuleExtensionSdkBoxBuilder<T> builder = create(extension, updater);
        builder.sdkTypeClass(extension.getSdkTypeClass());
        builder.sdkPointerFunc(dom -> dom.getInheritableSdk());
        return builder;
    }

    @Nonnull
    public static <T extends MutableModuleExtension<?>> ModuleExtensionSdkBoxBuilder<T> create(
        @Nonnull T extension,
        @Nullable Runnable updater
    ) {
        return new ModuleExtensionSdkBoxBuilder<>(extension).laterUpdater(updater);
    }

    @Nonnull
    private Function<T, MutableModuleInheritableNamedPointer<Sdk>> mySdkPointerFunction;
    @Nonnull
    private Predicate<SdkTypeId> mySdkFilter = Predicates.alwaysTrue();

    private final T myMutableModuleExtension;

    private String myLabelText = "SDK";

    private Image myNullItemIcon = null;

    private String myNullItemName = ProjectBundle.message("sdk.combo.box.item");

    private Runnable myLaterUpdater;

    private BiConsumer<Sdk, Sdk> myPostConsumer;

    private ModuleExtensionSdkBoxBuilder(@Nonnull T mutableModuleExtension) {
        myMutableModuleExtension = mutableModuleExtension;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkTypeClass(@Nonnull Class<? extends SdkTypeId> clazz) {
        mySdkFilter = sdkTypeId -> clazz.isAssignableFrom(sdkTypeId.getClass());
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkTypes(@Nonnull Set<SdkType> sdkTypes) {
        mySdkFilter = sdkTypes::contains;
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkType(@Nonnull SdkType sdkType) {
        return sdkTypes(Collections.singleton(sdkType));
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkPointerFunc(@Nonnull Function<T, MutableModuleInheritableNamedPointer<Sdk>> function) {
        mySdkPointerFunction = function;
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> labelText(@Nonnull String labelText) {
        myLabelText = labelText;
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> laterUpdater(@Nullable Runnable runnable) {
        myLaterUpdater = runnable;
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> postConsumer(@Nonnull BiConsumer<Sdk, Sdk> consumer) {
        myPostConsumer = consumer;
        return this;
    }

    @Nonnull
    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> nullItem(@Nullable String name, @Nullable Image icon) {
        myNullItemName = name;
        myNullItemIcon = icon;
        return this;
    }

    @Nonnull
    @RequiredReadAction
    public JComponent build() {
        SdkModel projectSdksModel = SdkModelFactory.getInstance().getOrCreateModel();

        SdkComboBox comboBox = new SdkComboBox(projectSdksModel, mySdkFilter, null, myNullItemName, myNullItemIcon);

        comboBox.insertModuleItems(myMutableModuleExtension, mySdkPointerFunction);

        MutableModuleInheritableNamedPointer<Sdk> inheritableSdk = mySdkPointerFunction.apply(myMutableModuleExtension);
        assert inheritableSdk != null;
        if (inheritableSdk.isNull()) {
            comboBox.setSelectedNoneSdk();
        }
        else {
            String sdkInheritModuleName = inheritableSdk.getModuleName();
            if (sdkInheritModuleName != null) {
                Module sdkInheritModule = inheritableSdk.getModule();
                if (sdkInheritModule == null) {
                    comboBox.addInvalidModuleItem(sdkInheritModuleName);
                }
                comboBox.setSelectedModule(sdkInheritModuleName);
            }
            else {
                comboBox.setSelectedSdk(inheritableSdk.getName());
            }
        }

        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Sdk oldValue = inheritableSdk.get();

                inheritableSdk.set(comboBox.getSelectedModuleName(), comboBox.getSelectedSdkName());

                if (myPostConsumer != null) {
                    Sdk sdk = inheritableSdk.get();
                    myPostConsumer.accept(oldValue, sdk);
                }

                if (myLaterUpdater != null) {
                    SwingUtilities.invokeLater(myLaterUpdater);
                }
            }
        });

        return LabeledComponent.left(comboBox, myLabelText);
    }
}
