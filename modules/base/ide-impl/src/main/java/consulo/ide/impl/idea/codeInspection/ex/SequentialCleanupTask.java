/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.application.progress.SequentialModalProgressTask;
import consulo.application.progress.SequentialTask;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Pair;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

class SequentialCleanupTask implements SequentialTask {
  private static final Logger LOG = Logger.getInstance(SequentialCleanupTask.class);

  private final Project myProject;
  private final LinkedHashMap<PsiFile, List<HighlightInfo>> myResults;
  private Iterator<PsiFile> myFileIterator;
  private final SequentialModalProgressTask myProgressTask;
  private int myCount = 0;

  public SequentialCleanupTask(Project project, LinkedHashMap<PsiFile, List<HighlightInfo>> results, SequentialModalProgressTask task) {
    myProject = project;
    myResults = results;
    myProgressTask = task;
    myFileIterator = myResults.keySet().iterator();
  }

  @Override
  public void prepare() {}

  @Override
  public boolean isDone() {
    return myFileIterator == null || !myFileIterator.hasNext();
  }

  @Override
  public boolean iteration() {
    final ProgressIndicator indicator = myProgressTask.getIndicator();
    if (indicator != null) {
      indicator.setFraction((double) myCount++/myResults.size());
    }
    final PsiFile file = myFileIterator.next();
    final List<HighlightInfo> infos = myResults.get(file);
    Collections.reverse(infos); //sort bottom - top
    for (HighlightInfo info : infos) {
      for (final Pair<IntentionActionDescriptor, TextRange> actionRange : ((HighlightInfoImpl)info).quickFixActionRanges) {
        try {
          actionRange.getFirst().getAction().invoke(myProject, null, file);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
    return true;
  }

  @Override
  public void stop() {
    myFileIterator = null;
  }
}
