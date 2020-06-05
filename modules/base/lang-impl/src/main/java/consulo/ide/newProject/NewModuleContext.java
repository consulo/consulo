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

import consulo.ide.newProject.node.NewModuleContextGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class NewModuleContext extends NewModuleContextGroup  {
  @Deprecated
  public static final String UGROUPED = "ungrouped";

  @Deprecated
  public static class Group implements Comparable<Group> {
    private final NewModuleContextGroup myGroup;

    public Group(NewModuleContextGroup group) {
      myGroup = group;
    }

    public void add(String name, Image icon, NewModuleBuilderProcessor<?> processor) {
      myGroup.add(LocalizeValue.of(name), icon, processor);
    }

    public String getName() {
      return myGroup.getName().getValue();
    }

    public String getId() {
      return myGroup.getId();
    }

    public Image getImage() {
      return myGroup.getImage();
    }

    @Override
    public int compareTo(@Nonnull Group o) {
      return 0;
    }
  }

  @Deprecated
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
      return 0;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  public NewModuleContext() {
    super("", LocalizeValue.empty(), null);
  }

  @Nonnull
  public Group get(@Nonnull String id) {
    if(UGROUPED.equals(id)) {
      return new Group(this);
    }
    NewModuleContextGroup group = findGroup(id);
    return new Group(group);
  }

  @Nonnull
  @Deprecated
  public Group createGroup(@Nonnull String id, @Nonnull String name) {
    if(UGROUPED.equals(id)) {
      return new Group(this);
    }
    NewModuleContextGroup group = addGroup(id, LocalizeValue.of(name));
    return new Group(group);
  }
}
