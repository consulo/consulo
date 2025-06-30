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

/**
 * created at Jan 17, 2002
 *
 * @author Jeka
 */
package consulo.compiler.util;

import consulo.compiler.CompileContext;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

public class MakeUtil {
    public static VirtualFile getSourceRoot(CompileContext context, Module module, VirtualFile file) {
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
        final VirtualFile root = fileIndex.getSourceRootForFile(file);
        if (root != null) {
            return root;
        }
        // try to find among roots of generated files.
        final VirtualFile[] sourceRoots = context.getSourceRoots(module);
        for (final VirtualFile sourceRoot : sourceRoots) {
            if (fileIndex.isInSourceContent(sourceRoot)) {
                continue; // skip content source roots, need only roots for generated files
            }
            if (VirtualFileUtil.isAncestor(sourceRoot, file, false)) {
                return sourceRoot;
            }
        }
        return null;
    }
}
