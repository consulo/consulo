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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.configurable.ConfigurationException;
import consulo.content.bundle.*;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author anna
 * @since 2006-06-05
 */
public class DefaultSdksModel implements SdkModel, SettingsSdksModel {
    private static final Logger LOG = Logger.getInstance(DefaultSdksModel.class);

    private final Map<Sdk, Sdk> mySdks = new HashMap<>();

    private final EventDispatcher<Listener> mySdkEventsDispatcher = EventDispatcher.create(Listener.class);
    private final Provider<SdkTable> mySdkTableProvider;

    private boolean myModified = false;

    private boolean myInitialized = false;

    public DefaultSdksModel() {
        this(SdkTable::getInstance);
    }

    public DefaultSdksModel(@Nonnull Provider<SdkTable> sdkTableProvider) {
        mySdkTableProvider = sdkTableProvider;
    }

    public void initializeIfNeed() {
        if (!myInitialized) {
            reset();
        }
    }

    @Override
    public Listener getMulticaster() {
        return mySdkEventsDispatcher.getMulticaster();
    }

    @Override
    public Sdk[] getSdks() {
        return ContainerUtil.toArray(mySdks.values(), Sdk.ARRAY_FACTORY);
    }

    @Override
    public void forEachBundle(@Nonnull Consumer<Sdk> sdkConsumer) {
        for (Sdk sdk : mySdks.values()) {
            sdkConsumer.accept(sdk);
        }
    }

    @Override
    @Nullable
    public Sdk findSdk(String sdkName) {
        for (Sdk sdk : mySdks.values()) {
            if (Comparing.strEqual(sdk.getName(), sdkName)) {
                return sdk;
            }
        }
        return null;
    }

    @Override
    public void addListener(Listener listener) {
        mySdkEventsDispatcher.addListener(listener);
    }

    @Override
    public void addListener(Listener listener, Disposable disposable) {
        mySdkEventsDispatcher.addListener(listener, disposable);
    }

    @Override
    public void removeListener(Listener listener) {
        mySdkEventsDispatcher.removeListener(listener);
    }

    @Override
    public void reset() {
        mySdks.clear();
        Sdk[] sdks = mySdkTableProvider.get().getAllSdks();
        for (Sdk sdk : sdks) {
            try {
                mySdks.put(sdk, (Sdk)sdk.clone());
            }
            catch (CloneNotSupportedException e) {
                LOG.error(e);
            }
        }
        myModified = false;
        myInitialized = true;
    }

    @Override
    public void dispose() {
        mySdks.clear();
        myInitialized = false;
    }

    @Override
    public Map<Sdk, Sdk> getModifiedSdksMap() {
        return mySdks;
    }

    @Override
    public boolean isModified() {
        return myModified;
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        apply(null);
    }

    @Override
    @RequiredUIAccess
    public void apply(@Nullable MasterDetailsComponent configurable) throws ConfigurationException {
        apply(configurable, false);
    }

    @RequiredUIAccess
    public void apply(@Nullable MasterDetailsComponent configurable, boolean addedOnly) throws ConfigurationException {
        String[] errorString = new String[1];
        if (!canApply(errorString, configurable, addedOnly)) {
            throw new ConfigurationException(errorString[0]);
        }
        Sdk[] allFromTable = mySdkTableProvider.get().getAllSdks();
        ArrayList<Sdk> itemsInTable = new ArrayList<>();
        // Delete removed and fill itemsInTable
        Application app = Application.get();
        app.runWriteAction(() -> {
            SdkTable sdkTable = mySdkTableProvider.get();
            for (Sdk tableItem : allFromTable) {
                if (mySdks.containsKey(tableItem)) {
                    itemsInTable.add(tableItem);
                }
                else {
                    sdkTable.removeSdk(tableItem);
                }
            }
        });
        app.runWriteAction(() -> {
            // Now all removed items are deleted from table, itemsInTable contains all items in table
            SdkTable sdkTable = mySdkTableProvider.get();
            for (Sdk originalSdk : itemsInTable) {
                Sdk modifiedSdk = mySdks.get(originalSdk);
                LOG.assertTrue(modifiedSdk != null);
                sdkTable.updateSdk(originalSdk, modifiedSdk);
            }
            // Add new items to table
            Sdk[] allSdks = sdkTable.getAllSdks();
            for (Sdk sdk : mySdks.keySet()) {
                LOG.assertTrue(sdk != null);
                if (ArrayUtil.find(allSdks, sdk) == -1) {
                    sdkTable.addSdk(sdk);
                }
            }
        });
        myModified = false;
    }

