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
package consulo.ide.impl.idea.ide.impl;

import consulo.application.ApplicationManager;
import consulo.content.ContentIterator;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author max
 */
public class PatchProjectUtil {
  private PatchProjectUtil() {
  }

  public static void patchProject(final Project project) {
    final Map<Pattern, Set<Pattern>> excludePatterns = loadPatterns("idea.exclude.patterns");
    final Map<Pattern, Set<Pattern>> includePatterns = loadPatterns("idea.include.patterns");

    if (excludePatterns.isEmpty() && includePatterns.isEmpty()) return;
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    final ModifiableModuleModel modulesModel = ModuleManager.getInstance(project).getModifiableModel();
    final Module[] modules = modulesModel.getModules();
    final ModifiableRootModel[] models = new ModifiableRootModel[modules.length];
    for (int i = 0; i < modules.length; i++) {
      models[i] = ModuleRootManager.getInstance(modules[i]).getModifiableModel();
      final int idx = i;
      final ContentEntry[] contentEntries = models[i].getContentEntries();
      for (final ContentEntry contentEntry : contentEntries) {
        final VirtualFile contentRoot = contentEntry.getFile();
        if (contentRoot == null) continue;
        final Set<VirtualFile> included = new HashSet<VirtualFile>();
        iterate(contentRoot, new ContentIterator() {
          @Override
          public boolean processFile(final VirtualFile fileOrDir) {
            String relativeName = VfsUtilCore.getRelativePath(fileOrDir, contentRoot, '/');
            for (Pattern module : excludePatterns.keySet()) {
              if (module == null || module.matcher(modules[idx].getName()).matches()) {
                final Set<Pattern> dirPatterns = excludePatterns.get(module);
                for (Pattern pattern : dirPatterns) {
                  if (pattern.matcher(relativeName).matches()) {
                    contentEntry.addFolder(fileOrDir, ExcludedContentFolderTypeProvider.getInstance());
                    return false;
                  }
                }
              }
            }
            if (includePatterns.isEmpty()) return true;
            for (Pattern module : includePatterns.keySet()) {
              if (module == null || module.matcher(modules[idx].getName()).matches()) {
                final Set<Pattern> dirPatterns = includePatterns.get(module);
                for (Pattern pattern : dirPatterns) {
                  if (pattern.matcher(relativeName).matches()) {
                    included.add(fileOrDir);
                    return true;
                  }
                }
              }
            }
            return true;
          }
        }, index);
        processIncluded(contentEntry, included);
      }
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        ModifiableModelCommitter.getInstance(project).multiCommit(models, modulesModel);
      }
    });
  }

  public static void processIncluded(final ContentEntry contentEntry, final Set<VirtualFile> included) {
    if (included.isEmpty()) return;
    final Set<VirtualFile> parents = new HashSet<VirtualFile>();
    for (VirtualFile file : included) {
      if (Comparing.equal(file, contentEntry.getFile())) return;
      final VirtualFile parent = file.getParent();
      if (parent == null || parents.contains(parent)) continue;
      parents.add(parent);
      for (VirtualFile toExclude : parent.getChildren()) {  // if it will ever dead-loop on symlink blame anna.kozlova
        boolean toExcludeSibling = true;
        for (VirtualFile includeRoot : included) {
          if (VfsUtilCore.isAncestor(toExclude, includeRoot, false)) {
            toExcludeSibling = false;
          }
        }
        if (toExcludeSibling) {
          contentEntry.addFolder(toExclude, ExcludedContentFolderTypeProvider.getInstance());
        }
      }
    }
    processIncluded(contentEntry, parents);
  }

  public static void iterate(VirtualFile contentRoot, final ContentIterator iterator, final ProjectFileIndex idx) {
    VfsUtilCore.visitChildrenRecursively(contentRoot, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!iterator.processFile(file)) return false;
        if (idx.getModuleForFile(file) == null) return false;  // already excluded
        return true;
      }
    });
  }

  public static Map<Pattern, Set<Pattern>> loadPatterns(@NonNls String propertyKey) {
    final Map<Pattern, Set<Pattern>> result = new HashMap<Pattern, Set<Pattern>>();
    final String patterns = System.getProperty(propertyKey);
    if (patterns != null) {
      final String[] pathPatterns = patterns.split(";");
      for (String excludedPattern : pathPatterns) {
        String module = null;
        int idx = 0;
        if (excludedPattern.startsWith("[")) {
          idx = excludedPattern.indexOf("]") + 1;
          module = excludedPattern.substring(1, idx - 1);
        }
        final Pattern modulePattern = module != null ? Pattern.compile(StringUtil.replace(module, "*", ".*")) : null;
        final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(excludedPattern.substring(idx)));
        Set<Pattern> dirPatterns = result.get(modulePattern);
        if (dirPatterns == null) {
          dirPatterns = new HashSet<Pattern>();
          result.put(modulePattern, dirPatterns);
        }
        dirPatterns.add(pattern);
      }
    }
    return result;
  }
}