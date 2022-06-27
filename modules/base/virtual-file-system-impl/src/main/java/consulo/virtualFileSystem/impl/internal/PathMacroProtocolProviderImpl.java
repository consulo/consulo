/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.macro.PathMacroProtocolProvider;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.VirtualFileSystemWithMacroSupport;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 27-Jun-22
 */
@ServiceImpl
public class PathMacroProtocolProviderImpl implements PathMacroProtocolProvider {
  private static final ExtensionPointCacheKey<VirtualFileSystem, List<String>> PROTOCOLS = ExtensionPointCacheKey.create("vfsMacroProtocols", virtualFileSystems -> {
    List<String> protocols = new ArrayList<>();
    for (VirtualFileSystem fileSystem : virtualFileSystems) {
      if (fileSystem instanceof VirtualFileSystemWithMacroSupport) {
        protocols.add(fileSystem.getProtocol());
      }
    }
    return protocols;
  });

  private final Application myApplication;

  @Inject
  public PathMacroProtocolProviderImpl(Application application) {
    myApplication = application;
  }

  @Nonnull
  @Override
  public List<String> getSupportedProtocols() {
    return myApplication.getExtensionPoint(VirtualFileSystem.class).getOrBuildCache(PROTOCOLS);
  }
}
