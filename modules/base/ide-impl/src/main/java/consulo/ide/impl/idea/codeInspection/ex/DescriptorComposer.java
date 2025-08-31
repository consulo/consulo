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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class DescriptorComposer extends HTMLComposerBase {
  private static final Logger LOG = Logger.getInstance(DescriptorComposer.class);
  private final InspectionToolPresentation myTool;

  public DescriptorComposer(@Nonnull InspectionToolPresentation tool) {
    myTool = tool;
  }

  @RequiredReadAction
  @Override
  public void compose(StringBuffer buf, RefEntity refEntity) {
    genPageHeader(buf, refEntity);
    if (myTool.getDescriptions(refEntity) != null) {
      appendHeading(buf, InspectionLocalize.inspectionProblemSynopsis());

      CommonProblemDescriptor[] descriptions = myTool.getDescriptions(refEntity);

      LOG.assertTrue(descriptions != null);

      startList(buf);
      for (int i = 0; i < descriptions.length; i++) {
        CommonProblemDescriptor description = descriptions[i];

        startListItem(buf);
        composeDescription(description, i, buf, refEntity);
        doneListItem(buf);
      }

      doneList(buf);

      appendResolution(buf,refEntity, quickFixTexts(refEntity, myTool));
    }
    else {
      appendNoProblems(buf);
    }
  }

  public static String[] quickFixTexts(RefEntity where, @Nonnull InspectionToolPresentation toolPresentation){
    QuickFixAction[] quickFixes = toolPresentation.getQuickFixes(new RefEntity[] {where});
    if (quickFixes == null) {
      return null;
    }
    List<String> texts = new ArrayList<>();
    for (QuickFixAction quickFix : quickFixes) {
      String text = quickFix.getText(where);
      if (text == null) continue;
      texts.add(text);
    }
    return texts.toArray(new String[texts.size()]);
  }

  protected void composeAdditionalDescription(@Nonnull StringBuffer buf, @Nonnull RefEntity refEntity) {}

  @RequiredReadAction
  @Override
  public void compose(StringBuffer buf, RefEntity refElement, CommonProblemDescriptor descriptor) {
    CommonProblemDescriptor[] descriptions = myTool.getDescriptions(refElement);

    int problemIdx = 0;
    if (descriptions != null) { //server-side inspections
      problemIdx = -1;
      for (int i = 0; i < descriptions.length; i++) {
        CommonProblemDescriptor description = descriptions[i];
        if (description == descriptor) {
          problemIdx = i;
          break;
        }
      }
      if (problemIdx == -1) return;
    }

    genPageHeader(buf, refElement);
    appendHeading(buf, InspectionLocalize.inspectionProblemSynopsis());
    //noinspection HardCodedStringLiteral
    buf.append("<br>");
    appendAfterHeaderIndention(buf);

    composeDescription(descriptor, problemIdx, buf, refElement);

    if (refElement instanceof RefElement && !refElement.isValid()) return;

    QuickFix[] fixes = descriptor.getFixes();
    if (fixes != null && fixes.length > 0) {
      //noinspection HardCodedStringLiteral
      buf.append("<br><br>");
      appendHeading(buf, InspectionLocalize.inspectionProblemResolution());
      //noinspection HardCodedStringLiteral
      buf.append("<br>");
      appendAfterHeaderIndention(buf);

      int idx = 0;
      for (QuickFix fix : fixes) {
        //noinspection HardCodedStringLiteral
        //noinspection HardCodedStringLiteral
        buf.append("<a HREF=\"file://bred.txt#invokelocal:").append(idx++);
        buf.append("\">");
        buf.append(fix.getName());
        //noinspection HardCodedStringLiteral
        buf.append("</a>");
        //noinspection HardCodedStringLiteral
        buf.append("<br>");
        appendAfterHeaderIndention(buf);
      }
    }
  }

  @RequiredReadAction
  protected void composeDescription(@Nonnull CommonProblemDescriptor description, int i, @Nonnull StringBuffer buf, @Nonnull RefEntity refElement) {
    PsiElement expression = description instanceof ProblemDescriptor ? ((ProblemDescriptor)description).getPsiElement() : null;
    StringBuilder anchor = new StringBuilder();
    VirtualFile vFile = null;

    if (expression != null) {
      vFile = expression.getContainingFile().getVirtualFile();
      if (vFile instanceof VirtualFileWindow) vFile = ((VirtualFileWindow)vFile).getDelegate();

      //noinspection HardCodedStringLiteral
      anchor.append("<a HREF=\"");
      try {
        if (myExporter == null){
          //noinspection HardCodedStringLiteral
          anchor.append(new URL(vFile.getUrl() + "#descr:" + i));
        }
        else {
          anchor.append(myExporter.getURL(refElement));
        }
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }

      anchor.append("\">");
      anchor.append(ProblemDescriptorUtil.extractHighlightedText(description, expression).replaceAll("\\$", "\\\\\\$"));
      //noinspection HardCodedStringLiteral
      anchor.append("</a>");
    }
    else {
      //noinspection HardCodedStringLiteral
      anchor.append("<font style=\"font-weight:bold; color:#FF0000\";>");
      anchor.append(InspectionLocalize.inspectionExportResultsInvalidatedItem());
      //noinspection HardCodedStringLiteral
      anchor.append("</font>");
    }

    String descriptionTemplate = description.getDescriptionTemplate();
    //noinspection HardCodedStringLiteral
    String reference = "#ref";
    boolean containsReference = descriptionTemplate.contains(reference);
    String res = descriptionTemplate.replaceAll(reference, anchor.toString());
    int lineNumber = description instanceof ProblemDescriptor ? ((ProblemDescriptor)description).getLineNumber() : -1;
    StringBuffer lineAnchor = new StringBuffer();
    if (expression != null && lineNumber > 0) {
      Document doc = FileDocumentManager.getInstance().getDocument(vFile);
      lineAnchor.append(InspectionLocalize.inspectionExportResultsAtLine()).append(" ");
      if (myExporter == null) {
        //noinspection HardCodedStringLiteral
        lineAnchor.append("<a HREF=\"");
        try {
          int offset = doc.getLineStartOffset(lineNumber - 1);
          offset = CharArrayUtil.shiftForward(doc.getCharsSequence(), offset, " \t");
          lineAnchor.append(new URL(vFile.getUrl() + "#" + offset));
        }
        catch (MalformedURLException e) {
          LOG.error(e);
        }
        lineAnchor.append("\">");
      }
      lineAnchor.append(Integer.toString(lineNumber));
      //noinspection HardCodedStringLiteral
      lineAnchor.append("</a>");
      //noinspection HardCodedStringLiteral
      String location = "#loc";
      if (!res.contains(location)) {
        res += " (" + location + ")";
      }
      res = res.replaceAll(location, lineAnchor.toString());
    }
    buf.append(res);
    buf.append(BR).append(BR);
    composeAdditionalDescription(buf, refElement);
  }


}
