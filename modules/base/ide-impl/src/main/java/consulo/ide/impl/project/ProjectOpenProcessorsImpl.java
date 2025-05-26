/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.project;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.moduleImport.ImportProjectOpenProcessor;
import consulo.project.impl.internal.DefaultProjectOpenProcessor;
import consulo.project.impl.internal.FolderProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
@Singleton
@ServiceImpl
public class ProjectOpenProcessorsImpl implements ProjectOpenProcessors {
    private LazyValue<List<ProjectOpenProcessor>> myDefaultProcessors = LazyValue.notNull(() -> {
        List<ProjectOpenProcessor> processors = new ArrayList<>(2);
        processors.add(DefaultProjectOpenProcessor.getInstance());
        processors.add(new ImportProjectOpenProcessor());
        return processors;
    });

    @Nonnull
    @Override
    public List<ProjectOpenProcessor> getProcessors() {
        return myDefaultProcessors.get();
    }

    @Nullable
    @Override
    public ProjectOpenProcessor findProcessor(@Nonnull File file) {
        for (ProjectOpenProcessor processor : getProcessors()) {
            if (processor.canOpenProject(file)) {
                return processor;
            }
        }
        return FolderProjectOpenProcessor.INSTANCE;
    }
}
