/*
 * Copyright 2013-2016 consulo.io
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
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
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

    public void add(String name, Image icon, NewModuleBuilderProcessor<?> processor) {
      myItems.add(new Item(name, icon, processor));
    }

    public String getName() {
      return myName;
    }

    public String getId() {
      return myId;
    }

    @Nonnull
    public Set<Item> getItems() {
      return myItems;
    }

    @Override
    public int compareTo(@Nonnull Group o) {
      int weight = getWeight();
      int oWeight = o.getWeight();
      if (weight != oWeight) {
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
    private Image myIcon;
    private NewModuleBuilderProcessor<?> myProcessor;

    public Item(String name, Image icon, NewModuleBuilderProcessor<?> processor) {
      myName = name;
      myIcon = icon;
      myProcessor = processor;
    }

    public String getName() {
      return myName;
    }

    public Image getIcon() {
      return myIcon;
    }

    public NewModuleBuilderProcessor<?> getProcessor() {
      return myProcessor;
    }

    @Override
    public int compareTo(@Nonnull Item o) {
      return myName.compareTo(o.myName);
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  private final Map<String, Group> myGroups = new HashMap<>();

  @Nonnull
  public Group get(@Nonnull String id) {
    Group group = myGroups.get(id);
    if (group == null) {
      throw new IllegalArgumentException("Group with " + id + " is not registered");
    }
    return group;
  }

  @Nonnull
  public Group createGroup(@Nonnull String id, @Nonnull String name) {
    return myGroups.computeIfAbsent(id, (s) -> new Group(id, name));
  }

  @Nonnull
  public Group[] getGroups() {
    Group[] groups = myGroups.values().toArray(new Group[myGroups.size()]);
    ContainerUtil.sort(groups);
    return groups;
  }
}
