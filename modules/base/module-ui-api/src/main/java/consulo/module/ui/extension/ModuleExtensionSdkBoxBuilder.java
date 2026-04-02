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
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.module.ui.awt.SdkComboBox;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.image.Image;
import consulo.util.lang.function.Predicates;
import org.jspecify.annotations.Nullable;

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
    public static <T extends MutableModuleExtensionWithSdk<?>>
    ModuleExtensionSdkBoxBuilder createAndDefine(T extension, @Nullable Runnable updater) {
        ModuleExtensionSdkBoxBuilder<T> builder = create(extension, updater);
        builder.sdkTypeClass(extension.getSdkTypeClass());
        builder.sdkPointerFunc(dom -> dom.getInheritableSdk());
        return builder;
    }

    public static <T extends MutableModuleExtension<?>>
    ModuleExtensionSdkBoxBuilder<T> create(T extension, @Nullable Runnable updater) {
        return new ModuleExtensionSdkBoxBuilder<>(extension).laterUpdater(updater);
    }

    private Function<T, MutableModuleInheritableNamedPointer<Sdk>> mySdkPointerFunction;
    
    private Predicate<SdkTypeId> mySdkFilter = Predicates.alwaysTrue();

    private final T myMutableModuleExtension;

    private String myLabelText = "SDK";

    private Image myNullItemIcon = null;

    private LocalizeValue myNullItemName = ProjectLocalize.sdkComboBoxItem();

    private Runnable myLaterUpdater;

    private BiConsumer<Sdk, Sdk> myPostConsumer;

    private ModuleExtensionSdkBoxBuilder(T mutableModuleExtension) {
        myMutableModuleExtension = mutableModuleExtension;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkTypeClass(Class<? extends SdkTypeId> clazz) {
        mySdkFilter = sdkTypeId -> clazz.isAssignableFrom(sdkTypeId.getClass());
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkTypes(Set<SdkType> sdkTypes) {
        mySdkFilter = sdkTypes::contains;
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkType(SdkType sdkType) {
        return sdkTypes(Collections.singleton(sdkType));
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> sdkPointerFunc(Function<T, MutableModuleInheritableNamedPointer<Sdk>> function) {
        mySdkPointerFunction = function;
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> labelText(String labelText) {
        myLabelText = labelText;
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> laterUpdater(@Nullable Runnable runnable) {
        myLaterUpdater = runnable;
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> postConsumer(BiConsumer<Sdk, Sdk> consumer) {
        myPostConsumer = consumer;
        return this;
    }

    @UsedInPlugin
    public ModuleExtensionSdkBoxBuilder<T> nullItem(LocalizeValue name, @Nullable Image icon) {
        myNullItemName = name;
        myNullItemIcon = icon;
        return this;
    }

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
