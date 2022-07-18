// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.internal;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationManagerEx;
import consulo.logging.Logger;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ManagingFS;
import consulo.virtualFileSystem.internal.NewVirtualFile;
import consulo.ide.impl.idea.openapi.vfs.newvfs.persistent.PersistentFS;
import javax.annotation.Nonnull;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class LoadAllVfsStoredContentsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(LoadAllVfsStoredContentsAction.class);

  private final AtomicInteger count = new AtomicInteger();
  private final AtomicLong totalSize = new AtomicLong();

  public LoadAllVfsStoredContentsAction() {
    super("Load All VirtualFiles Content", "Measure virtualFile.contentsToByteArray() for all virtual files stored in the VFS", null);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    String m = "Started loading content";
    LOG.info(m);
    System.out.println(m);
    long start = System.currentTimeMillis();

    count.set(0);
    totalSize.set(0);
    application.runProcessWithProgressSynchronously(() -> {
      PersistentFS vfs = (PersistentFS)application.getComponent(ManagingFS.class);
      VirtualFile[] roots = vfs.getRoots();
      for (VirtualFile root : roots) {
        iterateCached(root);
      }
    }, "Loading", false, null);

    long end = System.currentTimeMillis();
    String message = "Finished loading content of " + count + " files. " + "Total size=" + StringUtil.formatFileSize(totalSize.get()) + ". " + "Elapsed=" + ((end - start) / 1000) + "sec.";
    LOG.info(message);
    System.out.println(message);
  }

  private void iterateCached(VirtualFile root) {
    processFile((NewVirtualFile)root);
    Collection<VirtualFile> children = ((NewVirtualFile)root).getCachedChildren();
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
        if (!(stream instanceof DataInputStream)) return true;
        byte[] bytes = FileUtil.loadBytes(stream);
        totalSize.addAndGet(bytes.length);
        count.incrementAndGet();
        ProgressManager.getInstance().getProgressIndicator().setText(file.getPresentableUrl());
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return true;
  }

  @Override
  public void update(@Nonnull final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}