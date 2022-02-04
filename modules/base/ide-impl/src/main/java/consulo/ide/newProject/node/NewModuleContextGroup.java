/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.newProject.node;

import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 2020-06-02
 */
public class NewModuleContextGroup extends NewModuleContextNode implements Comparable<NewModuleContextGroup> {
  private final Map<String, NewModuleContextGroup> myGroups = new TreeMap<>();

  private final Set<NewModuleContextItem> myItems = new TreeSet<>();

  @Nonnull
  private final String myId;

  public NewModuleContextGroup(@Nonnull String id, @Nonnull LocalizeValue name, @Nullable Image image) {
    super(name, image);
    myId = id;
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  @Override
  public int compareTo(@Nonnull NewModuleContextGroup o) {
    return myId.compareToIgnoreCase(o.myId);
  }

  @Nonnull
  public NewModuleContextGroup findGroup(@Nonnull String id) {
    NewModuleContextGroup group = myGroups.get(id);
    if (group == null) {
      throw new IllegalArgumentException("Group with " + id + " is not registered");
    }
    return group;
  }

  @Nonnull
  public NewModuleContextGroup addGroup(@Nonnull String id, @Nonnull LocalizeValue name) {
    return addGroup(id, name, null);
  }

  @Nonnull
  public NewModuleContextGroup addGroup(@Nonnull String id, @Nonnull LocalizeValue name, @Nullable Image image) {
    return myGroups.computeIfAbsent(id, (s) -> new NewModuleContextGroup(id, name, image));
  }

  public void add(@Nonnull LocalizeValue name, @Nonnull Image icon, NewModuleBuilderProcessor<?> processor) {
    myItems.add(new NewModuleContextItem(name, icon, 0, processor));
  }

  public void add(@Nonnull LocalizeValue name, @Nonnull Image icon, int weight, NewModuleBuilderProcessor<?> processor) {
    myItems.add(new NewModuleContextItem(name, icon, weight, processor));
  }

  @Nonnull
  public List<Object> getAll() {
    List<Object> all = new ArrayList<>();
    all.addAll(myGroups.values());
    all.addAll(myItems);
    return all;
  }
}
