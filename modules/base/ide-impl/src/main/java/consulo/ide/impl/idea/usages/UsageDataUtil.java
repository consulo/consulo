/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.usages;

import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.usage.Usage;
import consulo.usage.UsageTarget;
import consulo.virtualFileSystem.VirtualFile;
import consulo.usage.rule.UsageInFile;
import consulo.usage.rule.UsageInFiles;
import consulo.ide.impl.idea.util.containers.ContainerUtil;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author cdr
 */
public class UsageDataUtil {
  @Nullable
  public static VirtualFile[] provideVirtualFileArray(Usage[] usages, UsageTarget[] usageTargets) {
    if (usages == null && usageTargets == null) {
      return null;
    }

    final Set<VirtualFile> result = new HashSet<VirtualFile>();

    if (usages != null) {
      for (Usage usage : usages) {
        if (usage instanceof UsageInFile) {
          VirtualFile file = ((UsageInFile)usage).getFile();
          if (file != null && file.isValid()) {
            result.add(file);
          }
        }

        if (usage instanceof UsageInFiles) {
          VirtualFile[] files = ((UsageInFiles)usage).getFiles();
          for (VirtualFile file : files) {
            if (file.isValid()) {
              result.add(file);
            }
          }
        }
      }
    }

    if (usageTargets != null) {
      for (UsageTarget usageTarget : usageTargets) {
        if (usageTarget.isValid()) {
          final VirtualFile[] files = usageTarget.getFiles();
          if (files != null) {
            ContainerUtil.addAll(result, files);
          }
        }
      }
    }

    return VfsUtilCore.toVirtualFileArray(result);
  }
}
