/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.archive.ArchiveHandler;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ConcurrentHashSet;
import com.intellij.util.messages.MessageBus;
import org.consulo.vfs.ArchiveFileSystemBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

public class JarFileSystemImpl extends ArchiveFileSystemBase implements JarFileSystem, ApplicationComponent {
  private final Set<String> myNoCopyJarPaths =
    SystemProperties.getBooleanProperty("idea.jars.nocopy", !SystemInfo.isWindows) ? null : new ConcurrentHashSet<String>(FileUtil.PATH_HASHING_STRATEGY);

  private File myNoCopyJarDir;

  public JarFileSystemImpl(MessageBus bus) {
    super(bus);
  }

  @Override
  public ArchiveHandler createHandler(ArchiveFileSystem fileSystem, String path) {
    return new JarHandler(fileSystem, path);
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "JarFileSystem";
  }

  @Override
  public void initComponent() {
    // we want to prevent Platform from copying its own jars when running from dist to save system resources
    final boolean isRunningFromDist = new File(PathManager.getLibPath() + File.separatorChar + "idea.jar").exists();
    if (isRunningFromDist) {
      myNoCopyJarDir = new File(new File(PathManager.getLibPath()).getParent());
    }
  }

  @Override
  public void disposeComponent() {
  }

  @Override
  public void setNoCopyJarForPath(String pathInJar) {
    if (myNoCopyJarPaths == null || pathInJar == null) {
      return;
    }
    int index = pathInJar.indexOf(ARCHIVE_SEPARATOR);
    if (index < 0) return;
    String path = pathInJar.substring(0, index);
    path = path.replace('/', File.separatorChar);
    myNoCopyJarPaths.add(path);
  }

  @Nullable
  public File getMirroredFile(@NotNull VirtualFile vFile) {
    VirtualFile jar = findByPathWithSeparator(vFile);
    final ArchiveHandler handler = jar != null ? getHandler(jar) : null;
    return handler != null ? handler.getMirrorFile(new File(vFile.getPath())) : null;
  }

  @Override
  public boolean isMakeCopyOfJar(@NotNull File originalJar) {
    if (myNoCopyJarPaths == null || myNoCopyJarPaths.contains(originalJar.getPath())) return false;
    if (myNoCopyJarDir != null && FileUtil.isAncestor(myNoCopyJarDir, originalJar, false)) return false;
    return true;
  }

  @Override
  @NotNull
  public String getProtocol() {
    return PROTOCOL;
  }
}
