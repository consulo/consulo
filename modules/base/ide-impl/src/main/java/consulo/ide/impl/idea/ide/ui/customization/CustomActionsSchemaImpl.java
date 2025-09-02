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
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author anna
 * @since 2005-01-20
 */
@Singleton
@ServiceImpl
@State(name = "CustomActionsSchema", storages = @Storage("customization.xml"))
public class CustomActionsSchemaImpl implements CustomActionsSchema, PersistentStateComponent<Element> {
    private static final String ACTIONS_SCHEMA = "custom_actions_schema";
    private static final String ACTIVE = "active";
    private static final String ELEMENT_ACTION = "action";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_ICON = "icon";
    private static final String GROUP = "group";

    private final Map<String, String> myIconCustomizations = new HashMap<>();

    private ArrayList<ActionUrl> myActions = new ArrayList<>();

    private final HashMap<String, ActionGroup> myIdToActionGroup = new HashMap<>();

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

    public static CustomActionsSchemaImpl getInstance() {
        return (CustomActionsSchemaImpl)CustomActionsSchema.getInstance();
    }

    public void addAction(ActionUrl url) {
        myActions.add(url);
        resortActions();
    }

    public ArrayList<ActionUrl> getActions() {
        return myActions;
    }

    public void setActions(ArrayList<ActionUrl> actions) {
        myActions = actions;
        resortActions();
    }

    public void copyFrom(CustomActionsSchemaImpl result) {
        myIdToActionGroup.clear();
        myActions.clear();
        myIconCustomizations.clear();

        for (ActionUrl actionUrl : result.myActions) {
            ActionUrl url = new ActionUrl(
                new ArrayList<>(actionUrl.getGroupPath()),
                actionUrl.getComponent(),
                actionUrl.getActionType(),
                actionUrl.getAbsolutePosition()
            );
            url.setInitialPosition(actionUrl.getInitialPosition());
            myActions.add(url);
        }
        resortActions();

        myIconCustomizations.putAll(result.myIconCustomizations);
    }

    private void resortActions() {
        Collections.sort(myActions, ActionUrlComparator.INSTANCE);
    }

    public boolean isModified(CustomActionsSchemaImpl schema) {
        ArrayList<ActionUrl> storedActions = schema.getActions();
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
    public void loadState(Element element) {
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

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        writeActions(element);
        writeIcons(element);
        return element;
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
            LocalizeValue actionText = myIdToNameList.getOrDefault(id, LocalizeValue.of());

            if (actionText != LocalizeValue.of()) {
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

    public void fillActionGroups(DefaultMutableTreeNode root) {
        ActionManager actionManager = ActionManager.getInstance();
        for (Map.Entry<String, LocalizeValue> entry : myIdToNameList.entrySet()) {
            ActionGroup actionGroup = (ActionGroup)actionManager.getAction(entry.getKey());
            if (actionGroup != null) {
                LocalizeValue actionText = entry.getValue();
                root.add(ActionsTreeUtil.createNode(ActionsTreeUtil.createGroup(actionGroup, actionText.get(), null, null, true, null, false)));
            }
        }
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
        ArrayList<ActionUrl> result = new ArrayList<>();
        ArrayList<String> groupPath = url.getGroupPath();
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

    public void addIconCustomization(String actionId, String iconPath) {
        myIconCustomizations.put(actionId, iconPath != null ? FileUtil.toSystemIndependentName(iconPath) : null);
    }

    public String getIconPath(String actionId) {
        String path = myIconCustomizations.get(actionId);
        return path == null ? "" : path;
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

    private void initActionIcons() {
        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : myIconCustomizations.keySet()) {
            AnAction anAction = actionManager.getAction(actionId);
            if (anAction != null) {
                Image icon;
                String iconPath = myIconCustomizations.get(actionId);
                if (iconPath != null && new File(FileUtil.toSystemDependentName(iconPath)).exists()) {
                    try {
                        icon = Image.fromUrl(VfsUtil.convertToURL(VfsUtil.pathToUrl(iconPath)));
                    }
                    catch (IOException e) {
                        icon = PlatformIconGroup.actionsHelp();

                        LOG.warn(e);
                    }
                }
                else {
                    icon = PlatformIconGroup.actionsHelp();
                }
                if (anAction.getTemplatePresentation() != null) {
                    anAction.getTemplatePresentation().setIcon(icon);
                    anAction.setDefaultIcon(false);
                }
            }
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
        public boolean equals(Object obj) {
            return obj instanceof Pair pair && first.equals(pair.first);
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
            switch (url.getActionType()) {
                case ActionUrl.DELETED:
                    return 1;
                case ActionUrl.ADDED:
                    return 2;
                default:
                    return 3;
            }
        }
    }
}