    private boolean canApply(
        String[] errorString,
        @Nullable MasterDetailsComponent rootConfigurable,
        boolean addedOnly
    ) throws ConfigurationException {

        LinkedHashMap<Sdk, Sdk> sdks = new LinkedHashMap<>(mySdks);
        if (addedOnly) {
            Sdk[] allSdks = mySdkTableProvider.get().getAllSdks();
            for (Sdk sdk : allSdks) {
                sdks.remove(sdk);
            }
        }
        ArrayList<String> allNames = new ArrayList<>();
        Sdk itemWithError = null;
        for (Sdk currItem : sdks.values()) {
            String currName = currItem.getName();
            if (currName.isEmpty()) {
                itemWithError = currItem;
                errorString[0] = ProjectLocalize.sdkListNameRequiredError().get();
                break;
            }
            if (allNames.contains(currName)) {
                itemWithError = currItem;
                errorString[0] = ProjectLocalize.sdkListUniqueNameRequiredError().get();
                break;
            }
            if (currItem.getSdkAdditionalData() instanceof ValidatableSdkAdditionalData sdkAdditionalData) {
                try {
                    sdkAdditionalData.checkValid(this);
                }
                catch (ConfigurationException e) {
                    if (rootConfigurable != null) {
                        if (!(rootConfigurable.getSelectedObject() instanceof Sdk projectJdk
                            && Comparing.strEqual(projectJdk.getName(), currName))) {
                            //do not leave current item with current name
                            rootConfigurable.selectNodeInTree(currName);
                        }
                    }
                    throw new ConfigurationException(ProjectLocalize.sdkConfigurationException(currName) + " " + e.getMessage());
                }
            }
            allNames.add(currName);
        }
        if (itemWithError == null) {
            return true;
        }
        if (rootConfigurable != null) {
            rootConfigurable.selectNodeInTree(itemWithError.getName());
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void removeSdk(Sdk editableObject) {
        Sdk removedSdk = null;
        for (Sdk sdk : mySdks.keySet()) {
            if (mySdks.get(sdk) == editableObject) {
                removedSdk = sdk;
                break;
            }
        }
        if (removedSdk != null) {
            mySdks.remove(removedSdk);
            mySdkEventsDispatcher.getMulticaster().sdkRemove(removedSdk);
            myModified = true;
        }
    }

    @Override
    public void createAddActions(
        DefaultActionGroup group,
        JComponent parent,
        Consumer<Sdk> updateTree,
        @Nullable Predicate<SdkTypeId> filter
    ) {
        List<SdkType> list = new ArrayList<>();
        Application.get().getExtensionPoint(SdkType.class)
            .collectExtensionsSafe(list, sdkType -> filter == null || filter.test(sdkType) ? sdkType : null);
        Collections.sort(list, (o1, o2) -> StringUtil.compare(o1.getPresentableName(), o2.getPresentableName(), true));

        for (SdkType type : list) {
            AnAction addAction = new DumbAwareAction(type.getPresentableName(), null, type.getIcon()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    doAdd(parent, type, updateTree);
                }
            };
            group.add(addAction);
        }
    }

    @RequiredUIAccess
    public void doAdd(JComponent parent, SdkType type, Consumer<Sdk> callback) {
        myModified = true;
        if (type instanceof SdkTypeWithCustomCreateUI customCreateUI) {
            customCreateUI.showCustomCreateUI(this, parent, sdk -> setupSdk(sdk, callback));
        }
        else if (type instanceof BundleType bundleType) {
            Platform platform = Platform.current();
            SdkUtil.selectSdkHome(platform, bundleType, homePath -> {
                String newSdkName = SdkUtil.createUniqueSdkName(platform, bundleType, homePath, getSdks());
                SdkImpl newSdk = new SdkImpl(mySdkTableProvider.get(), type, homePath, newSdkName);
                setupSdk(newSdk, callback);
            });
        }
        else {
            SdkUtil.selectSdkHome(
                type,
                home -> {
                    String newSdkName = SdkUtil.createUniqueSdkName(type, home, getSdks());
                    SdkImpl newSdk = new SdkImpl(mySdkTableProvider.get(), newSdkName, type);
                    newSdk.setHomePath(home);
                    setupSdk(newSdk, callback);
                }
            );
        }
    }

    @RequiredUIAccess
    private void setupSdk(Sdk newSdk, Consumer<Sdk> callback) {
        UIAccess uiAccess = UIAccess.current();

        new Task.Modal(null, "Setuping SDK...", false) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                SdkType sdkType = (SdkType)newSdk.getSdkType();
                sdkType.setupSdkPaths(newSdk);

                uiAccess.give(() -> {
                    if (newSdk.getVersionString() == null) {
                        String home = newSdk.getHomePath();
                        Messages.showMessageDialog(
                            ProjectLocalize.sdkJavaCorruptError(home).get(),
                            ProjectLocalize.sdkJavaCorruptTitle().get(),
                            UIUtil.getErrorIcon()
                        );
                    }

                    doAdd(newSdk, callback);
                });
            }
        }.queue();
    }

    @Override
    @RequiredUIAccess
    public void addSdk(Sdk sdk) {
        doAdd(sdk, null);
    }

    @Override
    @RequiredUIAccess
    public void doAdd(Sdk newSdk, @Nullable Consumer<Sdk> updateTree) {
        myModified = true;
        mySdks.put(newSdk, newSdk);
        if (updateTree != null) {
            updateTree.accept(newSdk);
        }
        mySdkEventsDispatcher.getMulticaster().sdkAdded(newSdk);
    }

    @Nullable
    public Sdk findSdk(@Nullable Sdk modelJdk) {
        for (Sdk sdk : mySdks.keySet()) {
            if (Comparing.equal(mySdks.get(sdk), modelJdk)) {
                return sdk;
            }
        }
        return null;
    }

    public boolean isInitialized() {
        return myInitialized;
    }
}
