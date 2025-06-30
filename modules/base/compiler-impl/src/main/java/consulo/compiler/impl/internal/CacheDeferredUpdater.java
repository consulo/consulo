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

import consulo.application.ApplicationManager;
import consulo.compiler.FileProcessingCompiler;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CacheDeferredUpdater {
    private final Map<File, List<Pair<FileProcessingCompilerStateCache, FileProcessingCompiler.ProcessingItem>>> myData =
        Maps.newHashMap(FileUtil.FILE_HASHING_STRATEGY);

    public void addFileForUpdate(final FileProcessingCompiler.ProcessingItem item, FileProcessingCompilerStateCache cache) {
        final File file = item.getFile();
        List<Pair<FileProcessingCompilerStateCache, FileProcessingCompiler.ProcessingItem>> list = myData.get(file);
        if (list == null) {
            list = new ArrayList<>();
            myData.put(file, list);
        }
        list.add(Pair.create(cache, item));
    }

    public void doUpdate() throws IOException {
        final IOException[] ex = {null};
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                for (Map.Entry<File, List<Pair<FileProcessingCompilerStateCache, FileProcessingCompiler.ProcessingItem>>> entry : myData.entrySet()) {
                    for (Pair<FileProcessingCompilerStateCache, FileProcessingCompiler.ProcessingItem> pair : entry.getValue()) {
                        final FileProcessingCompiler.ProcessingItem item = pair.getSecond();
                        pair.getFirst().update(entry.getKey(), item.getValidityState());
                    }
                }
            }
            catch (IOException e) {
                ex[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
    }
}
