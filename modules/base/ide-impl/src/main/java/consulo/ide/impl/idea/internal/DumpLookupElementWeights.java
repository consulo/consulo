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

package consulo.ide.impl.idea.internal;

import consulo.ide.impl.idea.codeInsight.lookup.impl.LookupImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class DumpLookupElementWeights extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DumpLookupElementWeights.class);

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    dumpLookupElementWeights((LookupImpl)LookupManager.getActiveLookup(editor));
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    presentation.setEnabled(editor != null && LookupManager.getActiveLookup(editor) != null);
  }

  public static void dumpLookupElementWeights(final LookupImpl lookup) {
    LookupElement selected = lookup.getCurrentItem();
    String sb = "selected: " + selected;
    if (selected != null) {
      sb += "\nprefix: " + lookup.itemPattern(selected);
    }
    sb += "\nweights:\n" + StringUtil.join(getLookupElementWeights(lookup, true), "\n");
    System.out.println(sb);
    LOG.info(sb);
    try {
      CopyPasteManager.getInstance().setContents(new StringSelection(sb));
    }
    catch (Exception ignore) {
    }
  }

  public static List<String> getLookupElementWeights(LookupImpl lookup, boolean hideSingleValued) {
    final Map<LookupElement, List<Pair<String, Object>>> weights = lookup.getRelevanceObjects(lookup.getItems(), hideSingleValued);
    return ContainerUtil.map(weights.entrySet(), entry -> entry.getKey().getLookupString() + "\t" + StringUtil.join(entry.getValue(), pair -> pair.first + "=" + pair.second, ", "));
  }
}