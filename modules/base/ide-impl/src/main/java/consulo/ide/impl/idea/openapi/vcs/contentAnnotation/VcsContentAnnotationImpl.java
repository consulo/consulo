/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.contentAnnotation;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.vcs.AbstractVcs;
import consulo.vcs.ProjectLevelVcsManager;
import consulo.vcs.VcsException;
import consulo.vcs.annotate.FileAnnotation;
import consulo.ide.impl.idea.openapi.vcs.diff.DiffMixin;
import consulo.vcs.history.VcsRevisionDescription;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.ThreeState;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Date;

/**
 * User: Irina.Chernushina
 * Date: 8/3/11
 * Time: 1:04 PM
 */
@Singleton
@ServiceImpl
public class VcsContentAnnotationImpl implements VcsContentAnnotation {
  private final Project myProject;
  private final VcsContentAnnotationSettings mySettings;
  private final ContentAnnotationCache myContentAnnotationCache;
  private static final Logger LOG = Logger.getInstance(VcsContentAnnotationImpl.class);

  public static VcsContentAnnotation getInstance(final Project project) {
    return ServiceManager.getService(project, VcsContentAnnotation.class);
  }

  @Inject
  public VcsContentAnnotationImpl(Project project, VcsContentAnnotationSettings settings, final ContentAnnotationCache contentAnnotationCache) {
    myProject = project;
    mySettings = settings;
    myContentAnnotationCache = contentAnnotationCache;
  }

  @javax.annotation.Nullable
  @Override
  public VcsRevisionNumber fileRecentlyChanged(VirtualFile vf) {
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    final AbstractVcs vcs = vcsManager.getVcsFor(vf);
    if (vcs == null) return null;
    if (vcs.getDiffProvider() instanceof DiffMixin) {
      final VcsRevisionDescription description = ((DiffMixin)vcs.getDiffProvider()).getCurrentRevisionDescription(vf);
      final Date date = description.getRevisionDate();
      return isRecent(date) ? description.getRevisionNumber() : null;
    }
    return null;
  }

  private boolean isRecent(Date date) {
    return date.getTime() > (System.currentTimeMillis() - mySettings.getLimit());
  }

  @Override
  public boolean intervalRecentlyChanged(VirtualFile file, TextRange lineInterval, VcsRevisionNumber currentRevisionNumber) {
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    final AbstractVcs vcs = vcsManager.getVcsFor(file);
    if (vcs == null || vcs.getDiffProvider() == null) return false;
    if (currentRevisionNumber == null) {
      currentRevisionNumber = vcs.getDiffProvider().getCurrentRevision(file);
      assert currentRevisionNumber != null;
    }
    final ThreeState isRecent = myContentAnnotationCache.isRecent(file, vcs.getKeyInstanceMethod(), currentRevisionNumber, lineInterval,
                                                                  System.currentTimeMillis() - mySettings.getLimit());
    if (! ThreeState.UNSURE.equals(isRecent)) return ThreeState.YES.equals(isRecent);

    final FileAnnotation fileAnnotation;
    try {
      fileAnnotation = vcs.getAnnotationProvider().annotate(file);
    }
    catch (VcsException e) {
      LOG.info(e);
      return false;
    }
    myContentAnnotationCache.register(file, vcs.getKeyInstanceMethod(), currentRevisionNumber, fileAnnotation);
    for (int i = lineInterval.getStartOffset(); i <= lineInterval.getEndOffset(); i++) {
      Date lineDate = fileAnnotation.getLineDate(i);
      if (lineDate != null && isRecent(lineDate)) return true;
    }
    return false;
  }
}
