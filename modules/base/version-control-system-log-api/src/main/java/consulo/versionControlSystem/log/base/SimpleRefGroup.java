/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.base;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.log.RefGroup;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsRefType;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.*;

public class SimpleRefGroup implements RefGroup {
  @Nonnull
  private final String myName;
  @Nonnull
  private final List<VcsRef> myRefs;

  public SimpleRefGroup(@Nonnull String name, @Nonnull List<VcsRef> refs) {
    myName = name;
    myRefs = refs;
  }

  @Override
  public boolean isExpanded() {
    return false;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public List<VcsRef> getRefs() {
    return myRefs;
  }

  @Nonnull
  @Override
  public List<Color> getColors() {
    return getColors(myRefs);
  }

  @Nonnull
  public static List<Color> getColors(@Nonnull Collection<VcsRef> refs) {
    MultiMap<VcsRefType, VcsRef> referencesByType = ContainerUtil.groupBy(refs, VcsRef::getType);
    if (referencesByType.size() == 1) {
      Map.Entry<VcsRefType, Collection<VcsRef>> firstItem =
              ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(referencesByType.entrySet()));
      boolean multiple = firstItem.getValue().size() > 1;
      Color color = firstItem.getKey().getBackgroundColor();
      return multiple ? Arrays.asList(color, color) : Collections.singletonList(color);
    }
    else {
      List<Color> colorsList = ContainerUtil.newArrayList();
      for (VcsRefType type : referencesByType.keySet()) {
        if (referencesByType.get(type).size() > 1) {
          colorsList.add(type.getBackgroundColor());
        }
        colorsList.add(type.getBackgroundColor());
      }
      return colorsList;
    }
  }

  public static void buildGroups(@Nonnull MultiMap<VcsRefType, VcsRef> groupedRefs,
                                 boolean compact,
                                 boolean showTagNames,
                                 @Nonnull List<RefGroup> result) {
    if (groupedRefs.isEmpty()) return;

    if (compact) {
      VcsRef firstRef = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(groupedRefs.values()));
      RefGroup group = ContainerUtil.getFirstItem(result);
      if (group == null) {
        result.add(new SimpleRefGroup(firstRef.getType().isBranch() || showTagNames ? firstRef.getName() : "",
                                      ContainerUtil.newArrayList(groupedRefs.values())));
      }
      else {
        group.getRefs().addAll(groupedRefs.values());
      }
    }
    else {
      for (Map.Entry<VcsRefType, Collection<VcsRef>> entry : groupedRefs.entrySet()) {
        if (entry.getKey().isBranch()) {
          for (VcsRef ref : entry.getValue()) {
            result.add(new SimpleRefGroup(ref.getName(), ContainerUtil.newArrayList(ref)));
          }
        }
        else {
          result.add(new SimpleRefGroup(showTagNames ? ObjectUtil.notNull(ContainerUtil.getFirstItem(entry.getValue())).getName() : "",
                                        ContainerUtil.newArrayList(entry.getValue())));
        }
      }
    }
  }
}
