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

import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;

/**
 * @author nik
 */
public class ArchiveDestinationInfo extends DestinationInfo {
  private static final Logger LOG = Logger.getInstance(ArchiveDestinationInfo.class);

  private final String myPathInJar;
  private final ArchivePackageInfo myArchivePackageInfo;

  public ArchiveDestinationInfo(final String pathInJar, final ArchivePackageInfo archivePackageInfo, DestinationInfo jarDestination) {
    super(appendPathInJar(jarDestination.getOutputPath(), pathInJar), jarDestination.getOutputFile(), jarDestination.getOutputFilePath());
    LOG.assertTrue(!pathInJar.startsWith(".."), pathInJar);
    myPathInJar = StringUtil.startsWithChar(pathInJar, '/') ? pathInJar : "/" + pathInJar;
    myArchivePackageInfo = archivePackageInfo;
  }

  private static String appendPathInJar(String outputPath, String pathInJar) {
    LOG.assertTrue(outputPath.length() > 0 && outputPath.charAt(outputPath.length() - 1) != '/');
    LOG.assertTrue(pathInJar.length() > 0 && pathInJar.charAt(0) != '/');
    return outputPath + ArchiveFileSystem.ARCHIVE_SEPARATOR + pathInJar;
  }

  public String getPathInJar() {
    return myPathInJar;
  }

  public ArchivePackageInfo getArchivePackageInfo() {
    return myArchivePackageInfo;
  }

  @Override
  public String toString() {
    return myPathInJar + "(" + getOutputPath() + ")";
  }
}
