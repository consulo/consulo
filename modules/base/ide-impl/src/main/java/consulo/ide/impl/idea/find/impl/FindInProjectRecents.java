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
package consulo.ide.impl.idea.find.impl;

import consulo.find.FindInProjectSettings;
import consulo.find.FindSettings;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.ide.impl.idea.util.ArrayUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

@Singleton
@State(
        name = "FindInProjectRecents",
        storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)})
public final class FindInProjectRecents extends FindInProjectSettingsBase implements FindInProjectSettings {
  public static FindInProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, FindInProjectSettings.class);
  }

  @Override
  @Nonnull
  public List<String> getRecentDirectories() {
    ArrayList<String> strings = new ArrayList<>(FindSettings.getInstance().getRecentDirectories());
    strings.addAll(super.getRecentDirectories());
    return strings;
  }

  @Override
  @Nonnull
  public String[] getRecentFindStrings() {
    return ArrayUtil.mergeArrays(FindSettings.getInstance().getRecentFindStrings(), super.getRecentFindStrings());
  }

  @Override
  @Nonnull
  public String[] getRecentReplaceStrings() {
    return ArrayUtil.mergeArrays(FindSettings.getInstance().getRecentReplaceStrings(), super.getRecentReplaceStrings());
  }
}
