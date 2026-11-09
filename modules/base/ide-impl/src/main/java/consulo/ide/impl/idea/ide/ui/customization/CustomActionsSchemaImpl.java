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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author anna
 * @since 2005-01-20
 */
@Singleton
@ServiceImpl
@State(name = "CustomActionsSchema", storages = @Storage("customization.xml"))
public class CustomActionsSchemaImpl implements CustomActionsSchema, PersistentStateComponentAsync<Element> {
    private static final String ACTIONS_SCHEMA = "custom_actions_schema";
    private static final String ACTIVE = "active";
    private static final String ELEMENT_ACTION = "action";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_ICON = "icon";
    private static final String GROUP = "group";

    private final Map<String, @Nullable String> myIconCustomizations = new HashMap<>();

    private final Map<String, @Nullable Image> myOriginalIcons = new HashMap<>();

    private List<ActionUrl> myActions = new ArrayList<>();

    private final Map<String, ActionGroup> myIdToActionGroup = new HashMap<>();

    private final Map<String, LocalizeValue> myIdToNameList = new HashMap<>();

    private static final Logger LOG = Logger.getInstance(CustomActionsSchemaImpl.class);

    private boolean myInitial = true;

    @Inject
    public CustomActionsSchemaImpl(Application application) {
        myIdToNameList.put(IdeActions.GROUP_MAIN_MENU, KeyMapLocalize.mainMenuActionTitle());
        myIdToNameList.put(IdeActions.GROUP_MAIN_TOOLBAR, KeyMapLocalize.mainToolbarTitle());
        myIdToNameList.put(IdeActions.GROUP_EDITOR_POPUP, KeyMapLocalize.editorPopupMenuTitle());
        myIdToNameList.put(IdeActions.GROUP_EDITOR_GUTTER, LocalizeValue.localizeTODO("Editor Gutter Popup Menu"));
        myIdToNameList.put(IdeActions.GROUP_EDITOR_TAB_POPUP, KeyMapLocalize.editorTabPopupMenuTitle());
        myIdToNameList.put(IdeActions.GROUP_PROJECT_VIEW_POPUP, KeyMapLocalize.projectViewPopupMenuTitle());
        myIdToNameList.put(IdeActions.GROUP_SCOPE_VIEW_POPUP, LocalizeValue.localizeTODO("Scope View Popup Menu"));
        myIdToNameList.put(IdeActions.GROUP_FAVORITES_VIEW_POPUP, KeyMapLocalize.favoritesPopupTitle());
        myIdToNameList.put(IdeActions.GROUP_COMMANDER_POPUP, KeyMapLocalize.commenderViewPopupMenuTitle());
        myIdToNameList.put(IdeActions.GROUP_J2EE_VIEW_POPUP, KeyMapLocalize.j2eeViewPopupMenuTitle());
        myIdToNameList.put(IdeActions.GROUP_NAVBAR_POPUP, LocalizeValue.localizeTODO("Navigation Bar"));
        myIdToNameList.put("NavBarToolBar", LocalizeValue.localizeTODO("Navigation Bar Toolbar"));
        myIdToNameList.put(IdeActions.GROUP_TOUCHBAR, LocalizeValue.localizeTODO("Touch Bar"));

        CustomizableActionGroupProvider.CustomizableActionGroupRegistrar registrar = myIdToNameList::put;

        application.getExtensionPoint(CustomizableActionGroupProvider.class).forEach(provider -> {
            provider.registerGroups(registrar);
        });
    }

    public static CompletableFuture<CustomActionsSchemaImpl> getInstanceAsync() {
        return CustomActionsSchema.getInstanceAsync().thenApply(CustomActionsSchemaImpl.class::cast);
    }

    public void addAction(ActionUrl url) {
        myActions.add(url);
        resortActions();
    }

    public List<ActionUrl> getActions() {
        return myActions;
    }

    public void setActions(List<ActionUrl> actions) {
        myActions = actions;
        resortActions();
    }

