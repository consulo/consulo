// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class FilesProcessorImpl implements Disposable {
    protected final Project myProject;

    private final Set<VirtualFile> myFiles = new LinkedHashSet<>();

    protected FilesProcessorImpl(Project project, Disposable parentDisposable) {
        myProject = project;
        Disposer.register(parentDisposable, this);
    }

    public abstract void doActionOnChosenFiles(Collection<VirtualFile> files);

    protected void processFiles(Collection<VirtualFile> files) {
        if (files.isEmpty()) {
            return;
        }

        addNewFiles(files);

        if (needDoForCurrentProject()) {
            doActionOnChosenFiles(acquireValidFiles());
        }
        else {
            handleProcessingForCurrentProject();
        }
    }

    protected void handleProcessingForCurrentProject() {
    }

    protected boolean removeFiles(Collection<VirtualFile> filesToRemove) {
        synchronized (myFiles) {
            return myFiles.removeAll(filesToRemove);
        }
    }

    protected boolean isFilesEmpty() {
        synchronized (myFiles) {
            return myFiles.isEmpty();
        }
    }

    private void addNewFiles(Collection<VirtualFile> filesToAdd) {
        synchronized (myFiles) {
            myFiles.addAll(filesToAdd);
        }
    }

    protected List<VirtualFile> selectValidFiles() {
        synchronized (myFiles) {
            myFiles.removeIf(it -> !it.isValid());
            return new ArrayList<>(myFiles);
        }
    }

    protected List<VirtualFile> acquireValidFiles() {
        synchronized (myFiles) {
            List<VirtualFile> result = new ArrayList<>();
            for (VirtualFile file : myFiles) {
                if (file.isValid()) {
                    result.add(file);
                }
            }
            myFiles.clear();
            return result;
        }
    }

    protected void clearFiles() {
        synchronized (myFiles) {
            myFiles.clear();
        }
    }

    @Override
    public void dispose() {
        clearFiles();
    }

    protected abstract boolean needDoForCurrentProject();
}
