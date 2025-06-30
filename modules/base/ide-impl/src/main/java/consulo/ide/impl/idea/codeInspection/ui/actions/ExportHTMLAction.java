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

package consulo.ide.impl.idea.codeInspection.ui.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.ide.impl.idea.codeEditor.printing.ExportToHTMLSettings;
import consulo.ide.impl.idea.codeInspection.InspectionApplication;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.export.ExportToHTMLDialog;
import consulo.ide.impl.idea.codeInspection.export.HTMLExportFrameMaker;
import consulo.ide.impl.idea.codeInspection.export.HTMLExportUtil;
import consulo.ide.impl.idea.codeInspection.ui.InspectionNode;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.codeInspection.ui.InspectionTreeNode;
import consulo.ide.impl.idea.codeInspection.util.RefEntityAlphabeticalComparator;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.inspection.HTMLExporter;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefModule;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.Tools;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.localize.LocalizeValue;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.lang.Comparing;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;
import org.jdom.Document;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author anna
 * @since 2006-01-11
 */
public class ExportHTMLAction extends AnAction implements DumbAware {
  private final InspectionResultsView myView;
   private static final String PROBLEMS = "problems";
  private static final String HTML = "HTML";
  private static final String XML = "XML";

  public ExportHTMLAction(InspectionResultsView view) {
    super(
      InspectionLocalize.inspectionActionExportHtml(),
      LocalizeValue.empty(),
      AllIcons.Actions.Export
    );
    myView = view;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ListPopup popup = JBPopupFactory.getInstance().createListPopup(
      new BaseListPopupStep<String>(InspectionLocalize.inspectionActionExportPopupTitle().get(), new String[]{HTML, XML}) {
        @Override
        public PopupStep onChosen(String selectedValue, boolean finalChoice) {
          return doFinalStep(() -> exportHTML(Comparing.strEqual(selectedValue, HTML)));
        }
      });
    InspectionResultsView.showPopup(e, popup);
  }

  private void exportHTML(boolean exportToHTML) {
    ExportToHTMLDialog exportToHTMLDialog = new ExportToHTMLDialog(myView.getProject(), exportToHTML);
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myView.getProject());
    if (exportToHTMLSettings.OUTPUT_DIRECTORY == null) {
      exportToHTMLSettings.OUTPUT_DIRECTORY = ContainerPathManager.get().getHomePath() + File.separator + "inspections";
    }
    exportToHTMLDialog.reset();
    exportToHTMLDialog.show();
    if (!exportToHTMLDialog.isOK()) {
      return;
    }
    exportToHTMLDialog.apply();

    String outputDirectoryName = exportToHTMLSettings.OUTPUT_DIRECTORY;
    ApplicationManager.getApplication().invokeLater(() -> {
      Runnable exportRunnable = () -> {
        if (!exportToHTML) {
          dupm2XML(outputDirectoryName);
        } else {
          HTMLExportFrameMaker maker = new HTMLExportFrameMaker(outputDirectoryName, myView.getProject());
          maker.start();
          try {
            InspectionTreeNode root = myView.getTree().getRoot();
            TreeUtil.traverse(root, node -> {
              if (node instanceof InspectionNode) {
                ApplicationManager.getApplication().runReadAction(() -> exportHTML(maker, (InspectionNode)node));
              }
              return true;
            });
          }
          catch (ProcessCanceledException e) {
            // Do nothing here.
          }

          maker.done();
        }
      };

      if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
        exportRunnable,
        exportToHTML
          ? InspectionLocalize.inspectionGeneratingHtmlProgressTitle().get()
          : InspectionLocalize.inspectionGeneratingXmlProgressTitle().get(),
        true,
        myView.getProject()
      )) {
        return;
      }

