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
package com.intellij.openapi.keymap.impl.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.QuickList;
import com.intellij.openapi.keymap.KeymapGroup;
import com.intellij.openapi.util.text.StringUtil;
import consulo.ui.image.Image;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * User: anna
 * Date: Mar 18, 2005
 */
public class Group implements KeymapGroup {
  private Group myParent;
  private final String myName;
  private String myId;
  private final Image myIcon;
  /**
   * Group or action id (String) or Separator or QuickList
   */
  private final ArrayList<Object> myChildren;

  private final Set<String> myIds = new HashSet<String>();

  public Group(String name, String id, Image icon) {
    myName = name;
    myId = id;
    myIcon = icon;
    myChildren = new ArrayList<>();
  }

  public Group(final String name, final Image icon) {
    myIcon = icon;
    myName = name;
    myChildren = new ArrayList<>();
  }

  public String getName() {
    return myName;
  }

  public Image getIcon() {
    return myIcon;
  }

  public String getId() {
    return myId;
  }

  public void addActionId(String id) {
    myChildren.add(id);
  }

  public void addQuickList(QuickList list) {
    myChildren.add(list);
  }

  public void addGroup(KeymapGroup keymapGroup) {
    Group group = (Group) keymapGroup;
    myChildren.add(group);
    group.myParent = this;
  }

  public void addSeparator() {
    myChildren.add(AnSeparator.getInstance());
  }

  public boolean containsId(String id) {
    return myIds.contains(id);
  }

  public Set<String> initIds(){
    for (Object child : myChildren) {
      if (child instanceof String) {
        myIds.add((String)child);
      }
      else if (child instanceof QuickList) {
        myIds.add(((QuickList)child).getActionId());
      }
      else if (child instanceof Group) {
        myIds.addAll(((Group)child).initIds());
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

  public void normalizeSeparators() {
    while (myChildren.size() > 0 && myChildren.get(0) instanceof AnSeparator) {
      myChildren.remove(0);
    }

    while (myChildren.size() > 0 && myChildren.get(myChildren.size() - 1) instanceof AnSeparator) {
      myChildren.remove(myChildren.size() - 1);
    }

    for (int i=1; i < myChildren.size() - 1; i++) {
      if (myChildren.get(i) instanceof AnSeparator && myChildren.get(i + 1) instanceof AnSeparator) {
        myChildren.remove(i);
        i--;
      }
    }
  }

  public String getActionQualifiedPath(String id) {
    Group cur = myParent;
    StringBuilder answer = new StringBuilder();

    while (cur != null && !cur.isRoot()) {
      answer.insert(0, cur.getName() + " | ");

      cur = cur.myParent;
    }

    String suffix = calcActionQualifiedPath(id);
    if (StringUtil.isEmpty(suffix)) return null;

    answer.append(suffix);

    return answer.toString();
  }

  private String calcActionQualifiedPath(String id) {
    for (Object child : myChildren) {
      if (child instanceof QuickList) {
        child = ((QuickList)child).getActionId();
      }
      if (child instanceof String) {
        if (id.equals(child)) {
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
      else if (child instanceof Group) {
        String path = ((Group)child).calcActionQualifiedPath(id);
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
    Group group = this;
    while (group != null && !group.isRoot()) {
      if (path.length() > 0) path.insert(0, " | ");
      path.insert(0, group.getName());
      group = group.myParent;
    }
    return path.toString();
  }

  public void addAll(Group group) {
    for (Object o : group.getChildren()) {
      if (o instanceof String) {
        addActionId((String)o);
      }
      else if (o instanceof QuickList) {
        addQuickList((QuickList)o);
      }
      else if (o instanceof Group) {
        addGroup((Group)o);
      }
      else if (o instanceof AnSeparator) {
        addSeparator();
      }
    }
  }

  public ActionGroup constructActionGroup(final boolean popup){
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup group = new DefaultActionGroup(getName(), popup);
    AnAction groupToRestorePresentation = null;
    if (getName() != null){
      groupToRestorePresentation = actionManager.getAction(getName());
    } else {
      if (getId() != null){
        groupToRestorePresentation = actionManager.getAction(getId());
      }
    }
    if (groupToRestorePresentation != null){
      group.copyFrom(groupToRestorePresentation);
    }
    for (Object o : myChildren) {
      if (o instanceof String) {
        group.add(actionManager.getAction((String)o));
      }
      else if (o instanceof AnSeparator) {
        group.addSeparator();
      }
      else if (o instanceof Group) {
        group.add(((Group)o).constructActionGroup(popup));
      }
    }
    return group;
  }


  public boolean equals(Object object) {
    if (!(object instanceof Group)) return false;
    final Group group = ((Group)object);
    if (group.getName() != null && getName() != null){
      return group.getName().equals(getName());
    }
    if (getChildren() != null && group.getChildren() != null){
      if (getChildren().size() != group.getChildren().size()){
        return false;
      }

      for (int i = 0; i < getChildren().size(); i++) {
        if (!getChildren().get(i).equals(group.getChildren().get(i))){
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public int hashCode() {
    return getName() != null ? getName().hashCode() : 0;
  }

  public String toString() {
    return getName();
  }
}
