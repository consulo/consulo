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
package consulo.component.impl.internal.macro;

import consulo.application.Application;
import consulo.application.macro.PathMacros;
import consulo.application.util.SystemInfo;
import consulo.component.macro.*;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import org.jdom.Element;

import javax.annotation.Nullable;
import java.util.Map;

public class BasePathMacroManager implements PathMacroManager {
  private PathMacros myPathMacros;

  public BasePathMacroManager(@Nullable PathMacros pathMacros) {
    myPathMacros = pathMacros;
  }

  protected static void addFileHierarchyReplacements(ExpandMacroToPathMap result, String macroName, @Nullable String path) {
    if (path == null) return;
    addFileHierarchyReplacements(result, getLocalFileSystem().findFileByPath(path), "$" + macroName + "$");
  }

  private static void addFileHierarchyReplacements(ExpandMacroToPathMap result, @Nullable VirtualFile f, String macro) {
    if (f == null) return;
    addFileHierarchyReplacements(result, f.getParent(), macro + "/..");
    result.put(macro, StringUtil.trimEnd(f.getPath(), "/"));
  }

  protected static void addFileHierarchyReplacements(ReplacePathToMacroMap result, String macroName, @Nullable String path, @Nullable String stopAt) {
    if (path == null) return;

    PathMacroProtocolProvider pathMacroProtocolProvider = Application.get().getInstance(PathMacroProtocolProvider.class);

    String macro = "$" + macroName + "$";
    path = StringUtil.trimEnd(FileUtil.toSystemIndependentName(path), "/");
    boolean overwrite = true;
    while (StringUtil.isNotEmpty(path) && path.contains("/")) {
      result.addReplacement(pathMacroProtocolProvider, path, macro, overwrite);

      if (path.equals(stopAt)) {
        break;
      }

      macro += "/..";
      overwrite = false;
      path = StringUtil.getPackageName(path, '/');
    }
  }

  private static VirtualFileSystem getLocalFileSystem() {
    // Use VFM directly because of mocks in tests.
    return VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
  }

  public ExpandMacroToPathMap getExpandMacroMap() {
    ExpandMacroToPathMap result = new ExpandMacroToPathMap();
    for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
      result.addMacroExpand(entry.getKey(), entry.getValue());
    }
    getPathMacros().addMacroExpands(result);
    return result;
  }

  public ReplacePathToMacroMap getReplacePathMap() {
    PathMacroProtocolProvider pathMacroProtocolProvider = Application.get().getInstance(PathMacroProtocolProvider.class);

    ReplacePathToMacroMap result = new ReplacePathToMacroMap();
    for (Map.Entry<String, String> entry : PathMacroUtil.getGlobalSystemMacros().entrySet()) {
      result.addMacroReplacement(pathMacroProtocolProvider, entry.getValue(), entry.getKey());
    }
    getPathMacros().addMacroReplacements(result);
    return result;
  }

  @Override
  public String expandPath(final String path) {
    return getExpandMacroMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public String collapsePath(final String path) {
    return getReplacePathMap().substitute(path, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void collapsePathsRecursively(final Element element) {
    getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive, true);
  }

  @Override
  public String collapsePathsRecursively(final String text) {
    return getReplacePathMap().substituteRecursively(text, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void expandPaths(final Element element) {
    getExpandMacroMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  @Override
  public void collapsePaths(final Element element) {
    getReplacePathMap().substitute(element, SystemInfo.isFileSystemCaseSensitive);
  }

  public PathMacros getPathMacros() {
    if (myPathMacros == null) {
      myPathMacros = PathMacros.getInstance();
    }

    return myPathMacros;
  }

  protected static boolean pathsEqual(@Nullable String path1, @Nullable String path2) {
    return path1 != null && path2 != null &&
           FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path1), FileUtil.toSystemIndependentName(path2));
  }
}
