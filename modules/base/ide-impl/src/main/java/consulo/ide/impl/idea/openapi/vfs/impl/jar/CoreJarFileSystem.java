/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vfs.impl.jar;

import consulo.ide.impl.idea.openapi.util.Couple;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.util.ConcurrentFactoryMap;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author yole
 */
@Deprecated
public class CoreJarFileSystem extends BaseVirtualFileSystem {
  private final Map<String, CoreJarHandler> myHandlers = ConcurrentFactoryMap.createMap(key -> new CoreJarHandler(CoreJarFileSystem.this, key));
  @Nonnull
  @Override
  public String getProtocol() {
    return StandardFileSystems.JAR_PROTOCOL;
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull @NonNls String path) {
    Couple<String> pair = splitPath(path);
    return myHandlers.get(pair.first).findFileByPath(pair.second);
  }

  @Nonnull
  protected Couple<String> splitPath(@Nonnull String path) {
    int separator = path.indexOf("!/");
    if (separator < 0) {
      throw new IllegalArgumentException("Path in JarFileSystem must contain a separator: " + path);
    }
    String localPath = path.substring(0, separator);
    String pathInJar = path.substring(separator + 2);
    return Couple.of(localPath, pathInJar);
  }

  @Override
  public void refresh(boolean asynchronous) { }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return findFileByPath(path);
  }

  @SuppressWarnings("unused")  // used in Kotlin
  public void clearHandlersCache() {
    myHandlers.clear();
  }
}