      if (exportToHTML && exportToHTMLSettings.OPEN_IN_BROWSER) {
        BrowserUtil.browse(new File(exportToHTMLSettings.OUTPUT_DIRECTORY, "index.html"));
      }
    });
  }

  private void dupm2XML(String outputDirectoryName) {
    try {
      new File(outputDirectoryName).mkdirs();
      InspectionTreeNode root = myView.getTree().getRoot();
      IOException[] ex = new IOException[1];
      TreeUtil.traverse(root, node -> {
        if (node instanceof InspectionNode toolNode) {
          Element problems = new Element(PROBLEMS);
          InspectionToolWrapper toolWrapper = toolNode.getToolWrapper();

          Set<InspectionToolWrapper> toolWrappers = getWorkedTools(toolNode);
          for (InspectionToolWrapper wrapper : toolWrappers) {
            InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(wrapper);
            presentation.exportResults(problems);
          }
          ProjectPathMacroManager.getInstance(myView.getProject()).collapsePaths(problems);
          try {
            JDOMUtil.writeDocument(
              new Document(problems),
              outputDirectoryName + File.separator + toolWrapper.getShortName() + InspectionApplication.XML_EXTENSION,
              CodeStyleSettingsManager.getSettings(null).getLineSeparator()
            );
          }
          catch (IOException e) {
            ex[0] = e;
          }
        }
        return true;
      });
      if (ex[0] != null) {
        throw ex[0];
      }
      Element element = new Element(InspectionApplication.INSPECTIONS_NODE);
      String profileName = myView.getCurrentProfileName();
      if (profileName != null) {
        element.setAttribute(InspectionApplication.PROFILE, profileName);
      }
      JDOMUtil.writeDocument(
        new Document(element),
        outputDirectoryName + File.separator + InspectionApplication.DESCRIPTIONS + InspectionApplication.XML_EXTENSION,
        CodeStyleSettingsManager.getSettings(null).getLineSeparator()
      );
    }
    catch (IOException e) {
      SwingUtilities.invokeLater(() -> Messages.showErrorDialog(myView, e.getMessage()));
    }
  }

  @Nonnull
  private Set<InspectionToolWrapper> getWorkedTools(@Nonnull InspectionNode node) {
    Set<InspectionToolWrapper> result = new HashSet<>();
    InspectionToolWrapper wrapper = node.getToolWrapper();
    if (myView.getCurrentProfileName() != null){
      result.add(wrapper);
      return result;
    }
    String shortName = wrapper.getShortName();
    GlobalInspectionContextImpl context = myView.getGlobalInspectionContext();
    Tools tools = context.getTools().get(shortName);
    if (tools != null) {   //dummy entry points tool
      for (ScopeToolState state : tools.getTools()) {
        InspectionToolWrapper toolWrapper = state.getTool();
        result.add(toolWrapper);
      }
    }
    return result;
  }

  @RequiredReadAction
  private void exportHTML(HTMLExportFrameMaker frameMaker, InspectionNode node) {
    final Set<InspectionToolWrapper> toolWrappers = getWorkedTools(node);
    InspectionToolWrapper toolWrapper = node.getToolWrapper();

    final HTMLExporter exporter =
      new HTMLExporter(frameMaker.getRootFolder() + "/" + toolWrapper.getShortName(), myView.getGlobalInspectionContext().getPresentation(toolWrapper).getComposer());
    frameMaker.startInspection(toolWrapper);
    HTMLExportUtil.runExport(myView.getProject(), new ThrowableRunnable<>() {
      @Override
      @RequiredReadAction
      public void run() throws IOException {
        exportHTML(toolWrappers, exporter);
        exporter.generateReferencedPages();
      }
    });
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @RequiredReadAction
  private void exportHTML(@Nonnull Set<InspectionToolWrapper> toolWrappers, HTMLExporter exporter) throws IOException {
    StringBuffer packageIndex = new StringBuffer();
    packageIndex.append("<html><body>");

    Map<String, Set<RefEntity>> content = new HashMap<>();

    for (InspectionToolWrapper toolWrapper : toolWrappers) {
      InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(toolWrapper);
      Map<String, Set<RefEntity>> toolContent = presentation.getContent();
      if (toolContent != null) {
        content.putAll(toolContent);
      }
    }

    Set<RefEntity> defaultPackageEntities = content.remove(null);
    if (defaultPackageEntities != null) {
      content.put("default package" , defaultPackageEntities);
    }

    List<String> packageNames = new ArrayList<>(content.keySet());

    Collections.sort(packageNames);
    for (String packageName : packageNames) {
      appendPackageReference(packageIndex, packageName);
      List<RefEntity> packageContent = new ArrayList<>(content.get(packageName));
      Collections.sort(packageContent, RefEntityAlphabeticalComparator.getInstance());
      StringBuffer contentIndex = new StringBuffer();
      contentIndex.append("<html><body>");
      for (RefEntity refElement : packageContent) {
        refElement = refElement.getRefManager().getRefinedElement(refElement);
        contentIndex.append("<a HREF=\"");
        contentIndex.append(exporter.getURL(refElement));
        contentIndex.append("\" target=\"elementFrame\">");
        contentIndex.append(refElement.getName());
        contentIndex.append("</a><br>");

        exporter.createPage(refElement);
      }

      contentIndex.append("</body></html>");
      HTMLExportUtil.writeFile(exporter.getRootFolder(), packageName + "-index.html", contentIndex, myView.getProject());
    }

    Set<RefModule> modules = new HashSet<>();
    for (InspectionToolWrapper toolWrapper : toolWrappers) {
      InspectionToolPresentation presentation = myView.getGlobalInspectionContext().getPresentation(toolWrapper);
      Set<RefModule> problems = presentation.getModuleProblems();
      if (problems != null) {
        modules.addAll(problems);
      }
    }

    List<RefModule> sortedModules = new ArrayList<>(modules);
    Collections.sort(sortedModules, RefEntityAlphabeticalComparator.getInstance());
    for (RefModule module : sortedModules) {
      appendPackageReference(packageIndex, module.getName());
      StringBuffer contentIndex = new StringBuffer();
      contentIndex.append("<html><body>");

      contentIndex.append("<a HREF=\"");
      contentIndex.append(exporter.getURL(module));
      contentIndex.append("\" target=\"elementFrame\">");
      contentIndex.append(module.getName());
      contentIndex.append("</a><br>");
      exporter.createPage(module);

      contentIndex.append("</body></html>");
      HTMLExportUtil.writeFile(exporter.getRootFolder(), module.getName() + "-index.html", contentIndex, myView.getProject());
    }

    packageIndex.append("</body></html>");

    HTMLExportUtil.writeFile(exporter.getRootFolder(), "index.html", packageIndex, myView.getProject());
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static void appendPackageReference(StringBuffer packageIndex, String packageName) {
    packageIndex.append("<a HREF=\"");
    packageIndex.append(packageName);
    packageIndex.append("-index.html\" target=\"packageFrame\">");
    packageIndex.append(packageName);
    packageIndex.append("</a><br>");
  }

}
