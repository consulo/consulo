// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.ui.console;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class HyperlinkInfoFactory {
  
  public static HyperlinkInfoFactory getInstance() {
    return Application.get().getInstance(HyperlinkInfoFactory.class);
  }

  
  public abstract HyperlinkInfo createMultipleFilesHyperlinkInfo(List<? extends VirtualFile> files, int line, Project project);

  /**
   * Creates a hyperlink which points to several files with ability to calculate a position inside line
   *
   * @param files   list of files to navigate to (will be suggested to user)
   * @param line    line number to navigate to
   * @param project a project
   * @param action  an action to be performed once editor is opened
   * @return newly created HyperlinkInfo which navigates to given line and column
   */
  public abstract HyperlinkInfo createMultipleFilesHyperlinkInfo(List<? extends VirtualFile> files, int line, Project project, @Nullable HyperlinkHandler action);

  /**
   * Creates a hyperlink that points to elements with ability to navigate to specific element within the file
   *
   * @param elements elements list
   * @return newly create HyperlinkInfo that navigates to given psi elements
   */
  public abstract HyperlinkInfo createMultiplePsiElementHyperlinkInfo(Collection<? extends PsiElement> elements);

  public interface HyperlinkHandler {
    void onLinkFollowed(Project project, VirtualFile psiFile, Editor targetEditor, @Nullable Editor originalEditor);
  }
}
