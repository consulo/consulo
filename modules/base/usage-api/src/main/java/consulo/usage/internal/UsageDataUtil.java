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
package consulo.usage.internal;

import consulo.usage.Usage;
import consulo.usage.UsageTarget;
import consulo.usage.rule.UsageInFile;
import consulo.usage.rule.UsageInFiles;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
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

        Set<VirtualFile> result = new HashSet<>();

        if (usages != null) {
            for (Usage usage : usages) {
                if (usage instanceof UsageInFile) {
                    VirtualFile file = ((UsageInFile) usage).getFile();
                    if (file != null && file.isValid()) {
                        result.add(file);
                    }
                }

                if (usage instanceof UsageInFiles) {
                    VirtualFile[] files = ((UsageInFiles) usage).getFiles();
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
                    VirtualFile[] files = usageTarget.getFiles();
                    if (files != null) {
                        ContainerUtil.addAll(result, files);
                    }
                }
            }
        }

        return VirtualFileUtil.toVirtualFileArray(result);
    }
}
