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
package consulo.module.ui.awt;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.content.bundle.*;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBoxWithWidePopup;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.function.Predicates;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

;

/**
 * @author Eugene Zhuravlev
 * @author VISTALL
 * @since 2005-05-18
 */
public class SdkComboBox extends ComboBoxWithWidePopup {
    @Nullable
    private final Predicate<SdkTypeId> myFilter;
    @Nullable
    private final Predicate<SdkTypeId> myCreationFilter;

    public SdkComboBox(@Nonnull BundleHolder sdksModel) {
        this(sdksModel, null, false);
    }

    public SdkComboBox(
        @Nonnull BundleHolder sdksModel,
        @Nullable Predicate<SdkTypeId> filter,
        boolean withNoneItem
    ) {
        this(sdksModel, filter, filter, withNoneItem);
    }

    public SdkComboBox(
        @Nonnull BundleHolder sdksModel,
        @Nullable Predicate<SdkTypeId> filter,
        @Nullable String nullItemName
    ) {
        this(sdksModel, filter, filter, nullItemName, null);
    }

    public SdkComboBox(
        @Nonnull BundleHolder sdksModel,
        @Nullable Predicate<SdkTypeId> filter,
        @Nullable Predicate<SdkTypeId> creationFilter,
        boolean withNoneItem
    ) {
        this(sdksModel, filter, creationFilter, withNoneItem ? ProjectLocalize.sdkComboBoxItem().get() : null, null);
    }

    public SdkComboBox(
        @Nonnull BundleHolder sdksModel,
        @Nullable Predicate<SdkTypeId> filter,
        @Nullable Predicate<SdkTypeId> creationFilter,
        @Nullable String nullItemName
    ) {
        this(sdksModel, filter, creationFilter, nullItemName, null);
    }

