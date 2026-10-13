/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ai.impl.internal.setting;

import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderType;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The AI accounts, grouped by their type the way the SDK table groups bundles: a type in the tree holds its
 * accounts, an account opens its details.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
public class AIProviderConfigurable extends UnifiedMasterDetailsComponent
    implements ApplicationConfigurable, Configurable.NoMargin, Configurable.NoScroll {

    private final Provider<AIProviderTable> myProviderTable;
    private final Provider<AISettings> mySettings;

    private final List<AIProviderDraft> myDrafts = new ArrayList<>();
    private final Set<String> myRemovedNames = new HashSet<>();

    @Inject
    public AIProviderConfigurable(Provider<AIProviderTable> providerTable, Provider<AISettings> settings) {
        myProviderTable = providerTable;
        mySettings = settings;
    }

    @Override
    public String getId() {
        return "ai.providers";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.configurableDisplayName();
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return AILocalize.labelEmptySelection();
    }

    @Override
    protected boolean isExpandAllByDefault() {
        return true;
    }

    @Override
    protected boolean isRemovedWhenEmpty(Object value) {
        return value instanceof AIProviderType;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.comparing(this::getValueText, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof AIProviderType type) {
            return type.getDisplayName().get();
        }
        if (value instanceof AIProviderDraft draft) {
            return draft.name;
        }
        return String.valueOf(value);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof AIProviderType type) {
            presentation.withIcon(type.getIcon());
            presentation.append(type.getDisplayName(), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof AIProviderDraft draft) {
            presentation.withIcon(draft.type.getIcon());
            presentation.append(draft.name);
            if (draft.isDefault) {
                presentation.append(" (" + AILocalize.labelDefaultProvider().get() + ")", TextAttribute.GRAYED);
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        myDrafts.clear();
        myRemovedNames.clear();

        AISettings settings = mySettings.get();
        for (AIProvider provider : myProviderTable.get().getProviders()) {
            AIProviderDraft draft = new AIProviderDraft(provider.getName(), provider.getName(), provider.getType());
            draft.options.putAll(provider.getOptions());
            draft.originalOptions.putAll(provider.getOptions());
            if (provider.getName().equals(settings.DEFAULT_PROVIDER_NAME)) {
                draft.isDefault = true;
                draft.defaultModelId = StringUtil.nullize(settings.DEFAULT_MODEL_ID);
            }
            myDrafts.add(draft);
        }

        for (AIProviderDraft draft : myDrafts) {
            if (!isAttached(draft.type)) {
                addValue(null, draft.type);
            }
            addValue(draft.type, draft);
        }
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (value instanceof AIProviderDraft draft) {
            return new AIProviderDetailsConfigurable(draft, myProviderTable.get(), () -> refreshValue(draft));
        }
        return null;
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddAction(), new RemoveAction(), new CopyAction());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (super.isModified() || !myRemovedNames.isEmpty()) {
            return true;
        }
        AISettings settings = mySettings.get();
        for (AIProviderDraft draft : myDrafts) {
            if (draft.originalName == null
                || !draft.originalName.equals(draft.name)
                || draft.apiKeyChanged
                || !draft.options.equals(draft.originalOptions)) {
                return true;
            }
            boolean storedDefault = draft.originalName.equals(settings.DEFAULT_PROVIDER_NAME);
            if (storedDefault != draft.isDefault) {
                return true;
            }
            if (draft.isDefault && !Objects.equals(StringUtil.nullize(settings.DEFAULT_MODEL_ID), draft.defaultModelId)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        super.apply();

        Set<String> names = new HashSet<>();
        for (AIProviderDraft draft : myDrafts) {
            if (!names.add(draft.name)) {
                throw new ConfigurationException(AILocalize.errorNameUnique(draft.name));
            }
        }

        AIProviderTable table = myProviderTable.get();
        for (String removed : myRemovedNames) {
            AIProvider provider = table.findProvider(removed);
            if (provider != null) {
                table.removeProvider(provider);
            }
        }
        myRemovedNames.clear();

        AIProviderDraft defaultDraft = null;
        for (AIProviderDraft draft : myDrafts) {
            applyDraft(table, draft);
            for (AISettings.ProviderEntry entry : mySettings.get().PROVIDERS) {
                if (entry.NAME.equals(draft.name)) {
                    entry.OPTIONS = new LinkedHashMap<>(draft.options);
                }
            }
            draft.originalOptions.clear();
            draft.originalOptions.putAll(draft.options);
            if (draft.isDefault) {
                defaultDraft = draft;
            }
        }

        if (defaultDraft == null) {
            mySettings.get().DEFAULT_PROVIDER_NAME = "";
            mySettings.get().DEFAULT_MODEL_ID = "";
        }
        else {
            AIProvider provider = table.findProvider(defaultDraft.name);
            AIModel model = null;
            for (AIModel candidate : defaultDraft.type.getModels()) {
                if (candidate.getId().equals(defaultDraft.defaultModelId)) {
                    model = candidate;
                }
            }
            table.setDefault(provider, model);
        }
    }

    private static void applyDraft(AIProviderTable table, AIProviderDraft draft) {
        String originalName = draft.originalName;
        if (originalName == null) {
            AIProvider provider = table.addProvider(draft.name, draft.type);
            if (draft.apiKey != null && !draft.apiKey.isEmpty()) {
                provider.setApiKey(draft.apiKey);
            }
        }
        else if (!originalName.equals(draft.name)) {
            AIProvider old = table.findProvider(originalName);
            CompletableFuture<@Nullable String> key = draft.apiKey != null || old == null
                ? CompletableFuture.completedFuture(draft.apiKey)
                : old.getApiKey();
            if (old != null) {
                key = key.thenCompose(value -> table.removeProvider(old).thenApply(ignored -> value));
            }
            AIProvider provider = table.addProvider(draft.name, draft.type);
            key.thenAccept(value -> {
                if (value != null && !value.isEmpty()) {
                    provider.setApiKey(value);
                }
            });
        }
        else if (draft.apiKeyChanged) {
            AIProvider provider = table.findProvider(draft.name);
            if (provider != null) {
                provider.setApiKey(draft.apiKey);
            }
        }
        draft.originalName = draft.name;
        draft.apiKeyChanged = false;
    }

    private String uniqueName(String base) {
        Set<String> names = new HashSet<>();
        for (AIProviderDraft draft : myDrafts) {
            names.add(draft.name);
        }
        if (!names.contains(base)) {
            return base;
        }
        int index = 2;
        while (names.contains(base + " " + index)) {
            index++;
        }
        return base + " " + index;
    }

    @RequiredUIAccess
    private void addDraft(AIProviderDraft draft) {
        myDrafts.add(draft);
        if (!isAttached(draft.type)) {
            addValue(null, draft.type);
        }
        addValue(draft.type, draft);
        refreshTree(draft);
    }

    private class AddAction extends DumbAwareAction {
        private AddAction() {
            super(AILocalize.actionAddAccount(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Tree<Object> tree = getTree();
            if (tree == null) {
                return;
            }

            DefaultActionGroup group = new DefaultActionGroup();
            for (AIProviderType type : myProviderTable.get().getTypes()) {
                group.add(new DumbAwareAction(type.getDisplayName(), LocalizeValue.empty(), type.getIcon()) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        addDraft(new AIProviderDraft(null, uniqueName(type.getDisplayName().get()), type));
                    }
                });
            }

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                AILocalize.actionAddAccount().get(),
                group,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class RemoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveAction() {
            super(AILocalize.actionRemoveAccount(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Object> selected = getSelectedValues();
            if (!canRemove(selected)) {
                return;
            }
            for (Object value : selected) {
                AIProviderDraft draft = (AIProviderDraft) value;
                myDrafts.remove(draft);
                if (draft.originalName != null) {
                    myRemovedNames.add(draft.originalName);
                }
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
                if (!(value instanceof AIProviderDraft)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class CopyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CopyAction() {
            super(AILocalize.actionCopyAccount(), LocalizeValue.empty(), PlatformIconGroup.actionsCopy());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (!(getSelectedValue() instanceof AIProviderDraft selected)) {
                return;
            }
            AIProviderDraft copy = new AIProviderDraft(null, uniqueName(selected.name), selected.type);
            copy.apiKey = selected.apiKey;
            copy.defaultModelId = selected.defaultModelId;
            copy.options.putAll(selected.options);
            addDraft(copy);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedValues().size() == 1 && getSelectedValue() instanceof AIProviderDraft);
        }
    }
}
