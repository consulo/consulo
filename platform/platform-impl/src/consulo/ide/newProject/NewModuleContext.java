/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.newProject;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class NewModuleContext {
  public static final String UGROUPED = "ungrouped";

  public static class Group implements Comparable<Group> {
    private final Set<Item> myItems = new TreeSet<>();
    private final String myId;
    private final String myName;

    public Group(String id, String name) {
      myId = id;
      myName = name;
    }

    public void add(String name, Icon icon, NewModuleBuilderProcessor<?> processor) {
      myItems.add(new Item(name, icon, processor));
    }

    public String getName() {
      return myName;
    }

    public String getId() {
      return myId;
    }

    @NotNull
    public Set<Item> getItems() {
      return myItems;
    }

    @Override
    public int compareTo(@NotNull Group o) {
      int weight = getWeight();
      int oWeight = o.getWeight();
      if(weight != oWeight) {
        return oWeight - weight;
      }

      return getName().compareTo(o.getName());
    }

    private int getWeight() {
      return getId().equals(UGROUPED) ? 1 : 100;
    }
  }

  public static class Item implements Comparable<Item> {
    private String myName;
    private Icon myIcon;
    private NewModuleBuilderProcessor<?> myProcessor;

    public Item(String name, Icon icon, NewModuleBuilderProcessor<?> processor) {
      myName = name;
      myIcon = icon;
      myProcessor = processor;
    }

    public String getName() {
      return myName;
    }

    public Icon getIcon() {
      return myIcon;
    }

    public NewModuleBuilderProcessor<?> getProcessor() {
      return myProcessor;
    }

    @Override
    public int compareTo(@NotNull Item o) {
      return myName.compareTo(o.myName);
    }
  }

  private final Map<String, Group> myGroups = new HashMap<>();

  @NotNull
  public Group get(@NotNull String id) {
    Group group = myGroups.get(id);
    if (group == null) {
      throw new IllegalArgumentException("Group with " + id + " is not registered");
    }
    return group;
  }

  @NotNull
  public Group createGroup(@NotNull String id, @NotNull String name) {
    return myGroups.computeIfAbsent(id, (s) -> new Group(id, name));
  }

  @NotNull
  public Group[] getGroups() {
    Group[] groups = myGroups.values().toArray(new Group[myGroups.size()]);
    ContainerUtil.sort(groups);
    return groups;
  }
}