    public SdkComboBox(
        @Nonnull BundleHolder sdksModel,
        @Nullable Predicate<SdkTypeId> filter,
        @Nullable Predicate<SdkTypeId> creationFilter,
        @Nullable String nullItemName,
        @Nullable Image nullIcon
    ) {
        super(new SdkComboBoxModel(sdksModel, getSdkFilter(filter), nullItemName));
        myFilter = filter;
        myCreationFilter = creationFilter;
        setRenderer(new ColoredListCellRenderer<SdkComboBoxItem>() {
            @Override
            public void customizeCellRenderer(@Nonnull JList list, SdkComboBoxItem value, int index, boolean selected, boolean hasFocus) {
                setIcon(Image.empty(16));    // to fix vertical size
                if (value instanceof InvalidSdkComboBoxItem) {
                    setIcon(PlatformIconGroup.actionsHelp());
                    append(value.getSdkName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
                }
                else if (value instanceof CustomSdkComboBoxItem customSdkComboBoxItem) {
                    setIcon(customSdkComboBoxItem.getIcon());
                    append(customSdkComboBoxItem.getPresentableName());
                }
                else if (value instanceof ModuleExtensionSdkComboBoxItem extensionSdkComboBoxItem) {
                    setIcon(PlatformIconGroup.nodesModule());
                    append(extensionSdkComboBoxItem.getModule().getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

                    String sdkName = extensionSdkComboBoxItem.getSdkName();
                    if (sdkName != null) {
                        append(" (" + extensionSdkComboBoxItem.getSdkName() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
                else if (value instanceof InvalidModuleComboBoxItem invalidModuleComboBoxItem) {
                    setIcon(PlatformIconGroup.nodesModule());
                    append(invalidModuleComboBoxItem.getModuleName(), SimpleTextAttributes.ERROR_BOLD_ATTRIBUTES);
                }
                else if (value == null || value instanceof NullSdkComboBoxItem) {
                    setIcon(ObjectUtil.notNull(nullIcon, Image.empty(Image.DEFAULT_ICON_SIZE)));
                    String name = ObjectUtil.notNull(nullItemName, ProjectLocalize.sdkComboBoxItem().get());
                    append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
                else {
                    Sdk sdk = value.getSdk();
                    String sdkName = value.getSdkName();
                    assert sdkName != null;
                    setIcon(sdk == null ? PlatformIconGroup.actionsHelp() : SdkUtil.getIcon(sdk));
                    append(sdkName, sdk == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    String version = sdk == null ? null : sdk.getVersionString();
                    if (version != null) {
                        append(" (", SimpleTextAttributes.GRAY_ATTRIBUTES);
                        append(version, SimpleTextAttributes.GRAY_ATTRIBUTES);
                        append(")", SimpleTextAttributes.GRAY_ATTRIBUTES);
                    }
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Rectangle rec = ScreenUtil.getScreenRectangle(0, 0);
        Dimension size = super.getPreferredSize();
        int maxWidth = rec.width / 4;
        if (size.width > maxWidth) {
            size.width = maxWidth;
        }
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minSize = super.getMinimumSize();
        Dimension prefSize = getPreferredSize();
        if (minSize.width > prefSize.width) {
            minSize.width = prefSize.width;
        }
        return minSize;
    }

    //@Deprecated
    //@DeprecationInfo(value = "Use #setSetupButton() without 'moduleJdkSetup' parameter")
    //public void setSetupButton(JButton setUpButton,
    //                           @Nullable Project project,
    //                           SdkModel sdksModel,
    //                           SdkComboBoxItem firstItem,
    //                           @Nullable Condition<Sdk> additionalSetup,
    //                           boolean moduleJdkSetup) {
    //  setSetupButton(setUpButton, project, sdksModel, firstItem, additionalSetup);
    //}

    //@Deprecated
    //@DeprecationInfo(value = "Use #setSetupButton() without 'actionGroupTitle' parameter")
    //public void setSetupButton(JButton setUpButton,
    //                           @Nullable Project project,
    //                           SdkModel sdksModel,
    //                           SdkComboBoxItem firstItem,
    //                           @Nullable Condition<Sdk> additionalSetup,
    //                           String actionGroupTitle) {
    //  setSetupButton(setUpButton, project, sdksModel, firstItem, additionalSetup);
    //}
    //
    //public void setSetupButton(@Nonnull JButton setUpButton,
    //                           @Nullable Project project,
    //                           @Nonnull SdkModel sdksModel,
    //                           @Nullable SdkComboBoxItem firstItem,
    //                           @Nullable Condition<Sdk> additionalSetup) {
    //  setUpButton.addActionListener(new ActionListener() {
    //    @Override
    //    @RequiredUIAccess
    //    public void actionPerformed(ActionEvent e) {
    //      DefaultActionGroup group = new DefaultActionGroup();
    //      ((DefaultSdksModel)sdksModel).createAddActions(group, SdkComboBox.this, new Consumer<Sdk>() {
    //        @Override
    //        public void consume(Sdk sdk) {
    //          //if (project != null) {
    //          //  SdkListConfigurable configurable = SdkListConfigurable.getInstance(project);
    //          //  configurable.addSdkNode(sdk, false);
    //          //}
    //          reloadModel(new SdkComboBoxItem(sdk), project);
    //          setSelectedSdk(sdk); //restore selection
    //          if (additionalSetup != null) {
    //            if (additionalSetup.value(sdk)) { //leave old selection
    //              setSelectedSdk(firstItem.getSdk());
    //            }
    //          }
    //        }
    //      }, myCreationFilter);
    //      DataContext dataContext = DataManager.getInstance().getDataContext(SdkComboBox.this);
    //      if (group.getChildrenCount() > 1) {
    //        JBPopupFactory.getInstance().createActionGroupPopup(ProjectBundle.message("set.up.jdk.title"), group, dataContext,
    //                                                            JBPopupFactory.ActionSelectionAid.MNEMONICS, false)
    //                .showUnderneathOf(setUpButton);
    //      }
    //      else {
    //        AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, new Presentation(""),
    //                                                      ActionManager.getInstance(), 0);
    //        group.getChildren(event)[0].actionPerformed(event);
    //      }
    //    }
    //  });
    //}
    //
    //public void setEditButton(JButton editButton, Project project, @Nonnull Computable<Sdk> retrieveSdk) {
    //  editButton.addActionListener(e -> {
    //    Sdk sdk = retrieveSdk.compute();
    //    if (sdk != null) {
    //      ShowSettingsUtil.getInstance().showProjectStructureDialog(project, projectStructureSelector -> projectStructureSelector.select(sdk, true));
    //    }
    //  });
    //  addActionListener(e -> {
    //    SdkComboBoxItem selectedItem = getSelectedItem();
    //    editButton.setEnabled(!(selectedItem instanceof InvalidSdkComboBoxItem) &&
    //                          selectedItem != null &&
    //                          selectedItem.getSdk() != null);
    //  });
    //}

    @Override
    public SdkComboBoxItem getSelectedItem() {
        return (SdkComboBoxItem)super.getSelectedItem();
    }

    @UsedInPlugin
    public void insertCustomSdkItem(@Nonnull String key, @Nonnull String presentableName, @Nonnull Image icon) {
        CustomSdkComboBoxItem sdkComboBoxItem = new CustomSdkComboBoxItem(key, presentableName, icon);
        int itemCount = getItemCount();
        if (itemCount > 0) {
            int index = 0;
            for (int i = 0; i < itemCount; i++) {
                Object itemAt = getItemAt(i);

                if (itemAt instanceof NullSdkComboBoxItem || itemAt instanceof CustomSdkComboBoxItem) {
                    index++;
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
    public <T extends MutableModuleExtension<?>> void insertModuleItems(
        @Nonnull T moduleExtension,
        @Nonnull Function<T, MutableModuleInheritableNamedPointer<Sdk>> sdkPointerFunction
    ) {

        for (Module module : ModuleManager.getInstance(moduleExtension.getModule().getProject()).getModules()) {
            // dont add self module
            if (module == moduleExtension.getModule()) {
                continue;
            }

            ModuleExtension extension = module.getExtension(moduleExtension.getId());
            if (extension == null) {
                continue;
            }
            MutableModuleInheritableNamedPointer<Sdk> sdkPointer = sdkPointerFunction.apply((T)extension);
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
        SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
        return selectedItem != null ? selectedItem.getSdk() : null;
    }

    @Nullable
    public String getSelectedSdkName() {
        SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
        if (selectedItem != null) {
            return selectedItem.getSdkName();
        }
        return null;
    }

    @Nullable
    public String getSelectedModuleName() {
        SdkComboBoxItem selectedItem = (SdkComboBoxItem)super.getSelectedItem();
        if (selectedItem instanceof ModuleExtensionSdkComboBoxItem moduleExtensionSdkComboBoxItem) {
            return moduleExtensionSdkComboBoxItem.getModule().getName();
        }
        else if (selectedItem instanceof InvalidModuleComboBoxItem invalidModuleComboBoxItem) {
            return invalidModuleComboBoxItem.getModuleName();
        }
        return null;
    }

    public void setSelectedSdk(Sdk sdk) {
        int index = indexOf(sdk);
        if (index >= 0) {
            setSelectedIndex(index);
        }
    }

    public void setSelectedModule(@Nonnull String name) {
        int index = indexOfModuleItems(name);
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
            SdkComboBoxModel model = (SdkComboBoxModel)getModel();

            int itemCount = getItemCount();
            for (int i = 0; i < itemCount; i++) {
                SdkComboBoxItem itemAt = model.getElementAt(i);
                String sdkName = itemAt.getSdkName();
                if (name.equals(sdkName)) {
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
        SdkComboBoxModel model = (SdkComboBoxModel)getModel();
        int count = model.getSize();
        for (int idx = 0; idx < count; idx++) {
            SdkComboBoxItem elementAt = model.getElementAt(idx);
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
        SdkComboBoxModel model = (SdkComboBoxModel)getModel();
        int count = model.getSize();
        for (int idx = 0; idx < count; idx++) {
            SdkComboBoxItem elementAt = model.getElementAt(idx);
            if (elementAt instanceof ModuleExtensionSdkComboBoxItem moduleExtensionSdkComboBoxItem) {
                String name = moduleExtensionSdkComboBoxItem.getModule().getName();
                if (name.equals(moduleName)) {
                    return idx;
                }
            }
            else if (elementAt instanceof InvalidModuleComboBoxItem invalidModuleComboBoxItem) {
                if (invalidModuleComboBoxItem.getModuleName().equals(moduleName)) {
                    return idx;
                }
            }
        }
        return -1;
    }

    private void removeInvalidElement() {
        SdkComboBoxModel model = (SdkComboBoxModel)getModel();
        int count = model.getSize();
        for (int idx = 0; idx < count; idx++) {
            SdkComboBoxItem elementAt = model.getElementAt(idx);
            if (elementAt instanceof InvalidSdkComboBoxItem) {
                removeItemAt(idx);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void reloadModel(SdkComboBoxItem firstItem, @Nullable Project project) {
        DefaultComboBoxModel<SdkComboBoxItem> model = ((DefaultComboBoxModel)getModel());
        if (project == null) {
            model.addElement(firstItem);
            return;
        }
        model.removeAllElements();
        model.addElement(firstItem);
        SdkModelFactory util = SdkModelFactory.getInstance();
        SdkModel projectSdksModel = util.getOrCreateModel();
        List<Sdk> sdks = new ArrayList<>(List.of(projectSdksModel.getSdks()));
        if (myFilter != null) {
            sdks = ContainerUtil.filter(sdks, getSdkFilter(myFilter));
        }
        Collections.sort(sdks, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        for (Sdk sdk : sdks) {
            model.addElement(new SdkComboBoxItem(sdk));
        }
    }

    private static class SdkComboBoxModel extends DefaultComboBoxModel<SdkComboBoxItem> {
        public SdkComboBoxModel(
            @Nonnull BundleHolder sdksModel,
            @Nullable Predicate<Sdk> sdkFilter,
            @Nullable String noneItemName
        ) {
            Sdk[] sdks = sdksModel.getBundles();
            if (sdkFilter != null) {
                List<Sdk> filtered = ContainerUtil.filter(sdks, sdkFilter);
                sdks = filtered.toArray(new Sdk[filtered.size()]);
            }
            Arrays.sort(sdks, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));

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
            return super.getElementAt(index);
        }
    }

    private static Predicate<Sdk> getSdkFilter(@Nullable Predicate<SdkTypeId> filter) {
        return filter == null ? Predicates.<Sdk>alwaysTrue() : sdk -> filter.test(sdk.getSdkType());
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

        public ModuleExtensionSdkComboBoxItem(
            ModuleExtension<?> moduleExtension,
            MutableModuleInheritableNamedPointer<Sdk> sdkPointer
        ) {
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

        @Nonnull
        public Module getModule() {
            return myModuleExtension.getModule();
        }
    }

    public static class CustomSdkComboBoxItem extends SdkComboBoxItem {
        private final String myKey;
        private final String myPresentableName;
        private final Image myIcon;

        public CustomSdkComboBoxItem(String key, String presentableName, Image icon) {
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

        @Nonnull
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
