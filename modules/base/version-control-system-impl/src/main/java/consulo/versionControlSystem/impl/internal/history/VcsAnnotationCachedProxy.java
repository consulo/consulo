/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.history;

import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.VcsAnnotation;
import consulo.versionControlSystem.annotate.VcsCacheableAnnotationProvider;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 * @since 2011-03-17
 */
public class VcsAnnotationCachedProxy implements AnnotationProvider {
  private final VcsHistoryCache myCache;
  private final AbstractVcs myVcs;
  private final static Logger LOG = Logger.getInstance(VcsAnnotationCachedProxy.class);
  private final AnnotationProvider myAnnotationProvider;

  public VcsAnnotationCachedProxy(@Nonnull AbstractVcs vcs, @Nonnull AnnotationProvider provider) {
    assert provider instanceof VcsCacheableAnnotationProvider;
    myVcs = vcs;
    myCache = ProjectLevelVcsManager.getInstance(vcs.getProject()).getVcsHistoryCache();
    myAnnotationProvider = provider;
  }

  @Override
  public FileAnnotation annotate(final VirtualFile file) throws VcsException {
    DiffProvider diffProvider = myVcs.getDiffProvider();
    VcsRevisionNumber currentRevision = diffProvider.getCurrentRevision(file);

    return annotate(file, currentRevision, true, new ThrowableComputable<FileAnnotation, VcsException>() {
      @Override
      public FileAnnotation compute() throws VcsException {
        return myAnnotationProvider.annotate(file);
      }
    });
  }

  @Override
  public FileAnnotation annotate(final VirtualFile file, final VcsFileRevision revision) throws VcsException {
    return annotate(file, revision.getRevisionNumber(), false, new ThrowableComputable<FileAnnotation, VcsException>() {
      @Override
      public FileAnnotation compute() throws VcsException {
        return myAnnotationProvider.annotate(file, revision);
      }
    });
  }

  @Override
  public boolean isCaching() {
    return true;
  }

  /**
   * @param currentRevision - just a hint for optimization
   */
  private FileAnnotation annotate(VirtualFile file, VcsRevisionNumber revisionNumber, boolean currentRevision,
                                  ThrowableComputable<FileAnnotation, VcsException> delegate) throws VcsException {
    AnnotationProvider annotationProvider = myAnnotationProvider;

    FilePath filePath = VcsUtil.getFilePath(file);

    VcsCacheableAnnotationProvider cacheableAnnotationProvider = (VcsCacheableAnnotationProvider)annotationProvider;

    VcsAnnotation vcsAnnotation = null;
    if (revisionNumber != null) {
      Object cachedData = myCache.get(filePath, myVcs.getKeyInstanceMethod(), revisionNumber);
      vcsAnnotation = ObjectUtil.tryCast(cachedData, VcsAnnotation.class);
    }

    if (vcsAnnotation != null) {
      VcsHistoryProvider historyProvider = myVcs.getVcsHistoryProvider();
      VcsAbstractHistorySession history = getHistory(revisionNumber, filePath, historyProvider, vcsAnnotation.getFirstRevision());
      if (history == null) return null;
      // question is whether we need "not moved" path here?
      ContentRevision fileContent = myVcs.getDiffProvider().createFileContent(revisionNumber, file);
      FileAnnotation restored = cacheableAnnotationProvider.
              restore(vcsAnnotation, history, fileContent.getContent(), currentRevision,
                      revisionNumber);
      if (restored != null) {
        return restored;
      }
    }

    FileAnnotation fileAnnotation = delegate.compute();
    vcsAnnotation = cacheableAnnotationProvider.createCacheable(fileAnnotation);
    if (vcsAnnotation == null) return fileAnnotation;

    if (revisionNumber != null) {
      myCache.put(filePath, myVcs.getKeyInstanceMethod(), revisionNumber, vcsAnnotation);
    }

    if (myVcs.getVcsHistoryProvider() instanceof VcsCacheableHistorySessionFactory) {
      loadHistoryInBackgroundToCache(revisionNumber, filePath, vcsAnnotation);
    }
    return fileAnnotation;
  }

