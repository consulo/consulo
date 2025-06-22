/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.task;

import consulo.util.collection.ContainerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.versionControlSystem.VcsTaskHandler;

import java.util.List;

/**
 * @author Dmitry Avdeev
 * @since 2013-07-18
 */
@Tag("branch")
public class BranchInfo {
  @Attribute("name")
  public String name;

  @Attribute("repository")
  public String repository;

  @Attribute("original")
  public boolean original;

  public static List<BranchInfo> fromTaskInfo(VcsTaskHandler.TaskInfo taskInfo, boolean original) {
    return ContainerUtil.map(taskInfo.getRepositories(), s -> {
      BranchInfo info = new BranchInfo();
      info.name = taskInfo.getName();
      info.repository = s;
      info.original = original;
      return info;
    });
  }
}
