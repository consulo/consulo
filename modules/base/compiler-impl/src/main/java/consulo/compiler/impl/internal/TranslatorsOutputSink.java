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

import consulo.compiler.CompileContextEx;
import consulo.compiler.IntermediateOutputCompiler;
import consulo.compiler.TranslatingCompiler;
import consulo.compiler.TranslatingCompilerFilesMonitor;
import consulo.logging.Logger;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.util.*;

class TranslatorsOutputSink implements TranslatingCompiler.OutputSink {
  private static final Logger LOG = Logger.getInstance(TranslatorsOutputSink.class);

  final Map<String, Collection<TranslatingCompiler.OutputItem>> myPostponedItems = new HashMap<>();
  private final CompileContextEx myContext;
  private final TranslatingCompiler[] myCompilers;
  private int myCurrentCompilerIdx;
  private final Set<VirtualFile> myCompiledSources = new HashSet<>();
  //private LinkedBlockingQueue<Future> myFutures = new LinkedBlockingQueue<Future>();

  TranslatorsOutputSink(CompileContextEx context, TranslatingCompiler[] compilers) {
    myContext = context;
    myCompilers = compilers;
  }

  public void setCurrentCompilerIndex(int index) {
    myCurrentCompilerIdx = index;
  }

  public Set<VirtualFile> getCompiledSources() {
    return Collections.unmodifiableSet(myCompiledSources);
  }

  @Override
  public void add(final String outputRoot, final Collection<TranslatingCompiler.OutputItem> items, final VirtualFile[] filesToRecompile) {
    for (TranslatingCompiler.OutputItem item : items) {
      final VirtualFile file = item.getSourceFile();
      if (file != null) {
        myCompiledSources.add(file);
      }
    }
    final TranslatingCompiler compiler = myCompilers[myCurrentCompilerIdx];
    if (compiler instanceof IntermediateOutputCompiler) {
      final LocalFileSystem lfs = LocalFileSystem.getInstance();
      final List<VirtualFile> outputs = new ArrayList<>();
      for (TranslatingCompiler.OutputItem item : items) {
        final VirtualFile vFile = lfs.findFileByPath(item.getOutputPath());
        if (vFile != null) {
          outputs.add(vFile);
        }
      }
      myContext.markGenerated(outputs);
    }
    final int nextCompilerIdx = myCurrentCompilerIdx + 1;
    try {
      TranslatingCompilerFilesMonitor translatingCompilerFilesMonitor = TranslatingCompilerFilesMonitor.getInstance();
      
      if (nextCompilerIdx < myCompilers.length) {
        final Map<String, Collection<TranslatingCompiler.OutputItem>> updateNow = new HashMap<>();
        // process postponed
        for (Map.Entry<String, Collection<TranslatingCompiler.OutputItem>> entry : myPostponedItems.entrySet()) {
          final String outputDir = entry.getKey();
          final Collection<TranslatingCompiler.OutputItem> postponed = entry.getValue();
          for (Iterator<TranslatingCompiler.OutputItem> it = postponed.iterator(); it.hasNext(); ) {
            TranslatingCompiler.OutputItem item = it.next();
            boolean shouldPostpone = false;
            for (int idx = nextCompilerIdx; idx < myCompilers.length; idx++) {
              shouldPostpone = myCompilers[idx].isCompilableFile(item.getSourceFile(), myContext);
              if (shouldPostpone) {
                break;
              }
            }
            if (!shouldPostpone) {
              // the file is not compilable by the rest of compilers, so it is safe to update it now
              it.remove();
              addItemToMap(updateNow, outputDir, item);
            }
          }
        }
        // process items from current compilation
        for (TranslatingCompiler.OutputItem item : items) {
          boolean shouldPostpone = false;
          for (int idx = nextCompilerIdx; idx < myCompilers.length; idx++) {
            shouldPostpone = myCompilers[idx].isCompilableFile(item.getSourceFile(), myContext);
            if (shouldPostpone) {
              break;
            }
          }
          if (shouldPostpone) {
            // the file is compilable by the next compiler in row, update should be postponed
            addItemToMap(myPostponedItems, outputRoot, item);
          }
          else {
            addItemToMap(updateNow, outputRoot, item);
          }
        }

        if (updateNow.size() == 1) {
          final Map.Entry<String, Collection<TranslatingCompiler.OutputItem>> entry = updateNow.entrySet().iterator().next();
          final String outputDir = entry.getKey();
          final Collection<TranslatingCompiler.OutputItem> itemsToUpdate = entry.getValue();
          translatingCompilerFilesMonitor.update(myContext, outputDir, itemsToUpdate, filesToRecompile);
        }
        else {
          for (Map.Entry<String, Collection<TranslatingCompiler.OutputItem>> entry : updateNow.entrySet()) {
            final String outputDir = entry.getKey();
            final Collection<TranslatingCompiler.OutputItem> itemsToUpdate = entry.getValue();
            translatingCompilerFilesMonitor.update(myContext, outputDir, itemsToUpdate, VirtualFile.EMPTY_ARRAY);
          }
          if (filesToRecompile.length > 0) {
            translatingCompilerFilesMonitor.update(myContext,
                                                   null,
                                                   Collections.<TranslatingCompiler.OutputItem>emptyList(),
                                                   filesToRecompile);
          }
        }
      }
      else {
        translatingCompilerFilesMonitor.update(myContext, outputRoot, items, filesToRecompile);
      }
    }
    catch (IOException e) {
      LOG.info(e);
      myContext.requestRebuildNextTime(e.getMessage());
    }
  }

  private static void addItemToMap(Map<String, Collection<TranslatingCompiler.OutputItem>> map,
                                   String outputDir,
                                   TranslatingCompiler.OutputItem item) {
    Collection<TranslatingCompiler.OutputItem> collection = map.get(outputDir);
    if (collection == null) {
      collection = new ArrayList<>();
      map.put(outputDir, collection);
    }
    collection.add(item);
  }

  public void flushPostponedItems() {
    final TranslatingCompilerFilesMonitor filesMonitor = TranslatingCompilerFilesMonitor.getInstance();
    try {
      for (Map.Entry<String, Collection<TranslatingCompiler.OutputItem>> entry : myPostponedItems.entrySet()) {
        final String outputDir = entry.getKey();
        final Collection<TranslatingCompiler.OutputItem> items = entry.getValue();
        filesMonitor.update(myContext, outputDir, items, VirtualFile.EMPTY_ARRAY);
      }
    }
    catch (IOException e) {
      LOG.info(e);
      myContext.requestRebuildNextTime(e.getMessage());
    }
  }
}
