/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.compiler.impl.packagingCompiler;

import com.intellij.openapi.util.Pair;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author nik
 */
public class DependentArchivesEvaluator {
  private final Set<ArchivePackageInfo> myArchivePackageInfos = new LinkedHashSet<ArchivePackageInfo>();

  public void addArchiveWithDependencies(final ArchivePackageInfo archivePackageInfo) {
    if (myArchivePackageInfos.add(archivePackageInfo)) {
      for (ArchiveDestinationInfo destination : archivePackageInfo.getArchiveDestinations()) {
        addArchiveWithDependencies(destination.getArchivePackageInfo());
      }
      for (Pair<String, ArchivePackageInfo> pair : archivePackageInfo.getPackedArchives()) {
        addArchiveWithDependencies(pair.getSecond());
      }
    }
  }

  public Set<ArchivePackageInfo> getArchivePackageInfos() {
    return myArchivePackageInfos;
  }
}
