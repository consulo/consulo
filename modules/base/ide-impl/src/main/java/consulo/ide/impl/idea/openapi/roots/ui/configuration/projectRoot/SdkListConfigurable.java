/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModel;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkTypeId;
import consulo.content.bundle.SdkUtil;
import consulo.content.bundle.UnknownSdkType;
import consulo.disposer.Disposable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@ExtensionImpl
public class SdkListConfigurable extends UnifiedMasterDetailsComponent
    implements ApplicationConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    public static final String ID = "sdks.list";

    public static final Predicate<SdkTypeId> ADD_SDK_FILTER = sdkTypeId -> sdkTypeId instanceof SdkType && ((SdkType) sdkTypeId).supportsUserAdd();

    private static final UnknownSdkType ourUnknownSdkType = UnknownSdkType.getInstance("UNKNOWN_BUNDLE");

    private final SdkModel.Listener myListener = new SdkModel.Listener() {
        @RequiredUIAccess
        @Override
        public void sdkChanged(Sdk sdk, String previousName) {
            updateSelectedName();
        }

        @RequiredUIAccess
        @Override
        public void sdkHomeSelected(Sdk sdk, String newSdkHome) {
            updateSelectedName();
        }
    };

    private final ShowSettingsUtil myShowSettingsUtil;

    private @Nullable Disposable myListenerDisposable;

    @Inject
    public SdkListConfigurable(ShowSettingsUtil showSettingsUtil) {
        myShowSettingsUtil = showSettingsUtil;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.globalBundlesDisplayName();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return ProjectLocalize.globalBundlesEmptyText();
    }

    @Override
    protected boolean isExpandAllByDefault() {
        return true;
    }

    @Override
    protected boolean isMultipleSelectAllowed() {
        return true;
    }

    @Override
    protected boolean isRemovedWhenEmpty(Object value) {
        return value instanceof SdkType;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof SdkType sdkType) {
            return sdkType.getDisplayName().get();
        }
        if (value instanceof Sdk sdk) {
            return sdk.getName();
        }
        return String.valueOf(value);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof SdkType sdkType) {
            presentation.withIcon(sdkType.getGroupIcon());
            presentation.append(sdkType.getDisplayName(), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof Sdk sdk) {
            presentation.withIcon(SdkUtil.getIcon(sdk));
            presentation.append(sdk.getName());

            String versionString = sdk.getVersionString();
            if (versionString != null) {
                presentation.append(" (" + versionString + ")", TextAttribute.GRAYED);
            }

            String homePath = sdk.getHomePath();
            if (homePath != null) {
                presentation.withTooltip(LocalizeValue.of(homePath));
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        Disposable listenerDisposable = myListenerDisposable;
        if (listenerDisposable != null) {
            listenerDisposable.disposeWithTree();
        }

        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
        listenerDisposable = Disposable.newDisposable();
        sdksModel.addListener(myListener, listenerDisposable);
        myListenerDisposable = listenerDisposable;

        Map<SdkType, List<Sdk>> map = new LinkedHashMap<>();
        for (Sdk sdk : sdksModel.getModifiedSdksMap().values()) {
            map.computeIfAbsent(getGroupType(sdk), type -> new ArrayList<>()).add(sdk);
        }

        for (Map.Entry<SdkType, List<Sdk>> entry : map.entrySet()) {
            SdkType sdkType = entry.getKey();
            addValue(null, sdkType);

            for (Sdk sdk : entry.getValue()) {
                addValue(sdkType, sdk);
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (value instanceof SdkImpl sdk) {
            return new SdkConfigurable(sdk, myShowSettingsUtil.getSdksModel(), () -> refreshValue(sdk));
        }
        return null;
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddSdkAction(), new RemoveSdkAction(), new CopySdkAction());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myShowSettingsUtil.getSdksModel().reset();

        super.reset();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return super.isModified() || myShowSettingsUtil.getSdksModel().isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();

        boolean modifiedSdks = super.isModified();

        super.apply();

        if (sdksModel.isModified() || modifiedSdks) {
            sdksModel.apply(this::select);
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();

        Disposable listenerDisposable = myListenerDisposable;
        if (listenerDisposable != null) {
            listenerDisposable.disposeWithTree();
            myListenerDisposable = null;
        }

        myShowSettingsUtil.getSdksModel().dispose();
    }

    @RequiredUIAccess
    public CompletableFuture<?> selectSdk(Sdk sdk) {
        Sdk modifiedSdk = myShowSettingsUtil.getSdksModel().getModifiedSdksMap().get(sdk);
        Sdk target = modifiedSdk != null ? modifiedSdk : sdk;
        Object value = findValue(candidate -> candidate == target);
        if (value == null) {
            value = findValue(candidate -> candidate instanceof Sdk candidateSdk && candidateSdk.getName().equals(sdk.getName()));
        }
        return select(value);
    }

    private static SdkType getGroupType(Sdk sdk) {
        SdkType sdkType = (SdkType) sdk.getSdkType();
        return sdkType instanceof UnknownSdkType ? ourUnknownSdkType : sdkType;
    }

    @RequiredUIAccess
    private void addSdkNode(Sdk sdk) {
        if (getTree() == null) {
            return;
        }

        SdkType sdkType = getGroupType(sdk);
        if (!isAttached(sdkType)) {
            addValue(null, sdkType);
        }
        addValue(sdkType, sdk);

        refreshTree(sdk);
    }

    @RequiredUIAccess
    private void updateSelectedName() {
        Object selected = getSelectedValue();
        if (selected == null) {
            return;
        }

        MasterDetailsConfigurable<?> configurable = getConfigurable(selected);
        if (configurable instanceof SdkConfigurable && isInitialized(configurable)) {
            configurable.updateName();
        }
        refreshValue(selected);
    }

    private class AddSdkAction extends DumbAwareAction {
        private AddSdkAction() {
            super(ProjectLocalize.addActionName(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Tree<Object> tree = getTree();
            if (tree == null) {
                return;
            }

            DefaultActionGroup group = new DefaultActionGroup();
            myShowSettingsUtil.getSdksModel().createAddActions(group, tree, SdkListConfigurable.this::addSdkNode, ADD_SDK_FILTER);

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ProjectLocalize.addActionName().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class RemoveSdkAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveSdkAction() {
            super(CommonLocalize.buttonDelete(), CommonLocalize.buttonDelete(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = getSelectedValues();
            if (!canRemove(selected)) {
                return;
            }

            SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
            for (Object value : selected) {
                sdksModel.removeSdk((Sdk) value);
            }
            removeSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(canRemove(getSelectedValues()));
        }

        private boolean canRemove(List<Object> selected) {
            if (selected.isEmpty()) {
                return false;
            }

            for (Object value : selected) {
                if (!(value instanceof Sdk sdk) || sdk.isPredefined()) {
                    return false;
                }
            }
            return true;
        }
    }

    private class CopySdkAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CopySdkAction() {
            super(CommonLocalize.buttonCopy(), CommonLocalize.buttonCopy(), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (getSelectedValues().size() != 1
                || !(getSelectedValue() instanceof SdkImpl selected)
                || selected.getSdkType() instanceof UnknownSdkType) {
                return;
            }

            SettingsSdksModel sdksModel = myShowSettingsUtil.getSdksModel();
            String defaultNewName = SdkUtil.createUniqueSdkName(selected.getName(), sdksModel.getSdks());

            Component parent = getTree();
            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(ProjectLocalize.sdkCopyTitle())
                .text(ProjectLocalize.sdkCopyPrompt())
                .value(defaultNewName)
                .validator(name -> {
                    if (name.isBlank()) {
                        return InputProblem.error(ProjectLocalize.sdkListNameRequiredError());
                    }
                    if (sdksModel.findSdk(name) != null) {
                        return InputProblem.error(ProjectLocalize.sdkListUniqueNameRequiredError());
                    }
                    return null;
                })
                .showAsync(parent)
                .whenCompleteAsync((newName, error) -> {
                    if (error != null || newName == null) {
                        return;
                    }
                    SdkImpl sdk = selected.clone();
                    sdk.setName(newName);
                    sdk.setPredefined(false);

                    sdksModel.doAdd(sdk, SdkListConfigurable.this::addSdkNode);
                 }, uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(
                getSelectedValues().size() == 1 && getSelectedValue() instanceof SdkImpl sdk && !(sdk.getSdkType() instanceof UnknownSdkType)
            );
        }
    }
}
