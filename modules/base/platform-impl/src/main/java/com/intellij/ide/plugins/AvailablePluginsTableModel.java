/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 19-Aug-2006
 * Time: 14:54:29
 */
package com.intellij.ide.plugins;

import com.intellij.util.ui.ColumnInfo;
import consulo.annotation.DeprecationInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.RepositoryTagLocalize;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.*;

/**
 * @author stathik
 */
public class AvailablePluginsTableModel extends PluginTableModel {
  private Pair<String, LocalizeValue> myTargetTagInfo = getUnspecifiedTagInfo();

  private final Map<String, LocalizeValue> myAvailableTags = new TreeMap<>();

  public AvailablePluginsTableModel() {
    super.columns = new ColumnInfo[]{new AvailablePluginColumnInfo(this)};

    setSortKey(new RowSorter.SortKey(getNameColumn(), SortOrder.ASCENDING));
    view = new ArrayList<>();
  }

  @Nonnull
  public Pair<String, LocalizeValue> getTargetTagInfo() {
    return myTargetTagInfo;
  }

  @Nonnull
  public static Pair<String, LocalizeValue> getUnspecifiedTagInfo() {
    return Pair.create("", RepositoryTagLocalize.all());
  }

  public void setTargetTag(String tag, LocalizeValue tagValue, String filter) {
    myTargetTagInfo = Pair.create(tag, tagValue);

    filter(filter);
  }

  @Override
  public boolean isPluginDescriptorAccepted(PluginDescriptor descriptor) {
    String tag = myTargetTagInfo.getFirst();

    if (!StringUtil.isEmpty(tag)) {
      if (!getHackyTags(descriptor).contains(tag)) {
        return false;
      }
    }
    return true;
  }

  @Deprecated
  @DeprecationInfo("Migrate to #getTags()")
  private static Set<String> getHackyTags(PluginDescriptor descriptor) {
    Set<String> tags = descriptor.getTags();
    if(!tags.isEmpty()) {
      return tags;
    }
    return Set.of(descriptor.getCategory());
  }

  @Nonnull
  public Map<String, LocalizeValue> getAvailableTags() {
    return myAvailableTags;
  }

  private static void updateStatus(final PluginDescriptor descr) {
    if (descr instanceof PluginNode) {
      final PluginNode node = (PluginNode)descr;
      PluginDescriptor existing = PluginManager.findPlugin(descr.getPluginId());
      if (existing != null) {
        node.setStatus(PluginNode.STATUS_INSTALLED);
        node.setInstalledVersion(existing.getVersion());
      }
    }
  }

  @Override
  public void updatePluginsList(List<PluginDescriptor> list) {
    view.clear();
    myAvailableTags.clear();
    filtered.clear();

    //  For each downloadable plugin we need to know whether its counterpart
    //  is already installed, and if yes compare the difference in versions:
    //  availability of newer versions will be indicated separately.
    for (PluginDescriptor descr : list) {
      updateStatus(descr);
      view.add(descr);

      Set<String> tags = descr.getTags();
      if (!tags.isEmpty()) {
        for (String tag : tags) {
          myAvailableTags.put(tag, PluginManagerMain.getTagLocalizeValue(tag));
        }
      } else {
        String category = descr.getCategory();
        myAvailableTags.put(category, LocalizeValue.of(category));
      }
    }

    fireTableDataChanged();
  }

  @Override
  public void filter(final List<PluginDescriptor> filtered) {
    view.clear();
    for (PluginDescriptor descriptor : filtered) {
      view.add(descriptor);
    }
    super.filter(filtered);
  }

  @Override
  public int getNameColumn() {
    return 0;
  }
}