    public void copyFrom(CustomActionsSchemaImpl result) {
        myIdToActionGroup.clear();
        myActions.clear();
        myIconCustomizations.clear();

        for (ActionUrl actionUrl : result.myActions) {
            myActions.add(actionUrl.copy());
        }
        resortActions();

        myIconCustomizations.putAll(result.myIconCustomizations);
    }

    private void resortActions() {
        Collections.sort(myActions, ActionUrlComparator.INSTANCE);
    }

    public boolean isModified(CustomActionsSchemaImpl schema) {
        List<ActionUrl> storedActions = schema.getActions();
        if (storedActions.size() != getActions().size()) {
            return true;
        }
        for (int i = 0; i < getActions().size(); i++) {
            if (!getActions().get(i).equals(storedActions.get(i))) {
                return true;
            }
        }
        if (schema.myIconCustomizations.size() != myIconCustomizations.size()) {
            return true;
        }
        for (String actionId : myIconCustomizations.keySet()) {
            if (!Comparing.strEqual(schema.getIconPath(actionId), getIconPath(actionId))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Coroutine<?, ?> loadState(Element element) {
        return Coroutine.<Object, Object>first(CodeExecution.apply(input -> {
                readState(element);
                return null;
            }))
            .then(CompletableFutureStep.await(input -> resolveActionUrlsAsync()));
    }

    /**
     * Expands the group of every url read from the storage. Deserialization only records group ids, since
     * walking an action group is asynchronous.
     */
    private CompletableFuture<?> resolveActionUrlsAsync() {
        CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
        for (ActionUrl actionUrl : myActions) {
            chain = chain.thenCompose(ignored -> actionUrl.resolveComponentAsync());
        }
        return chain;
    }

    private void readState(Element element) {
        Element schElement = element;
        String activeName = element.getAttributeValue(ACTIVE);
        if (activeName != null) {
            for (Element toolbarElement : element.getChildren(ACTIONS_SCHEMA)) {
                for (Object o : toolbarElement.getChildren("option")) {
                    if (Comparing.strEqual(((Element) o).getAttributeValue("name"), "myName")
                        && Comparing.strEqual(((Element) o).getAttributeValue("value"), activeName)) {
                        schElement = toolbarElement;
                        break;
                    }
                }
            }
        }
        for (Object groupElement : schElement.getChildren(GROUP)) {
            ActionUrl url = new ActionUrl();
            url.readExternal((Element) groupElement);
            myActions.add(url);
        }

        readIcons(element);
    }

    @Override
    public Coroutine<?, @Nullable Element> getState() {
        return Coroutine.first(CodeExecution.apply(input -> {
            Element element = new Element("state");
            writeActions(element);
            writeIcons(element);
            return element;
        }));
    }

    private void writeActions(Element element) throws WriteExternalException {
        for (ActionUrl group : myActions) {
            Element groupElement = new Element(GROUP);
            group.writeExternal(groupElement);
            element.addContent(groupElement);
        }
    }

    @Override
    public AnAction getCorrectedAction(String id) {
        if (!myIdToNameList.containsKey(id)) {
            return ActionManager.getInstance().getAction(id);
        }

        if (myIdToActionGroup.get(id) == null) {
            LocalizeValue actionText = myIdToNameList.getOrDefault(id, LocalizeValue.empty());

            if (actionText.isNotEmpty()) {
                ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction(id);
                if (actionGroup != null) {
                    myIdToActionGroup.put(id, CustomizationUtil.correctActionGroup(actionGroup, this, actionText.get()));
                }
            }
        }
        return myIdToActionGroup.get(id);
    }

    public void resetMainActionGroups() {
        for (Map.Entry<String, LocalizeValue> entry : myIdToNameList.entrySet()) {
            ActionGroup actionGroup = (ActionGroup)ActionManager.getInstance().getAction(entry.getKey());
            if (actionGroup != null) {
                myIdToActionGroup.put(entry.getKey(), CustomizationUtil.correctActionGroup(actionGroup, this, entry.getValue().get()));
            }
        }
    }

    /**
     * Builds the keymap group of every customizable action group, in a fresh tree nobody else holds.
     */
    public CompletableFuture<List<KeymapGroupImpl>> createActionGroupsAsync() {
        ActionManager actionManager = ActionManager.getInstance();
        List<KeymapGroupImpl> groups = new ArrayList<>();
        CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, LocalizeValue> entry : myIdToNameList.entrySet()) {
            ActionGroup actionGroup = (ActionGroup)actionManager.getAction(entry.getKey());
            if (actionGroup == null) {
                continue;
            }
            chain = chain.thenCompose(ignored ->
                KeymapUtil.createGroupAsync(actionGroup, entry.getValue(), null, null, true, null, false)
                    .thenAccept(group -> groups.add((KeymapGroupImpl)group)));
        }
        return chain.thenApply(ignored -> groups);
    }

    public boolean isCorrectActionGroup(ActionGroup group, String defaultGroupName) {
        if (myActions.isEmpty()) {
            return false;
        }

        String text = group.getTemplatePresentation().getText();
        if (!StringUtil.isEmpty(text)) {
            for (ActionUrl url : myActions) {
                if (url.getGroupPath().contains(text) || url.getGroupPath().contains(defaultGroupName)) {
                    return true;
                }
                if (url.getComponent() instanceof KeymapGroupImpl urlGroup) {
                    String id = urlGroup.getName() != null ? urlGroup.getName() : urlGroup.getId();
                    if (id == null || id.equals(text) || id.equals(defaultGroupName)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public List<ActionUrl> getChildActions(ActionUrl url) {
        List<ActionUrl> result = new ArrayList<>();
        List<String> groupPath = url.getGroupPath();
        for (ActionUrl actionUrl : myActions) {
            int index = 0;
            if (groupPath.size() <= actionUrl.getGroupPath().size()) {
                while (index < groupPath.size()) {
                    if (!Comparing.equal(groupPath.get(index), actionUrl.getGroupPath().get(index))) {
                        break;
                    }
                    index++;
                }
                if (index == groupPath.size()) {
                    result.add(actionUrl);
                }
            }
        }
        return result;
    }

    public void removeIconCustomization(String actionId) {
        myIconCustomizations.remove(actionId);
    }

    public void addIconCustomization(String actionId, @Nullable String iconPath) {
        myIconCustomizations.put(actionId, iconPath != null ? FileUtil.toSystemIndependentName(iconPath) : null);
    }

    public String getIconPath(String actionId) {
        String path = myIconCustomizations.get(actionId);
        return path == null ? "" : path;
    }

    public boolean hasIconCustomization(String actionId) {
        return myIconCustomizations.containsKey(actionId);
    }

    public Map<String, @Nullable String> getIconCustomizations() {
        return Collections.unmodifiableMap(myIconCustomizations);
    }

    /**
     * The icon the action shows with this schema applied.
     *
     * @param applicationSchema the schema that knows the icons the actions were registered with
     */
    public @Nullable Image getActionIcon(String actionId, CustomActionsSchemaImpl applicationSchema) {
        if (!myIconCustomizations.containsKey(actionId)) {
            return applicationSchema.getOriginalIcon(actionId);
        }
        String reference = myIconCustomizations.get(actionId);
        if (reference == null) {
            return PlatformIconGroup.toolbarUnknown();
        }
        Image icon = applicationSchema.loadIcon(reference);
        return icon != null ? icon : PlatformIconGroup.actionsHelp();
    }

    /**
     * The icon the action was registered with. Only the application schema records the icons it replaced, so ask that one.
     */
    public @Nullable Image getOriginalIcon(String actionId) {
        if (myOriginalIcons.containsKey(actionId)) {
            return myOriginalIcons.get(actionId);
        }
        AnAction action = ActionManager.getInstance().getAction(actionId);
        return action != null ? action.getTemplatePresentation().getIcon() : null;
    }

    /**
     * An icon reference is the id of the action to take the icon from, an icon key ({@code groupId@imageId}) or the path
     * of an icon file.
     */
    public @Nullable Image loadIcon(String reference) {
        if (ActionManager.getInstance().getActionOrStub(reference) != null) {
            return getOriginalIcon(reference);
        }
        ImageKey imageKey = ImageKey.fromString(reference, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
        if (imageKey != null) {
            return imageKey;
        }
        return loadIconFile(reference);
    }

    public static String getIconReference(ImageKey imageKey) {
        return imageKey.getGroupId() + "@" + imageKey.getImageId();
    }

    public static @Nullable Image loadIconFile(String path) {
        File file = new File(FileUtil.toSystemDependentName(path));
        if (!file.isFile()) {
            return null;
        }
        try {
            return Image.fromUrl(VirtualFileUtil.convertToURL(VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(path))));
        }
        catch (IOException e) {
            LOG.warn(e);
            return null;
        }
    }

    private void readIcons(Element parent) {
        for (Object actionO : parent.getChildren(ELEMENT_ACTION)) {
            Element action = (Element)actionO;
            String actionId = action.getAttributeValue(ATTRIBUTE_ID);
            String iconPath = action.getAttributeValue(ATTRIBUTE_ICON);
            if (actionId != null) {
                myIconCustomizations.put(actionId, iconPath);
            }
        }
        SwingUtilities.invokeLater(this::initActionIcons);
    }

    private void writeIcons(Element parent) {
        for (String actionId : myIconCustomizations.keySet()) {
            Element action = new Element(ELEMENT_ACTION);
            action.setAttribute(ATTRIBUTE_ID, actionId);
            String icon = myIconCustomizations.get(actionId);
            if (icon != null) {
                action.setAttribute(ATTRIBUTE_ICON, icon);
            }
            parent.addContent(action);
        }
    }

    public void initActionIcons() {
        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : List.copyOf(myOriginalIcons.keySet())) {
            if (!myIconCustomizations.containsKey(actionId)) {
                AnAction anAction = actionManager.getAction(actionId);
                Image original = myOriginalIcons.remove(actionId);
                if (anAction != null) {
                    anAction.getTemplatePresentation().setIcon(original);
                    anAction.setDefaultIcon(true);
                }
            }
        }

        for (String actionId : myIconCustomizations.keySet()) {
            AnAction anAction = actionManager.getAction(actionId);
            if (anAction == null) {
                continue;
            }
            if (!myOriginalIcons.containsKey(actionId)) {
                myOriginalIcons.put(actionId, anAction.getTemplatePresentation().getIcon());
            }

            anAction.getTemplatePresentation().setIcon(getActionIcon(actionId, this));
            anAction.setDefaultIcon(false);
        }

        if (!myInitial) {
            IdeFrameEx frame = WindowManagerEx.getInstanceEx().getIdeFrame(null);
            if (frame != null) {
                frame.updateView();
            }
        }

        myInitial = false;
    }

    private static class Pair {
        String first;
        String second;

        public Pair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int hashCode() {
            return first.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj == this
                || obj instanceof Pair pair && first.equals(pair.first);
        }
    }

    private static class ActionUrlComparator implements Comparator<ActionUrl> {
        public static ActionUrlComparator INSTANCE = new ActionUrlComparator();
        private static final int DELETED = 1;
        private static final int ADDED = 2;

        @Override
        public int compare(ActionUrl u1, ActionUrl u2) {
            int w1 = getEquivalenceClass(u1);
            int w2 = getEquivalenceClass(u2);
            if (w1 != w2) {
                return w1 - w2; // deleted < added < others
            }
            if (w1 == DELETED) {
                return u2.getAbsolutePosition() - u1.getAbsolutePosition(); // within DELETED equivalence class urls with greater position go first
            }
            return u1.getAbsolutePosition() - u2.getAbsolutePosition(); // within ADDED equivalence class: urls with lower position go first
        }

        private static int getEquivalenceClass(ActionUrl url) {
            return switch (url.getActionType()) {
                case ActionUrl.DELETED -> 1;
                case ActionUrl.ADDED -> 2;
                default -> 3;
            };
        }
    }
}
