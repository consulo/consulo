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

package consulo.compiler.artifact.element;

import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author nik
 */
public class ArchivePackageInfo {
  private final List<Pair<String, VirtualFile>> myPackedFiles;
  private final LinkedHashSet<Pair<String, ArchivePackageInfo>> myPackedArchives;
  private final List<DestinationInfo> myDestinations;
  private final ArchivePackageWriter<?> myPackageWriter;

  public ArchivePackageInfo(ArchivePackageWriter<?> packageWriter) {
    myPackageWriter = packageWriter;
    myDestinations = new ArrayList<>();
    myPackedFiles = new ArrayList<>();
    myPackedArchives = new LinkedHashSet<>();
  }

  public void addDestination(DestinationInfo info) {
    myDestinations.add(info);
    if (info instanceof ArchiveDestinationInfo) {
      ArchiveDestinationInfo destinationInfo = (ArchiveDestinationInfo)info;
      destinationInfo.getArchivePackageInfo().myPackedArchives.add(Pair.create(destinationInfo.getPathInJar(), this));
    }
  }

  public void addContent(String pathInJar, VirtualFile sourceFile) {
    myPackedFiles.add(Pair.create(pathInJar, sourceFile));
  }

  public List<Pair<String, VirtualFile>> getPackedFiles() {
    return myPackedFiles;
  }

  public LinkedHashSet<Pair<String, ArchivePackageInfo>> getPackedArchives() {
    return myPackedArchives;
  }

  @Nonnull
  public ArchivePackageWriter<?> getPackageWriter() {
    return myPackageWriter;
  }

  public List<ArchiveDestinationInfo> getArchiveDestinations() {
    final ArrayList<ArchiveDestinationInfo> list = new ArrayList<>();
    for (DestinationInfo destination : myDestinations) {
      if (destination instanceof ArchiveDestinationInfo) {
        list.add((ArchiveDestinationInfo)destination);
      }
    }
    return list;
  }

  public List<DestinationInfo> getAllDestinations() {
    return myDestinations;
  }

  public String getPresentableDestination() {
    return !myDestinations.isEmpty() ? myDestinations.get(0).getOutputPath() : "";
  }
}
