/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.pathMacro.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroManager;
import consulo.pathMacro.PromptingMacro;
import consulo.pathMacro.SecondQueueExpandMacro;
import consulo.pathMacro.impl.internal.builtin.*;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.ConvertingIterator;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Function;

@Singleton
@ServiceImpl
public final class MacroManagerImpl implements MacroManager {
  private final List<Macro> myPredefinedMacroes = new ArrayList<>();

  public MacroManagerImpl() {
    registerMacro(new SourcepathMacro());
    registerMacro(new FileDirMacro());
    registerMacro(new FileDirNameMacro());
    registerMacro(new FileParentDirMacro());
    registerMacro(new FileDirPathFromParentMacro());
    registerMacro(new FileExtMacro());
    registerMacro(new FileNameMacro());
    registerMacro(new FileNameWithoutExtension());
    registerMacro(new FileNameWithoutAllExtensions());
    registerMacro(new FilePathMacro());
    registerMacro(new FileEncodingMacro());
    registerMacro(new FileDirRelativeToProjectRootMacro());
    registerMacro(new FilePathRelativeToProjectRootMacro());
    registerMacro(new FileDirRelativeToSourcepathMacro());
    registerMacro(new FilePathRelativeToSourcepathMacro());
    registerMacro(new PromptMacro());
    registerMacro(new FilePromptMacro());
    registerMacro(new SourcepathEntryMacro());
    registerMacro(new ProjectFileDirMacro());
    registerMacro(new ProjectNameMacro());
    registerMacro(new ProjectPathMacro());

    registerMacro(new ModuleDirMacro());
    registerMacro(new ModuleNameMacro());
    registerMacro(new ModuleProfileNameMacro());
    registerMacro(new ModulePathMacro());
    registerMacro(new ModuleProductionOutputDirPathMacro());
    registerMacro(new ModuleTestOutputDirPathMacro());

    registerMacro(new FileRelativePathMacro());
    registerMacro(new FileRelativeDirMacro());
    registerMacro(new LineNumberMacro());
    registerMacro(new ColumnNumberMacro());

    registerMacro(new ClasspathMacro());
    registerMacro(new ClasspathEntryMacro());

    registerMacro(new SelectedTextMacro());
    registerMacro(new SelectionStartLineMacro());
    registerMacro(new SelectionStartColumnMacro());
    registerMacro(new SelectionEndLineMacro());
    registerMacro(new SelectionEndColumnMacro());

    if (File.separatorChar != '/') {
      registerMacro(new FileDirRelativeToProjectRootMacro2());
      registerMacro(new FilePathRelativeToProjectRootMacro2());
      registerMacro(new FileDirRelativeToSourcepathMacro2());
      registerMacro(new FilePathRelativeToSourcepathMacro2());
      registerMacro(new FileRelativeDirMacro2());
      registerMacro(new FileRelativePathMacro2());
    }
  }

  private void registerMacro(Macro macro) {
    myPredefinedMacroes.add(macro);
  }

  @Override
  @Nonnull
  public Collection<Macro> getMacros() {
    return ContainerUtil.concat(myPredefinedMacroes, Application.get().getExtensionPoint(Macro.class).getExtensionList());
  }

  @Override
  public void cacheMacrosPreview(DataContext dataContext) {
    dataContext = getCorrectContext(dataContext);
    for (Macro macro : getMacros()) {
      macro.cachePreview(dataContext);
    }
  }

  private static DataContext getCorrectContext(DataContext dataContext) {
    if (dataContext.hasData(FileEditor.KEY)) {
      return dataContext;
    }
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return dataContext;
    }
    FileEditorManager editorManager = FileEditorManager.getInstance(project);
    VirtualFile[] files = editorManager.getSelectedFiles();
    if (files.length == 0) {
      return dataContext;
    }
    FileEditor fileEditor = editorManager.getSelectedEditor(files[0]);
    return fileEditor == null ? dataContext : DataManager.getInstance().getDataContext(fileEditor.getComponent());
  }

  /**
   * Expands all macros that are found in the <code>str</code>.
   */
  @Override
  @Nullable
  public String expandMacrosInString(String str, boolean firstQueueExpand, DataContext dataContext) throws Macro.ExecutionCancelledException {
    return expandMacroSet(str, firstQueueExpand, dataContext, getMacros().iterator());
  }

  @Nullable
  private String expandMacroSet(String str, boolean firstQueueExpand, DataContext dataContext, Iterator<Macro> macros) throws Macro.ExecutionCancelledException {
    if (str == null) return null;
    while (macros.hasNext()) {
      Macro macro = macros.next();
      if (macro instanceof SecondQueueExpandMacro && firstQueueExpand) continue;
      String name = "$" + macro.getName() + "$";
      String macroNameWithParamStart = "$" + macro.getName() + "(";
      if (str.contains(name)) {
        String expanded = macro.expand(dataContext);
        //if (dataContext instanceof DataManagerImpl.MyDataContext) {
        //  // hack: macro.expand() can cause UI events such as showing dialogs ('Prompt' macro) which may 'invalidate' the datacontext
        //  // since we know exactly that context is valid, we need to update its event count
        //  ((DataManagerImpl.MyDataContext)dataContext).setEventCount(IdeEventQueue.getInstance().getEventCount());
        //}
        if (expanded == null) {
          expanded = "";
        }
        str = StringUtil.replace(str, name, expanded);
      }
      else if (str.contains(macroNameWithParamStart)) {
        String macroNameWithParamEnd = ")$";
        Map<String, String> toReplace = null;
        int i = str.indexOf(macroNameWithParamStart);
        while (i != -1) {
          int j = str.indexOf(macroNameWithParamEnd, i + macroNameWithParamStart.length());
          if (j > i) {
            String param = str.substring(i + macroNameWithParamStart.length(), j);
            if (toReplace == null) toReplace = new HashMap<>();
            String expanded = macro.expand(dataContext, param);
            if (expanded == null) {
              expanded = "";
            }
            toReplace.put(macroNameWithParamStart + param + macroNameWithParamEnd, expanded);
            i = j + macroNameWithParamEnd.length();
          }
          else {
            break;
          }
        }
        if (toReplace != null) {
          for (Map.Entry<String, String> entry : toReplace.entrySet()) {
            str = StringUtil.replace(str, entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return str;
  }

  @Override
  public String expandSilentMarcos(String str, boolean firstQueueExpand, DataContext dataContext) throws Macro.ExecutionCancelledException {
    final Function<Macro, Macro> convertor = macro -> {
      if (macro instanceof PromptingMacro) {
        return new Macro.Silent(macro, "");
      }
      return macro;
    };
    return expandMacroSet(str, firstQueueExpand, dataContext, ConvertingIterator.create(getMacros().iterator(), convertor));
  }
}
