/*
 * Copyright 2013-2023 consulo.io
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
package consulo.compiler.impl.internal;

import consulo.compiler.Compiler;
import consulo.compiler.TranslatingCompiler;
import consulo.compiler.util.ModuleCompilerUtil;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.component.util.graph.Graph;
import consulo.component.util.graph.GraphGenerator;
import consulo.component.util.graph.InboundSemiGraph;
import consulo.project.Project;
import consulo.util.collection.Chunk;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 17/04/2023
 */
public class CompilerExtensionCache {
  private static final ExtensionPointCacheKey<Compiler, CompilerExtensionCache> KEY =
    ExtensionPointCacheKey.create("CompilerExtensionCache", CompilerExtensionCache::new);

  @Nonnull
  public static CompilerExtensionCache get(Project project) {
    return project.getExtensionPoint(Compiler.class).getOrBuildCache(KEY);
  }

  private final List<TranslatingCompiler> myTranslatingCompilers = new ArrayList<>();
  private final List<Compiler> myCompilers = new ArrayList<>();
  private final Map<TranslatingCompiler, Collection<FileType>> myTranslatingCompilerInputFileTypes = new HashMap<>();
  private final Map<TranslatingCompiler, Collection<FileType>> myTranslatingCompilerOutputFileTypes = new HashMap<>();
  private final Set<FileType> myCompilableFileTypes = new HashSet<>();

  public CompilerExtensionCache(ExtensionWalker<Compiler> walker) {
    List<TranslatingCompiler> translatingCompilers = new ArrayList<>();
    walker.walk(compiler -> {
      compiler.registerCompilableFileTypes(myCompilableFileTypes::add);

      if (compiler instanceof TranslatingCompiler) {
        TranslatingCompiler translatingCompiler = (TranslatingCompiler)compiler;

        translatingCompilers.add(translatingCompiler);

        myTranslatingCompilerInputFileTypes.put(translatingCompiler, Arrays.asList(translatingCompiler.getInputFileTypes()));
        myTranslatingCompilerOutputFileTypes.put(translatingCompiler, Arrays.asList(translatingCompiler.getOutputFileTypes()));
      }
      else {
        myCompilers.add(compiler);
      }
    });

    final List<Chunk<TranslatingCompiler>> chunks = ModuleCompilerUtil.getSortedChunks(createCompilerGraph(translatingCompilers));

    for (Chunk<TranslatingCompiler> chunk : chunks) {
      myTranslatingCompilers.addAll(chunk.getNodes());
    }
  }

  public boolean isCompilableFileType(@Nonnull FileType type) {
    return myCompilableFileTypes.contains(type);
  }

  private Graph<TranslatingCompiler> createCompilerGraph(final List<TranslatingCompiler> compilers) {
    return GraphGenerator.generate(new InboundSemiGraph<TranslatingCompiler>() {
      @Override
      public Collection<TranslatingCompiler> getNodes() {
        return compilers;
      }

      @Override
      public Iterator<TranslatingCompiler> getIn(TranslatingCompiler compiler) {
        final Collection<FileType> compilerInput = myTranslatingCompilerInputFileTypes.get(compiler);
        if (compilerInput == null || compilerInput.isEmpty()) {
          return Collections.<TranslatingCompiler>emptySet().iterator();
        }

        final Set<TranslatingCompiler> inCompilers = new HashSet<>();

        for (Map.Entry<TranslatingCompiler, Collection<FileType>> entry : myTranslatingCompilerOutputFileTypes.entrySet()) {
          final Collection<FileType> outputs = entry.getValue();
          TranslatingCompiler comp = entry.getKey();
          if (outputs != null && ContainerUtil.intersects(compilerInput, outputs)) {
            inCompilers.add(comp);
          }
        }
        return inCompilers.iterator();
      }
    });
  }

  @Nonnull
  public Collection<FileType> getRegisteredInputTypes(@Nonnull TranslatingCompiler compiler) {
    final Collection<FileType> fileTypes = myTranslatingCompilerInputFileTypes.get(compiler);
    return fileTypes == null ? Collections.<FileType>emptyList() : fileTypes;
  }

  @Nonnull
  public Collection<FileType> getRegisteredOutputTypes(@Nonnull TranslatingCompiler compiler) {
    final Collection<FileType> fileTypes = myTranslatingCompilerOutputFileTypes.get(compiler);
    return fileTypes == null ? Collections.<FileType>emptyList() : fileTypes;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends Compiler> T[] getCompilers(@Nonnull Class<T> compilerClass, Predicate<Compiler> filter) {
    final List<T> compilers = new ArrayList<>(myCompilers.size());
    for (final Compiler item : myCompilers) {
      if (compilerClass.isAssignableFrom(item.getClass()) && filter.test(item)) {
        compilers.add((T)item);
      }
    }
    for (final Compiler item : myTranslatingCompilers) {
      if (compilerClass.isAssignableFrom(item.getClass()) && filter.test(item)) {
        compilers.add((T)item);
      }
    }
    final T[] array = (T[])Array.newInstance(compilerClass, compilers.size());
    return compilers.toArray(array);
  }
}