  // todo will be removed - when annotation will be presented together with history
  private void loadHistoryInBackgroundToCache(final VcsRevisionNumber revisionNumber,
                                              final FilePath filePath,
                                              final VcsAnnotation vcsAnnotation) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        try {
          getHistory(revisionNumber, filePath, myVcs.getVcsHistoryProvider(), vcsAnnotation.getFirstRevision());
        }
        catch (VcsException e) {
          LOG.info(e);
        }
      }
    });
  }

  private VcsAbstractHistorySession getHistory(VcsRevisionNumber revision, FilePath filePath, VcsHistoryProvider historyProvider,
                                               @jakarta.annotation.Nullable VcsRevisionNumber firstRevision) throws VcsException {
    boolean historyCacheSupported = historyProvider instanceof VcsCacheableHistorySessionFactory;
    if (historyCacheSupported) {
      VcsCacheableHistorySessionFactory cacheableHistorySessionFactory = (VcsCacheableHistorySessionFactory)historyProvider;
      VcsAbstractHistorySession cachedSession =
              myCache.getMaybePartial(filePath, myVcs.getKeyInstanceMethod(), cacheableHistorySessionFactory);
      if (cachedSession != null && ! cachedSession.getRevisionList().isEmpty()) {
        VcsFileRevision recentRevision = cachedSession.getRevisionList().get(0);
        if (recentRevision.getRevisionNumber().compareTo(revision) >= 0 && (firstRevision == null || cachedSession.getHistoryAsMap().containsKey(firstRevision))) {
          return cachedSession;
        }
      }
    }
    // history may be also cut
    VcsAbstractHistorySession sessionFor;
    if (firstRevision != null) {
      sessionFor = limitedHistory(filePath, firstRevision);
    } else {
      sessionFor = (VcsAbstractHistorySession) historyProvider.createSessionFor(filePath);
    }
    if (sessionFor != null && historyCacheSupported) {
      VcsCacheableHistorySessionFactory cacheableHistorySessionFactory = (VcsCacheableHistorySessionFactory)historyProvider;
      FilePath correctedPath = cacheableHistorySessionFactory.getUsedFilePath(sessionFor);
      myCache.put(filePath, correctedPath, myVcs.getKeyInstanceMethod(), sessionFor, cacheableHistorySessionFactory, firstRevision == null);
    }
    return sessionFor;
  }

  @Override
  public boolean isAnnotationValid(@Nonnull VcsFileRevision rev) {
    return myAnnotationProvider.isAnnotationValid(rev);
  }

  private VcsAbstractHistorySession limitedHistory(FilePath filePath, @Nonnull final VcsRevisionNumber firstNumber) throws VcsException {
    final VcsAbstractHistorySession[] result = new VcsAbstractHistorySession[1];
    final VcsException[] exc = new VcsException[1];

    try {
      myVcs.getVcsHistoryProvider().reportAppendableHistory(filePath, new VcsAppendableHistorySessionPartner() {
        @Override
        public void reportCreatedEmptySession(VcsAbstractHistorySession session) {
          result[0] = session;
        }

        @Override
        public void acceptRevision(VcsFileRevision revision) {
          result[0].appendRevision(revision);
          if (firstNumber.equals(revision.getRevisionNumber())) throw new ProcessCanceledException();
        }

        @Override
        public void reportException(VcsException exception) {
          exc[0] = exception;
        }

        @Override
        public void finished() {
        }

        @Override
        public void beforeRefresh() {
        }

        @Override
        public void forceRefresh() {
        }
      });
    } catch (ProcessCanceledException e) {
      // ok
    }
    if (exc[0] != null) {
      throw exc[0];
    }
    return result[0];
  }
}

