/*
 * Copyright 2013-2016 consulo.io
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
package consulo.execution.debug.breakpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 5/7/2016
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XLineBreakpointTypeResolver {
  ExtensionPointCacheKey<XLineBreakpointTypeResolver, Map<FileType, List<XLineBreakpointTypeResolver>>> KEY = ExtensionPointCacheKey.create("XLineBreakpointTypeResolver", extensions -> {
    Map<FileType, List<XLineBreakpointTypeResolver>> map = new HashMap<>();
    for (XLineBreakpointTypeResolver extension : extensions) {
      map.computeIfAbsent(extension.getFileType(), fileType -> new ArrayList<>()).add(extension);
    }
    return map;
  });

  @Nonnull
  static List<XLineBreakpointTypeResolver> forFileType(FileType fileType) {
    ExtensionPoint<XLineBreakpointTypeResolver> extensionPoint = Application.get().getExtensionPoint(XLineBreakpointTypeResolver.class);
    Map<FileType, List<XLineBreakpointTypeResolver>> map = extensionPoint.getOrBuildCache(KEY);
    return map.getOrDefault(fileType, List.of());
  }

  @Nullable
  @RequiredReadAction
  static XLineBreakpointType<?> forFile(@Nonnull Project project, @Nonnull VirtualFile virtualFile, int line) {
    for (XLineBreakpointTypeResolver resolver : forFileType(virtualFile.getFileType())) {
      XLineBreakpointType<?> breakpointType = resolver.resolveBreakpointType(project, virtualFile, line);
      if (breakpointType != null) {
        return breakpointType;
      }
    }
    return null;
  }

  @Nullable
  @RequiredReadAction
  XLineBreakpointType<?> resolveBreakpointType(@Nonnull Project project, @Nonnull VirtualFile virtualFile, int line);

  @Nonnull
  FileType getFileType();
}
