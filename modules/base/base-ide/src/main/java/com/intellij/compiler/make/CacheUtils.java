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
package com.intellij.compiler.make;

import com.intellij.compiler.impl.ExitException;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import consulo.compiler.make.DependencyCache;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * Date: Aug 18, 2003
 * Time: 6:32:32 PM
 */
public class CacheUtils {
  private static final Logger LOG = Logger.getInstance(CacheUtils.class);

  public static String getMethodSignature(String name, String descriptor) {
    final StringBuilder builder = new StringBuilder();
    builder.append(name);
    builder.append(descriptor.substring(0, descriptor.indexOf(')') + 1));
    return builder.toString();
  }

  public static boolean areArraysContentsEqual(int[] exceptions1, int[] exceptions2) {
    if (exceptions1.length != exceptions2.length) {
      return false;
    }
    if (exceptions1.length != 0) { // optimization
      IntSet exceptionsSet = IntSets.newHashSet(exceptions1);
      for (int exception : exceptions2) {
        if (!exceptionsSet.contains(exception)) {
          return false;
        }
      }
    }
    return true;
  }

  public static Collection<VirtualFile> findDependentFiles(
    final CompileContextEx context,
    final Set<VirtualFile> compiledWithErrors,
    final @javax.annotation.Nullable Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> filter)
    throws CacheCorruptedException, ExitException {

    context.getProgressIndicator().setText(CompilerBundle.message("progress.checking.dependencies"));

    final DependencyCache dependencyCache = context.getDependencyCache();
    final Set<VirtualFile> dependentFiles = new HashSet<VirtualFile>();

    dependencyCache.findDependentFiles(context, new Ref<CacheCorruptedException>(), filter, dependentFiles, compiledWithErrors);

    context.getProgressIndicator().setText(
      dependentFiles.size() > 0? CompilerBundle.message("progress.found.dependent.files", dependentFiles.size()) : ""
    );

    return dependentFiles;
  }

  @Nonnull
  public static Set<VirtualFile> getFilesCompiledWithErrors(final CompileContextEx context) {
    CompilerMessage[] messages = context.getMessages(CompilerMessageCategory.ERROR);
    Set<VirtualFile> compiledWithErrors = Collections.emptySet();
    if (messages.length > 0) {
      compiledWithErrors = new HashSet<VirtualFile>(messages.length);
      for (CompilerMessage message : messages) {
        final VirtualFile file = message.getVirtualFile();
        if (file != null) {
          compiledWithErrors.add(file);
        }
      }
    }
    return compiledWithErrors;
  }
}
