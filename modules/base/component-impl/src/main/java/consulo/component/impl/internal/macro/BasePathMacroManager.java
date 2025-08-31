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
import consulo.component.macro.*;
import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import jakarta.annotation.Nullable;
import org.jdom.Element;

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
  public String expandPath(String path) {
    return getExpandMacroMap().substitute(path, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public String collapsePath(String path) {
    return getReplacePathMap().substitute(path, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public void collapsePathsRecursively(Element element) {
    getReplacePathMap().substitute(element, Platform.current().fs().isCaseSensitive(), true);
  }

  @Override
  public String collapsePathsRecursively(String text) {
    return getReplacePathMap().substituteRecursively(text, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public void expandPaths(Element element) {
    getExpandMacroMap().substitute(element, Platform.current().fs().isCaseSensitive());
  }

  @Override
  public void collapsePaths(Element element) {
    getReplacePathMap().substitute(element, Platform.current().fs().isCaseSensitive());
  }

  public PathMacros getPathMacros() {
    if (myPathMacros == null) {
      myPathMacros = PathMacros.getInstance();
    }

    return myPathMacros;
  }

  protected static boolean pathsEqual(@Nullable String path1, @Nullable String path2) {
    return path1 != null && path2 != null
      && FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path1), FileUtil.toSystemIndependentName(path2));
  }
}
