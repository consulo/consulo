/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.devkit.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CopyingCompiler;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.build.PluginBuildUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15:26/14.06.13
 */
public class CopyArtifactCompiler extends CopyingCompiler {
  public static final Key<Module> TARGET_MODULE = Key.create("copy-target-module");

  @NotNull
  @Override
  public String getDescription() {
    return "Copy Artifacts";
  }

  @Override
  protected boolean isDirectoryCopying() {
    return true;
  }

  @Override
  public VirtualFile[] getFilesToCopy(CompileContext context) {
    final Artifact[] artifacts = ArtifactCompileScope.getArtifacts(context.getCompileScope());
    if (artifacts == null) {
      return VirtualFile.EMPTY_ARRAY;
    }
    final Module module = TARGET_MODULE.get(context.getCompileScope());
    if (module == null) {
      return VirtualFile.EMPTY_ARRAY;
    }

    List<VirtualFile> items = new ArrayList<VirtualFile>(artifacts.length);
    for (Artifact artifact : artifacts) {
      final VirtualFile outputFile = artifact.getOutputFile();
      if (outputFile == null) {
        continue;
      }
      items.add(outputFile);
    }

    return VfsUtilCore.toVirtualFileArray(items);
  }

  @Override
  public String getDestinationPath(CompileContext context, VirtualFile sourceFile) {

    final Module module = TARGET_MODULE.get(context.getCompileScope());

    assert module != null;

    final String pluginExPath = PluginBuildUtil.getPluginExPath(module);

    assert pluginExPath != null;

    return pluginExPath;
  }
}
