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
package consulo.ide.impl.ui;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class DListItem {
  public static class Builder {
    @Nullable
    private DListItem myParent;
    @Nullable
    private String myName;
    @Nullable
    private Icon myIcon;
    @Nullable
    private Object myAttach;
    @NotNull
    private List<Builder> myItems = new ArrayList<Builder>();

    @NotNull
    public Builder withName(@NotNull String name) {
      myName = name;
      return this;
    }

    @NotNull
    public Builder withParent(@Nullable DListItem parent) {
      myParent = parent;
      return this;
    }

    @NotNull
    public Builder withIcon(@Nullable Icon icon) {
      myIcon = icon;
      return this;
    }

    @NotNull
    public Builder withAttach(@Nullable Object attach) {
      myAttach = attach;
      return this;
    }

    @NotNull
    public Builder withItems(@NotNull Collection<? extends Builder> item) {
      myItems.addAll(item);
      return this;
    }

    @NotNull
    public Builder withItems(@NotNull Builder... items) {
      Collections.addAll(myItems, items);
      return this;
    }

    @NotNull
    public DListItem create() {
      List<DListItem> child = new ArrayList<DListItem>();

      DListItem item = new DListItem(myParent, myName, myIcon, myAttach, child);
      for (Builder builder : myItems) {
        builder.withParent(item);
        child.add(builder.create());
      }
      ContainerUtil.sort(child, new Comparator<DListItem>() {
        @Override
        public int compare(DListItem o1, DListItem o2) {
          boolean empty1 = o1.getItems().isEmpty();
          boolean empty2 = o2.getItems().isEmpty();
          if (empty1 && empty2 || !empty1 && !empty2) {
            return StringUtil.compare(o1.getName(), o2.getName(), true);
          }
          if (empty1) {
            return 1;
          }
          else {
            return -1;
          }
        }
      });
      return item;
    }
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  @Nullable
  private DListItem myParent;
  @Nullable
  private final String myName;
  @Nullable
  private final Icon myIcon;
  @Nullable
  private final Object myAttach;

  @NotNull
  private List<DListItem> myItems;

  private DListItem(@Nullable DListItem parent, @Nullable String name, @Nullable Icon icon, @Nullable Object attach, @NotNull List<DListItem> items) {
    myParent = parent;
    myName = name;
    myIcon = icon;
    myAttach = attach;
    myItems = items;
  }

  @Nullable
  public Icon getIcon() {
    return myIcon;
  }

  @Nullable
  public String getName() {
    return myName;
  }

  @Nullable
  public DListItem getParent() {
    return myParent;
  }

  @Nullable
  public Object getAttach() {
    return myAttach;
  }

  @NotNull
  @Immutable
  public List<DListItem> getItems() {
    return myItems;
  }
}
