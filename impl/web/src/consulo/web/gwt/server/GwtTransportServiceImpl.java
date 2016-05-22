/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.intellij.codeInsight.daemon.impl.*;
import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.util.BitUtil;
import consulo.web.gwt.shared.GwtTransportService;
import consulo.web.gwt.shared.transport.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtTransportServiceImpl extends RemoteServiceServlet implements GwtTransportService {

  private Project getProject() {
    String path = "R:/_github.com/consulo/mssdw";

    try {
      final Project project;
      ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
      Project[] openProjects = projectManager.getOpenProjects();
      if (openProjects.length > 0) {
        project = openProjects[0];
      }
      else {
        project = projectManager.loadProject(path);
        projectManager.openTestProject(project);
        final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
        startupManager.runStartupActivities();
        startupManager.startCacheUpdate();
      }
      return project;
    }
    catch (Exception e) {
      e.getMessage();
    }
    return null;
  }

  @NotNull
  @Override
  public List<GwtVirtualFile> listChildren(String fileUrl) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl == null) {
      return Collections.emptyList();
    }
    Project project = getProject();
    List<GwtVirtualFile> list = new ArrayList<GwtVirtualFile>();
    for (VirtualFile virtualFile : fileByUrl.getChildren()) {
      list.add(GwtVirtualFileUtil.createVirtualFile(project, virtualFile));
    }
    return list;
  }

  @Override
  public GwtProjectInfo getProjectInfo(String path) {
    final Project project = getProject();
    GwtVirtualFile virtualFile = GwtVirtualFileUtil.createVirtualFile(project, project.getBaseDir());
    final List<String> moduleFileUrls = new ArrayList<String>();
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
          String moduleDirUrl = module.getModuleDirUrl();
          if (moduleDirUrl != null) {
            moduleFileUrls.add(moduleDirUrl);
          }
        }
      }
    });
    return new GwtProjectInfo(project.getName(), virtualFile, moduleFileUrls);
  }

  @Override
  public GwtVirtualFile findFileByUrl(String fileUrl) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl == null) {
      return null;
    }
    return GwtVirtualFileUtil.createVirtualFile(getProject(), fileByUrl);
  }

  @Nullable
  @Override
  public GwtNavigateInfo getNavigationInfo(String fileUrl, final int offset) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl == null) {
      return null;
    }

    final GwtTextRange[] range = new GwtTextRange[1];
    final String[] text = new String[1];
    final List<GwtNavigatable> navigatables = new ArrayList<GwtNavigatable>();
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      @RequiredReadAction
      public void run() {
        Project project = getProject();
        PsiFile file = PsiManager.getInstance(project).findFile(fileByUrl);
        assert file != null;

        final CtrlMouseHandler.Info infoAt =
                CtrlMouseHandler.getInfoAt(findEditor(project, fileByUrl, offset), file, offset, CtrlMouseHandler.BrowseMode.Declaration);

        text[0] = infoAt == null ? null : infoAt.getInfo().text;
        PsiReference referenceAt = file.findReferenceAt(offset);
        if (referenceAt != null) {
          List<TextRange> absoluteRanges = ReferenceRange.getAbsoluteRanges(referenceAt);
          if (absoluteRanges.isEmpty()) {
            return;
          }
          range[0] = new GwtTextRange(absoluteRanges.get(0).getStartOffset(), absoluteRanges.get(0).getEndOffset());

          PsiElement resolvedElement = referenceAt.resolve();
          if (resolvedElement != null) {
            PsiElement navigationElement = resolvedElement.getNavigationElement();
            if (navigationElement == null) {
              navigationElement = resolvedElement;
            }

            VirtualFile virtualFile = navigationElement.getContainingFile().getVirtualFile();
            assert virtualFile != null;
            navigatables.add(new GwtNavigatable(GwtVirtualFileUtil.createVirtualFile(project, virtualFile), navigationElement.getTextOffset()));
          }
        }
      }
    });
    if (range[0] == null || navigatables.isEmpty()) {
      return null;
    }
    return new GwtNavigateInfo(text[0], range[0], navigatables);
  }

  @NotNull
  @Override
  public GwtEditorColorScheme serviceEditorColorScheme(String scheme, String[] colorKeys, String[] attributes) {
    GwtEditorColorScheme gwtScheme = new GwtEditorColorScheme(scheme);

    final EditorColorsManager colorsManager = EditorColorsManager.getInstance();

    EditorColorsScheme globalScheme = colorsManager.getScheme(scheme);
    if (globalScheme != null) {
      final EditorColorsScheme finalGlobalScheme = globalScheme;
      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        @Override
        public void run() {
          colorsManager.setGlobalScheme(finalGlobalScheme);
        }
      }, ModalityState.any());
    }

    if (globalScheme == null) {
      globalScheme = colorsManager.getGlobalScheme();
    }

    for (String colorKey : colorKeys) {
      ColorKey key = ColorKey.find(colorKey);
      assert key != null;

      gwtScheme.putColor(colorKey, createColor(globalScheme.getColor(key)));
    }
    for (String attribute : attributes) {
      TextAttributesKey textAttributesKey = TextAttributesKey.find(attribute);

      TextAttributes textAttributes = globalScheme.getAttributes(textAttributesKey);
      if (textAttributes != null) {
        gwtScheme.putAttributes(attribute, createTextAttributes(textAttributes));
      }
    }
    return gwtScheme;
  }

  @NotNull
  @Override
  public List<String> serviceEditorColorSchemeList() {
    List<String> list = new ArrayList<String>();
    EditorColorsScheme[] allSchemes = EditorColorsManager.getInstance().getAllSchemes();
    for (EditorColorsScheme allScheme : allSchemes) {
      list.add(allScheme.getName());
    }

    return list;
  }

  @Override
  public String getContent(final String fileUrl) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl != null) {
      if (fileByUrl.isDirectory()) {
        return null;
      }
      BinaryFileDecompiler binaryFileDecompiler = null;
      FileType fileType = fileByUrl.getFileType();
      if (fileType.isBinary()) {
        binaryFileDecompiler = BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType);
        if (binaryFileDecompiler == null) {
          return null;
        }
      }

      if (binaryFileDecompiler != null) {
        return binaryFileDecompiler.decompile(fileByUrl).toString();
      }

      return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
        @Override
        public String compute() {
          return getFileText(getProject(), fileByUrl).toString();
        }
      });
    }
    return null;
  }

  @NotNull
  @Override
  public List<GwtHighlightInfo> getLexerHighlight(String fileUrl) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl != null) {
      if (fileByUrl.isDirectory()) {
        return Collections.emptyList();
      }
      return ApplicationManager.getApplication().runReadAction(new Computable<List<GwtHighlightInfo>>() {
        @Override
        public List<GwtHighlightInfo> compute() {
          List<GwtHighlightInfo> list = new ArrayList<GwtHighlightInfo>();
          Project project = getProject();

          EditorHighlighter highlighter = HighlighterFactory.createHighlighter(project, fileByUrl);
          highlighter.setText(getFileText(project, fileByUrl));

          HighlighterIterator iterator = highlighter.createIterator(0);
          while (!iterator.atEnd()) {
            int start = iterator.getStart();
            int end = iterator.getEnd();
            TextAttributes textAttributes = iterator.getTextAttributes();

            GwtHighlightInfo highlightInfo = createHighlightInfo(textAttributes, new GwtTextRange(start, end), 0);
            if (!highlightInfo.isEmpty()) {
              list.add(highlightInfo);
            }
            iterator.advance();
          }
          return list;
        }
      });
    }

    return Collections.emptyList();
  }

  @RequiredReadAction
  private static CharSequence getFileText(Project project, VirtualFile virtualFile) {
    FileType fileType = virtualFile.getFileType();
    if (fileType.isBinary()) {
      BinaryFileDecompiler binaryFileDecompiler = BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType);
      if (binaryFileDecompiler != null) {
        return binaryFileDecompiler.decompile(virtualFile);
      }
    }
    PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
    assert file != null;
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    assert document != null;
    return document.getText();
  }


  @NotNull
  public static GwtHighlightInfo createHighlightInfo(TextAttributes textAttributes, GwtTextRange textRange, int severity) {
    return new GwtHighlightInfo(createTextAttributes(textAttributes), textRange, severity);
  }

  @NotNull
  public static GwtTextAttributes createTextAttributes(TextAttributes textAttributes) {
    GwtColor foreground = null;
    GwtColor background = null;

    Color foregroundColor = textAttributes.getForegroundColor();
    if (foregroundColor != null) {
      foreground = createColor(foregroundColor);
    }

    Color backgroundColor = textAttributes.getBackgroundColor();
    if (backgroundColor != null) {
      background = createColor(backgroundColor);
    }

    int flags = 0;
    flags = BitUtil.set(flags, GwtTextAttributes.BOLD, (textAttributes.getFontType() & Font.BOLD) != 0);
    flags = BitUtil.set(flags, GwtTextAttributes.ITALIC, (textAttributes.getFontType() & Font.ITALIC) != 0);
    flags = BitUtil.set(flags, GwtTextAttributes.UNDERLINE, textAttributes.getEffectType() == EffectType.LINE_UNDERSCORE);
    flags = BitUtil.set(flags, GwtTextAttributes.LINE_THROUGH, textAttributes.getEffectType() == EffectType.STRIKEOUT);

    return new GwtTextAttributes(foreground, background, flags);
  }

  @NotNull
  private static GwtColor createColor(Color color) {
    return new GwtColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  @NotNull
  @Override
  public List<GwtHighlightInfo> runHighlightPasses(String fileUrl, final int offset) {
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    if (fileByUrl != null) {
      if (fileByUrl.isDirectory() || fileByUrl.getFileType().isBinary()) {
        return Collections.emptyList();
      }
      IdentifierHighlighterPassFactory.ourTestingIdentifierHighlighting = true;
      return ApplicationManager.getApplication().runReadAction(new Computable<List<GwtHighlightInfo>>() {
        @Override
        public List<GwtHighlightInfo> compute() {
          final List<GwtHighlightInfo> list = new ArrayList<GwtHighlightInfo>();
          final Project project = getProject();
          final PsiFile file = PsiManager.getInstance(project).findFile(fileByUrl);
          if (file == null) {
            return Collections.emptyList();
          }
          try {

            SwingUtilities.invokeAndWait(new Runnable() {
              @Override
              public void run() {
                Editor editor = findEditor(project, fileByUrl, offset);
                DaemonCodeAnalyzerImpl analyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzerEx.getInstanceEx(project);
                TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
                List<HighlightInfo> highlightInfos =
                        analyzer.runPasses(file, PsiDocumentManager.getInstance(project).getDocument(file), textEditor, new int[0], false, null);

                for (HighlightInfo highlightInfo : highlightInfos) {
                  TextAttributes textAttributes = highlightInfo.getTextAttributes(null, null);
                  if (textAttributes == null) {
                    continue;
                  }
                  GwtHighlightInfo info = createHighlightInfo(textAttributes, new GwtTextRange(highlightInfo.getStartOffset(), highlightInfo.getEndOffset()),
                                                              highlightInfo.getSeverity().myVal);
                  if (highlightInfo.getSeverity() != HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY) {
                    info.setTooltip(highlightInfo.getToolTip());
                  }
                  list.add(info);
                }
              }
            });
          }
          catch (Exception e) {
            e.printStackTrace();
          }

          return list;
        }
      });
    }
    return Collections.emptyList();
  }

  @NotNull
  private static Editor findEditor(Project project, VirtualFile fileByUrl, final int offset) {
    final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    final FileEditor[] editors = fileEditorManager.getEditors(fileByUrl);
    for (FileEditor fileEditor : editors) {
      if (fileEditor instanceof TextEditor) {
        final Editor editor = ((TextEditor)fileEditor).getEditor();
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            editor.getCaretModel().moveToOffset(offset);
          }
        });
        return editor;
      }
    }

    final Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, fileByUrl, offset), false);
    assert editor != null;
    return editor;
  }
}
