/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ide.impl.idea.tools;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.language.file.LanguageFileType;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.content.ContentIterator;
import consulo.module.content.ProjectRootManager;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ScanSourceCommentsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(ScanSourceCommentsAction.class);

  @Override
  public void actionPerformed(AnActionEvent e) {

    final Project p = e.getDataContext().getData(CommonDataKeys.PROJECT);
    final String file =
      Messages.showInputDialog(p, "Enter path to the file comments will be extracted to", "Comments File Path", Messages.getQuestionIcon());

    try {
      final PrintStream stream = new PrintStream(file);
      stream.println("Comments in " + p.getName());

      ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
        @Override
        public void run() {
          final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          ProjectRootManager.getInstance(p).getFileIndex().iterateContent(new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile fileOrDir) {
              if (fileOrDir.isDirectory()) {
                indicator.setText("Extracting comments");
                indicator.setText2(fileOrDir.getPresentableUrl());
              }
              scanCommentsInFile(p, fileOrDir);
              return true;
            }
          });

          indicator.setText2("");
          int count = 1;
          for (CommentDescriptor descriptor : myComments.values()) {
            stream.println("#" + count + " ---------------------------------------------------------------");
            descriptor.print(stream);
            stream.println();
            count++;
          }

        }
      }, "Generating Comments", true, p);


      stream.close();

    }
    catch (Throwable e1) {
      LOG.error(e1);
      Messages.showErrorDialog(p, "Error writing? " + e1.getMessage(), "Problem writing");
    }
  }

  private final Map<String, CommentDescriptor> myComments = new HashMap<String, CommentDescriptor>();

  private void commentFound(VirtualFile file, String text) {
    String reduced = text.replaceAll("\\s", "");
    CommentDescriptor descriptor = myComments.get(reduced);
    if (descriptor == null) {
      descriptor = new CommentDescriptor(text);
      myComments.put(reduced, descriptor);
    }
    descriptor.addFile(file);
  }

  private void scanCommentsInFile(Project project, final VirtualFile vFile) {
    if (!vFile.isDirectory() && vFile.getFileType() instanceof LanguageFileType) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
      if (psiFile == null) return;

      for (PsiFile root : psiFile.getViewProvider().getAllFiles()) {
        root.accept(new PsiRecursiveElementWalkingVisitor() {
          @Override
          public void visitComment(PsiComment comment) {
            commentFound(vFile, comment.getText());
          }
        });
      }
    }
  }


  private class CommentDescriptor {
    private final String myText;
    private final Set<VirtualFile> myFiles = new LinkedHashSet<VirtualFile>();

    public CommentDescriptor(String text) {
      myText = text;
    }

    public void addFile(VirtualFile file) {
      myFiles.add(file);
    }

    public void print(PrintStream out) {
      out.println(myText);
      int count = myFiles.size();
      int i = 0;

      for (VirtualFile file : myFiles) {
        out.println(file.getPresentableUrl());
        count--;
        i++;
        if (i > 5 && count > 2) break;
      }

      if (count > 0) {
        out.println("And " + count + " more files");
      }
    }
  }
}
