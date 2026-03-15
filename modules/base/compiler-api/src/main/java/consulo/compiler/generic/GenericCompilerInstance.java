/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.generic;

import consulo.compiler.CompileContext;
import consulo.compiler.generic.BuildTarget;
import consulo.compiler.generic.CompileItem;
import consulo.compiler.generic.GenericCompilerCacheState;
import consulo.project.Project;

import java.io.File;
import java.util.List;

/**
 * @author nik
 */
public abstract class GenericCompilerInstance<T extends BuildTarget, Item
    extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> {
    protected final CompileContext myContext;

    protected GenericCompilerInstance(CompileContext context) {
        myContext = context;
    }

    protected Project getProject() {
        return myContext.getProject();
    }

    
    public abstract List<T> getAllTargets();

    
    public abstract List<T> getSelectedTargets();

    public abstract void processObsoleteTarget(
        String targetId,
        List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems
    );

    
    public abstract List<Item> getItems(T target);

    public abstract void processItems(
        T target,
        List<GenericCompilerProcessingItem<Item, SourceState, OutputState>> changedItems,
        List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems,
        OutputConsumer<Item> consumer
    );

    public interface OutputConsumer<Item extends CompileItem<?, ?, ?>> {
        void addFileToRefresh(File file);

        void addDirectoryToRefresh(File dir);

        void addProcessedItem(Item sourceItem);
    }
}
