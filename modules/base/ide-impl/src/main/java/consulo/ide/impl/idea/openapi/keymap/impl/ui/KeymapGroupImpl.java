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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.keymap.KeymapGroup;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author anna
 * @since 2005-03-18
 */
public class KeymapGroupImpl implements KeymapGroup {
    private KeymapGroupImpl myParent;
    @Nonnull
    private final LocalizeValue myName;
    private String myId;
    private final Image myIcon;
    /**
     * KeymapGroupImpl or action id (String) or Separator or QuickList
     */
    private final ArrayList<Object> myChildren;

    private final Set<String> myIds = new HashSet<>();

    public KeymapGroupImpl(@Nonnull LocalizeValue name) {
        this(name, null);
    }

    public KeymapGroupImpl(@Nonnull LocalizeValue name, Image icon) {
        this(name, null, icon);
    }

    public KeymapGroupImpl(@Nonnull LocalizeValue name, String id, Image icon) {
        myName = name;
        myId = id;
        myIcon = icon;
        myChildren = new ArrayList<>();
    }

    @Deprecated
    public KeymapGroupImpl(String name) {
        this(LocalizeValue.ofNullable(name));
    }

    @Deprecated
    public KeymapGroupImpl(String name, Image icon) {
        this(LocalizeValue.ofNullable(name), icon);
    }

    @Deprecated
    public KeymapGroupImpl(String name, String id, Image icon) {
        this(LocalizeValue.ofNullable(name), id, icon);
    }

    public String getName() {
        return myName.get();
    }

    public Image getIcon() {
        return myIcon;
    }

    public String getId() {
        return myId;
    }

    @Override
    public void addActionId(String id) {
        myChildren.add(id);
    }

    public void addQuickList(QuickList list) {
        myChildren.add(list);
    }

    @Override
    public void addGroup(KeymapGroup keymapGroup) {
        KeymapGroupImpl group = (KeymapGroupImpl)keymapGroup;
        myChildren.add(group);
        group.myParent = this;
    }

    public void addSeparator() {
        myChildren.add(AnSeparator.getInstance());
    }

    public boolean containsId(String id) {
        return myIds.contains(id);
    }

    public Set<String> initIds() {
        for (Object child : myChildren) {
            if (child instanceof String actionId) {
                myIds.add(actionId);
            }
            else if (child instanceof QuickList quickList) {
                myIds.add(quickList.getActionId());
            }
            else if (child instanceof KeymapGroupImpl group) {
                myIds.addAll(group.initIds());
            }
        }
        return myIds;
    }

    public ArrayList<Object> getChildren() {
        return myChildren;
    }

    public int getSize() {
        return myChildren.size();
    }

    @Override
    public void normalizeSeparators() {
        while (myChildren.size() > 0 && myChildren.get(0) instanceof AnSeparator) {
            myChildren.remove(0);
        }

        while (myChildren.size() > 0 && myChildren.get(myChildren.size() - 1) instanceof AnSeparator) {
            myChildren.remove(myChildren.size() - 1);
        }

        for (int i = 1; i < myChildren.size() - 1; i++) {
            if (myChildren.get(i) instanceof AnSeparator && myChildren.get(i + 1) instanceof AnSeparator) {
                myChildren.remove(i);
                i--;
            }
        }
    }

    public String getActionQualifiedPath(String id) {
        KeymapGroupImpl cur = myParent;
        StringBuilder answer = new StringBuilder();

        while (cur != null && !cur.isRoot()) {
            answer.insert(0, cur.getName() + " | ");

            cur = cur.myParent;
        }

        String suffix = calcActionQualifiedPath(id);
        if (StringUtil.isEmpty(suffix)) {
            return null;
        }

        answer.append(suffix);

        return answer.toString();
    }

    private String calcActionQualifiedPath(String id) {
        for (Object child : myChildren) {
            if (child instanceof QuickList quickList) {
                child = quickList.getActionId();
            }
            if (child instanceof String actionId) {
                if (id.equals(actionId)) {
                    AnAction action = ActionManager.getInstance().getActionOrStub(id);
                    String path;
                    if (action != null) {
                        path = action.getTemplatePresentation().getText();
                    }
                    else {
                        path = id;
                    }
                    return !isRoot() ? getName() + " | " + path : path;
                }
            }
            else if (child instanceof KeymapGroupImpl group) {
                String path = group.calcActionQualifiedPath(id);
                if (path != null) {
                    return !isRoot() ? getName() + " | " + path : path;
                }
            }
        }
        return null;
    }

    public boolean isRoot() {
        return myParent == null;
    }

    public String getQualifiedPath() {
        StringBuilder path = new StringBuilder(64);
        KeymapGroupImpl group = this;
        while (group != null && !group.isRoot()) {
            if (path.length() > 0) {
                path.insert(0, " | ");
            }
            path.insert(0, group.getName());
            group = group.myParent;
        }
        return path.toString();
    }

    public void addAll(KeymapGroupImpl group) {
        for (Object o : group.getChildren()) {
            if (o instanceof String actionId) {
                addActionId(actionId);
            }
            else if (o instanceof QuickList quickList) {
                addQuickList(quickList);
            }
            else if (o instanceof KeymapGroupImpl subGroup) {
                addGroup(subGroup);
            }
            else if (o instanceof AnSeparator) {
                addSeparator();
            }
        }
    }

    public ActionGroup constructActionGroup(boolean popup) {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup group = new DefaultActionGroup(getName(), popup);
        AnAction groupToRestorePresentation = null;
        if (getName() != null) {
            groupToRestorePresentation = actionManager.getAction(getName());
        }
        else {
            if (getId() != null) {
                groupToRestorePresentation = actionManager.getAction(getId());
            }
        }
        if (groupToRestorePresentation != null) {
            group.copyFrom(groupToRestorePresentation);
        }
        for (Object o : myChildren) {
            if (o instanceof String actionId) {
                group.add(actionManager.getAction(actionId));
            }
            else if (o instanceof AnSeparator) {
                group.addSeparator();
            }
            else if (o instanceof KeymapGroupImpl keymapGroup) {
                group.add(keymapGroup.constructActionGroup(popup));
            }
        }
        return group;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof KeymapGroupImpl group)) {
            return false;
        }
        if (group.getName() != null && getName() != null) {
            return group.getName().equals(getName());
        }
        if (getChildren() != null && group.getChildren() != null) {
            if (getChildren().size() != group.getChildren().size()) {
                return false;
            }

            for (int i = 0; i < getChildren().size(); i++) {
                if (!getChildren().get(i).equals(group.getChildren().get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }

    @Override
    public String toString() {
        return getName();
    }
}
