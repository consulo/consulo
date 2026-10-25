// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff.combined;

import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

public class CombinedDiffVirtualFileImpl extends CombinedDiffVirtualFile {
    private final Project myProject;
    private final List<CombinedBlockProducer> myProducers;

    public CombinedDiffVirtualFileImpl(Project project, List<CombinedBlockProducer> producers, String name) {
        this(project, producers, name, name);
    }

    public CombinedDiffVirtualFileImpl(Project project, List<CombinedBlockProducer> producers, String name, String path) {
        super(name, path);
        myProject = project;
        myProducers = producers;
    }

    public Project getProject() {
        return myProject;
    }

    public List<CombinedBlockProducer> getProducers() {
        return myProducers;
    }

    @RequiredUIAccess
    @Override
    public CombinedDiffComponentProcessor createViewer(Project project) {
        CombinedDiffComponentProcessor processor = CombinedDiffManager.getInstance(project).createProcessor();
        processor.setBlocks(myProducers);
        return processor;
    }
}
