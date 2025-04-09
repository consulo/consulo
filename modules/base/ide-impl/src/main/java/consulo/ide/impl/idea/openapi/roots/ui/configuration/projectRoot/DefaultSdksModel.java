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

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.configurable.ConfigurationException;
import consulo.content.bundle.*;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.ProjectBundle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * User: anna
 * Date: 05-Jun-2006
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
        final Sdk[] sdks = mySdkTableProvider.get().getAllSdks();
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
        final Sdk[] allFromTable = mySdkTableProvider.get().getAllSdks();
        final ArrayList<Sdk> itemsInTable = new ArrayList<>();
        // Delete removed and fill itemsInTable
        ApplicationManager.getApplication().runWriteAction(() -> {
            final SdkTable sdkTable = mySdkTableProvider.get();
            for (final Sdk tableItem : allFromTable) {
                if (mySdks.containsKey(tableItem)) {
                    itemsInTable.add(tableItem);
                }
                else {
                    sdkTable.removeSdk(tableItem);
                }
            }
        });
        ApplicationManager.getApplication().runWriteAction(() -> {
            // Now all removed items are deleted from table, itemsInTable contains all items in table
            final SdkTable sdkTable = mySdkTableProvider.get();
            for (Sdk originalSdk : itemsInTable) {
                final Sdk modifiedSdk = mySdks.get(originalSdk);
                LOG.assertTrue(modifiedSdk != null);
                sdkTable.updateSdk(originalSdk, modifiedSdk);
            }
            // Add new items to table
            final Sdk[] allSdks = sdkTable.getAllSdks();
            for (final Sdk sdk : mySdks.keySet()) {
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
                errorString[0] = ProjectBundle.message("sdk.list.name.required.error");
                break;
            }
            if (allNames.contains(currName)) {
                itemWithError = currItem;
                errorString[0] = ProjectBundle.message("sdk.list.unique.name.required.error");
                break;
            }
            final SdkAdditionalData sdkAdditionalData = currItem.getSdkAdditionalData();
            if (sdkAdditionalData instanceof ValidatableSdkAdditionalData) {
                try {
                    ((ValidatableSdkAdditionalData)sdkAdditionalData).checkValid(this);
                }
                catch (ConfigurationException e) {
                    if (rootConfigurable != null) {
                        final Object projectJdk = rootConfigurable.getSelectedObject();
                        if (!(projectJdk instanceof Sdk) || !Comparing.strEqual(((Sdk)projectJdk).getName(), currName)) {
                            //do not leave current item with current name
                            rootConfigurable.selectNodeInTree(currName);
                        }
                    }
                    throw new ConfigurationException(ProjectBundle.message("sdk.configuration.exception", currName) + " " + e.getMessage());
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
    public void removeSdk(final Sdk editableObject) {
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
        final JComponent parent,
        final Consumer<Sdk> updateTree,
        @Nullable Predicate<SdkTypeId> filter
    ) {
        final List<SdkType> types = SdkType.EP_NAME.getExtensionList();
        List<SdkType> list = new ArrayList<>(types.size());
        for (SdkType sdkType : types) {
            if (filter != null && !filter.test(sdkType)) {
                continue;
            }

            list.add(sdkType);
        }
        Collections.sort(list, (o1, o2) -> StringUtil.compare(o1.getPresentableName(), o2.getPresentableName(), true));

        for (final SdkType type : list) {
            final AnAction addAction = new DumbAwareAction(type.getPresentableName(), null, type.getIcon()) {
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
    public void doAdd(JComponent parent, final SdkType type, final Consumer<Sdk> callback) {
        myModified = true;
        if (type instanceof SdkTypeWithCustomCreateUI customCreateUI) {
            customCreateUI.showCustomCreateUI(this, parent, sdk -> setupSdk(sdk, callback));
        }
        else if (type instanceof BundleType bundleType) {
            Platform platform = Platform.current();
            SdkUtil.selectSdkHome(platform, bundleType, homePath -> {
                String newSdkName = SdkUtil.createUniqueSdkName(platform, bundleType, homePath, getSdks());
                final SdkImpl newSdk = new SdkImpl(mySdkTableProvider.get(), type, homePath, newSdkName);
                setupSdk(newSdk, callback);
            });
        }
        else {
            SdkUtil.selectSdkHome(type, home -> {
                String newSdkName = SdkUtil.createUniqueSdkName(type, home, getSdks());
                final SdkImpl newSdk = new SdkImpl(mySdkTableProvider.get(), newSdkName, type);
                newSdk.setHomePath(home);
                setupSdk(newSdk, callback);
            });
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
                            ProjectBundle.message("sdk.java.corrupt.error", home),
                            ProjectBundle.message("sdk.java.corrupt.title"),
                            Messages.getErrorIcon()
                        );
                    }

                    doAdd(newSdk, callback);
                });
            }
        }.queue();
    }

    @Override
    public void addSdk(Sdk sdk) {
        doAdd(sdk, null);
    }

    @Override
    public void doAdd(Sdk newSdk, @Nullable Consumer<Sdk> updateTree) {
        myModified = true;
        mySdks.put(newSdk, newSdk);
        if (updateTree != null) {
            updateTree.accept(newSdk);
        }
        mySdkEventsDispatcher.getMulticaster().sdkAdded(newSdk);
    }

    @Nullable
    public Sdk findSdk(@Nullable final Sdk modelJdk) {
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
