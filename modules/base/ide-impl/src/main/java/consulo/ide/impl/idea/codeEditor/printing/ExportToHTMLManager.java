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

package consulo.ide.impl.idea.codeEditor.printing;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataContext;
import consulo.webBrowser.BrowserUtil;
import consulo.language.editor.action.PrintOption;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

class ExportToHTMLManager {
  private static final Logger LOG = Logger.getInstance(ExportToHTMLManager.class);
  private static FileNotFoundException myLastException;

  private ExportToHTMLManager() {
  }

  /**
   * Should be invoked in event dispatch thread
   */
  @RequiredUIAccess
  public static void executeExport(DataContext dataContext) throws FileNotFoundException {
    PsiDirectory psiDirectory = null;
    PsiElement psiElement = dataContext.getData(PsiElement.KEY);
    if (psiElement instanceof PsiDirectory directory) {
      psiDirectory = directory;
    }
    PsiFile psiFile = dataContext.getData(PsiFile.KEY);
    String shortFileName = null;
    String directoryName = null;
    if (psiFile != null || psiDirectory != null) {
      if (psiFile != null) {
        shortFileName = psiFile.getVirtualFile().getName();
        if (psiDirectory == null) {
          psiDirectory = psiFile.getContainingDirectory();
        }
      }
      if (psiDirectory != null) {
        directoryName = psiDirectory.getVirtualFile().getPresentableUrl();
      }
    }

    Editor editor = dataContext.getData(Editor.KEY);
    boolean isSelectedTextEnabled = false;
    if (editor != null && editor.getSelectionModel().hasSelection()) {
      isSelectedTextEnabled = true;
    }
    Project project = dataContext.getRequiredData(Project.KEY);
    ExportToHTMLDialog exportToHTMLDialog = new ExportToHTMLDialog(shortFileName, directoryName, isSelectedTextEnabled, project);

    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(project);
    if (exportToHTMLSettings.OUTPUT_DIRECTORY == null) {
      VirtualFile baseDir = project.getBaseDir();

      if (baseDir != null) {
        exportToHTMLSettings.OUTPUT_DIRECTORY = baseDir.getPresentableUrl() + File.separator + "exportToHTML";
      }
      else {
        exportToHTMLSettings.OUTPUT_DIRECTORY = "";
      }
    }
    exportToHTMLDialog.reset();
    exportToHTMLDialog.show();
    if (!exportToHTMLDialog.isOK()) {
      return;
    }
    try {
      exportToHTMLDialog.apply();
    }
    catch (ConfigurationException e) {
      Messages.showErrorDialog(project, e.getMessage(), CommonLocalize.titleError().get());
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();
    String outputDirectoryName = exportToHTMLSettings.OUTPUT_DIRECTORY;
    if (exportToHTMLSettings.getPrintScope() != PrintSettings.PRINT_DIRECTORY) {
      if (psiFile == null || psiFile.getText() == null) {
        return;
      }
      String dirName = constructOutputDirectory(psiFile, outputDirectoryName);
      HTMLTextPainter textPainter = new HTMLTextPainter(psiFile, project, dirName, exportToHTMLSettings.PRINT_LINE_NUMBERS);
      if (exportToHTMLSettings.getPrintScope() == PrintSettings.PRINT_SELECTED_TEXT && editor != null && editor.getSelectionModel().hasSelection()) {
        int firstLine = editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionStart());
        textPainter.setSegment(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), firstLine);
      }
      textPainter.paint(null, psiFile.getFileType());
      if (exportToHTMLSettings.OPEN_IN_BROWSER) {
        BrowserUtil.browse(textPainter.getHTMLFileName());
      }
    }
    else {
      myLastException = null;
      ExportRunnable exportRunnable = new ExportRunnable(exportToHTMLSettings, psiDirectory, outputDirectoryName, project);
      ProgressManager.getInstance().runProcessWithProgressSynchronously(exportRunnable, CodeEditorBundle.message("export.to.html.title"), true, project);
      if (myLastException != null) {
        throw myLastException;
      }
    }
  }

  @RequiredReadAction
  private static boolean exportPsiFile(
    PsiFile psiFile,
    String outputDirectoryName,
    Project project,
    HashMap<PsiFile, PsiFile> filesMap
  ) {
    try {
      return project.getApplication().runReadAction((ThrowableComputable<Boolean, FileNotFoundException>)() -> {
        ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(project);

        if (psiFile instanceof PsiBinaryFile) {
          return true;
        }

        TreeMap<Integer, PsiReference> refMap = null;
        for (PrintOption printOption : Application.get().getExtensionList(PrintOption.class)) {
          TreeMap<Integer, PsiReference> map = printOption.collectReferences(psiFile, filesMap);
          if (map != null) {
            refMap = new TreeMap<>();
            refMap.putAll(map);
          }
        }

        String dirName = constructOutputDirectory(psiFile, outputDirectoryName);
        HTMLTextPainter textPainter = new HTMLTextPainter(psiFile, project, dirName, exportToHTMLSettings.PRINT_LINE_NUMBERS);
        textPainter.paint(refMap, psiFile.getFileType());
        return true;
      });
    }
    catch (FileNotFoundException throwable) {
      myLastException = throwable;
      return true;
    }
  }

  private static String constructOutputDirectory(PsiFile psiFile, String outputDirectoryName) {
    return constructOutputDirectory(psiFile.getContainingDirectory(), outputDirectoryName);
  }

  private static String constructOutputDirectory(@Nonnull PsiDirectory directory, String outputDirectoryName) {
    String qualifiedName = PsiPackageHelper.getInstance(directory.getProject()).getQualifiedName(directory, false);
    String dirName = outputDirectoryName;
    if (qualifiedName.length() > 0) {
      dirName += File.separator + qualifiedName.replace('.', File.separatorChar);
    }
    File dir = new File(dirName);
    dir.mkdirs();
    return dirName;
  }

  @RequiredReadAction
  private static void addToPsiFileList(PsiDirectory psiDirectory,
                                       ArrayList<PsiFile> filesList,
                                       boolean isRecursive,
                                       String outputDirectoryName) throws FileNotFoundException {
    PsiFile[] files = psiDirectory.getFiles();
    Collections.addAll(filesList, files);
    generateIndexHtml(psiDirectory, isRecursive, outputDirectoryName);
    if (isRecursive) {
      PsiDirectory[] directories = psiDirectory.getSubdirectories();
      for (PsiDirectory directory : directories) {
        addToPsiFileList(directory, filesList, isRecursive, outputDirectoryName);
      }
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @RequiredReadAction
  private static void generateIndexHtml(PsiDirectory psiDirectory, boolean recursive, String outputDirectoryName)
    throws FileNotFoundException {
    String indexHtmlName = constructOutputDirectory(psiDirectory, outputDirectoryName) + File.separator + "index.html";
    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(indexHtmlName), StandardCharsets.UTF_8);
    String title = PsiPackageHelper.getInstance(psiDirectory.getProject()).getQualifiedName(psiDirectory, true);
    try {
      writer.write("<html><head><title>" + title + "</title></head><body>");
      if (recursive) {
        PsiDirectory[] directories = psiDirectory.getSubdirectories();
        for (PsiDirectory directory: directories) {
          writer.write("<a href=\"" + directory.getName() + "/index.html\"><b>" + directory.getName() + "</b></a><br />");
        }
      }
      PsiFile[] files = psiDirectory.getFiles();
      for (PsiFile file: files) {
        if (!(file instanceof PsiBinaryFile)) {
          writer.write("<a href=\"" + getHTMLFileName(file) + "\">" + file.getVirtualFile().getName() + "</a><br />");
        }
      }
      writer.write("</body></html>");
      writer.close();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static class ExportRunnable implements Runnable {
    private final ExportToHTMLSettings myExportToHTMLSettings;
    private final PsiDirectory myPsiDirectory;
    private final String myOutputDirectoryName;
    private final Project myProject;

    public ExportRunnable(ExportToHTMLSettings exportToHTMLSettings,
                          PsiDirectory psiDirectory,
                          String outputDirectoryName,
                          Project project) {
      myExportToHTMLSettings = exportToHTMLSettings;
      myPsiDirectory = psiDirectory;
      myOutputDirectoryName = outputDirectoryName;
      myProject = project;
    }

    @Override
    @RequiredReadAction
    public void run() {
      ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();

      ArrayList<PsiFile> filesList = new ArrayList<>();
      boolean isRecursive = myExportToHTMLSettings.isIncludeSubdirectories();

      try {
        myProject.getApplication().runReadAction((ThrowableComputable<Void, FileNotFoundException>)() -> {
          addToPsiFileList(myPsiDirectory, filesList, isRecursive, myOutputDirectoryName);
          return null;
        });
      }
      catch (FileNotFoundException e) {
        myLastException = e;
        return;
      }
      HashMap<PsiFile, PsiFile> filesMap = new HashMap<>();
      for (PsiFile psiFile : filesList) {
        filesMap.put(psiFile, psiFile);
      }
      for (int i = 0; i < filesList.size(); i++) {
        PsiFile psiFile = filesList.get(i);
        if (progressIndicator.isCanceled()) {
          return;
        }
        progressIndicator.setText(CodeEditorBundle.message("export.to.html.generating.file.progress", getHTMLFileName(psiFile)));
        progressIndicator.setFraction(((double)i)/filesList.size());
        if (!exportPsiFile(psiFile, myOutputDirectoryName, myProject, filesMap)) {
          return;
        }
      }
      if (myExportToHTMLSettings.OPEN_IN_BROWSER) {
        String dirToShow = myExportToHTMLSettings.OUTPUT_DIRECTORY;
        if (!dirToShow.endsWith(File.separator)) {
          dirToShow += File.separatorChar;
        }
        dirToShow += PsiPackageHelper.getInstance(myProject).getQualifiedName(myPsiDirectory, false).replace('.', File.separatorChar);
        BrowserUtil.browse(dirToShow);
      }
    }
  }

  static String getHTMLFileName(PsiFile psiFile) {
    //noinspection HardCodedStringLiteral
    return psiFile.getVirtualFile().getName() + ".html";
  }
}
