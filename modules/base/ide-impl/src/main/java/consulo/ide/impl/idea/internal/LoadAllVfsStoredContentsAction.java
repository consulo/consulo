// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.internal;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.inject.Inject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class LoadAllVfsStoredContentsAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(LoadAllVfsStoredContentsAction.class);

    private final Application myApplication;
    private final ManagingFS myManagingFS;

    private final AtomicInteger myCount = new AtomicInteger();
    private final AtomicLong myTotalSize = new AtomicLong();

    @Inject
    public LoadAllVfsStoredContentsAction(Application application, ManagingFS managingFS) {
        super("Load All VirtualFiles Content", "Measure virtualFile.contentsToByteArray() for all virtual files stored in the VFS", null);
        myApplication = application;
        myManagingFS = managingFS;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        String m = "Started loading content";
        LOG.info(m);
        System.out.println(m);
        long start = System.currentTimeMillis();

        myCount.set(0);
        myTotalSize.set(0);
        ((ApplicationEx) myApplication).runProcessWithProgressSynchronously(
            () -> {
                VirtualFile[] roots = myManagingFS.getRoots();
                for (VirtualFile root : roots) {
                    iterateCached(root);
                }
            },
            "Loading",
            false,
            null
        );

        long end = System.currentTimeMillis();
        String message = "Finished loading content of " + myCount + " files. " +
            "Total size=" + StringUtil.formatFileSize(myTotalSize.get()) + ". " +
            "Elapsed=" + ((end - start) / 1000) + "sec.";
        LOG.info(message);
        System.out.println(message);
    }

    private void iterateCached(VirtualFile root) {
        processFile((NewVirtualFile) root);
        Collection<VirtualFile> children = ((NewVirtualFile) root).getCachedChildren();
        for (VirtualFile child : children) {
            iterateCached(child);
        }
    }

    public boolean processFile(NewVirtualFile file) {
        if (file.isDirectory() || file.is(VFileProperty.SPECIAL)) {
            return true;
        }
        try {
            try (InputStream stream = PersistentFS.getInstance().getInputStream(file)) {
                // check if it's really cached in VFS
                if (!(stream instanceof DataInputStream)) {
                    return true;
                }
                byte[] bytes = StreamUtil.loadFromStream(stream);
                myTotalSize.addAndGet(bytes.length);
                myCount.incrementAndGet();
                Objects.requireNonNull(ProgressManager.getInstance().getProgressIndicator())
                    .setText(LocalizeValue.of(file.getPresentableUrl()));
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }
}