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
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 22, 2001
 * Time: 4:54:17 PM
 * To change template for new interface use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.module.content.util.ProjectUtilCore;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
public abstract class HTMLComposerBase extends HTMLComposer {
  public static final String BR = "<br>";
  public static final String NBSP = "&nbsp;";
  public static final String CODE_CLOSING = "</code>";
  public static final String CODE_OPENING = "<code>";
  public static final String B_OPENING = "<b>";
  public static final String B_CLOSING = "</b>";
  public static final String CLOSE_TAG = "\">";
  public static final String A_HREF_OPENING = "<a HREF=\"";
  public static final String A_CLOSING = "</a>";

  public HTMLExporter myExporter;
  private final int[] myListStack;
  private int myListStackTop;
  private final Map<Key, HTMLComposerExtension> myExtensions = new HashMap<>();
  private final Map<Language, HTMLComposerExtension> myLanguageExtensions = new HashMap<>();

  protected HTMLComposerBase() {
    myListStack = new int[5];
    myListStackTop = -1;
    for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
      final HTMLComposerExtension extension = factory.createHTMLComposerExtension(this);
      if (extension != null) {
        myExtensions.put(extension.getID(), extension);
        myLanguageExtensions.put(extension.getLanguage(), extension);
      }
    }
  }

  @RequiredReadAction
  public abstract void compose(StringBuffer buf, RefEntity refEntity);

  public void compose(StringBuffer buf, RefEntity refElement, CommonProblemDescriptor descriptor) {}

  @RequiredReadAction
  public void composeWithExporter(StringBuffer buf, RefEntity refEntity, HTMLExporter exporter) {
    myExporter = exporter;
    compose(buf, refEntity);
    myExporter = null;
  }

  @RequiredReadAction
  protected void genPageHeader(final StringBuffer buf, RefEntity refEntity) {
    if (refEntity instanceof RefElement) {
      RefElement refElement = (RefElement)refEntity;

      appendHeading(buf, InspectionLocalize.inspectionOfflineViewToolDisplayNameTitle());
      buf.append(BR);
      appendAfterHeaderIndention(buf);

      appendShortName(buf, refElement);
      buf.append(BR).append(BR);

      appendHeading(buf, InspectionLocalize.inspectionExportResultsCapitalizedLocation());
      buf.append(BR);
      appendAfterHeaderIndention(buf);
      appendLocation(buf, refElement);
      buf.append(BR).append(BR);
    }
  }

  @RequiredReadAction
  private void appendLocation(final StringBuffer buf, final RefElement refElement) {
    final HTMLComposerExtension extension = getLanguageExtension(refElement);
    if (extension != null) {
      extension.appendLocation(refElement, buf);
    }
    if (refElement instanceof RefFile){
      buf.append(InspectionLocalize.inspectionExportResultsFile().get());
      buf.append(NBSP);
      appendElementReference(buf, refElement, false);
    }
  }

  @Nullable
  @RequiredReadAction
  private HTMLComposerExtension getLanguageExtension(final RefElement refElement) {
    final PsiElement element = refElement.getPsiElement();
    return element != null ? myLanguageExtensions.get(element.getLanguage()) : null;
  }

  @RequiredReadAction
  private void appendShortName(final StringBuffer buf, RefElement refElement) {
    final HTMLComposerExtension extension = getLanguageExtension(refElement);
    if (extension != null) {
      extension.appendShortName(refElement, buf);
    } else {
      refElement.accept(new RefVisitor() {
        @RequiredReadAction
        @Override public void visitFile(@Nonnull RefFile file) {
          final PsiFile psiFile = file.getPsiElement();
          if (psiFile != null) {
            buf.append(B_OPENING);
            buf.append(psiFile.getName());
            buf.append(B_CLOSING);
          }
        }
      });
    }
  }

  @RequiredReadAction
  public void appendQualifiedName(StringBuffer buf, RefEntity refEntity) {
    if (refEntity == null) return;
    String qName = "";

    while (!(refEntity instanceof RefProject)) {
      if (qName.length() > 0) qName = "." + qName;

      String name = refEntity.getName();
      if (refEntity instanceof RefElement) {
        final HTMLComposerExtension extension = getLanguageExtension((RefElement)refEntity);
        if (extension != null) {
          name = extension.getQualifiedName(refEntity);
        }
      }

      qName = name + qName;
      refEntity = refEntity.getOwner();
    }

    buf.append(qName);
  }

  @Override
  @RequiredReadAction
  public void appendElementReference(final StringBuffer buf, RefElement refElement) {
    appendElementReference(buf, refElement, true);
  }

  @Override
  public void appendElementReference(final StringBuffer buf, RefElement refElement, String linkText, @NonNls String frameName) {
    final String url = refElement.getURL();
    if (url != null) {
      appendElementReference(buf, url, linkText, frameName);
    }
  }

  @Override
  public void appendElementReference(final StringBuffer buf, String url, String linkText, @NonNls String frameName) {
    buf.append(A_HREF_OPENING);
    buf.append(url);
    if (frameName != null) {
      @NonNls final String target = "\" target=\"";
      buf.append(target);
      buf.append(frameName);
    }

    buf.append("\">");
    buf.append(linkText);
    buf.append(A_CLOSING);
  }

  protected void appendQuickFix(@NonNls final StringBuffer buf, String text, int index) {
    if (myExporter == null) {
      buf.append("<a HREF=\"file://bred.txt#invoke:").append(index);
      buf.append("\">");
      buf.append(text);
      buf.append("</a>");
    }
  }

  @Override
  @RequiredReadAction
  public void appendElementReference(final StringBuffer buf, RefElement refElement, boolean isPackageIncluded) {
    final HTMLComposerExtension extension = getLanguageExtension(refElement);

    if (extension != null) {
      extension.appendReferencePresentation(refElement, buf, isPackageIncluded);
    }
    else if (refElement instanceof RefFile) {
      buf.append(A_HREF_OPENING);

      buf.append(refElement.getURL());

      buf.append("\">");
      String refElementName = refElement.getName();
      final PsiElement element = refElement.getPsiElement();
      if (element != null) {
        VirtualFile file = PsiUtilCore.getVirtualFile(element);
        if (file != null) {
          refElementName =
            ProjectUtilCore.displayUrlRelativeToProject(file, file.getPresentableUrl(), element.getProject(), true, false);
        }
      }
      buf.append(refElementName);
      buf.append(A_CLOSING);
    }
  }

  @Override
  public String composeNumereables(int n, String statement, String singleEnding, String multipleEnding) {
    final StringBuilder buf = new StringBuilder();
    buf.append(n);
    buf.append(' ');
    buf.append(statement);

    if (n % 10 == 1 && n % 100 != 11) {
      buf.append(singleEnding);
    }
    else {
      buf.append(multipleEnding);
    }
    return buf.toString();
  }

  @Override
  @RequiredReadAction
  public void appendElementInReferences(StringBuffer buf, RefElement refElement) {
    if (refElement.getInReferences().size() > 0) {
      appendHeading(buf, InspectionLocalize.inspectionExportResultsUsedFrom());
      startList(buf);
      for (RefElement refCaller : refElement.getInReferences()) {
        appendListItem(buf, refCaller);
      }
      doneList(buf);
    }
  }

  @Override
  @RequiredReadAction
  public void appendElementOutReferences(StringBuffer buf, RefElement refElement) {
    if (refElement.getOutReferences().size() > 0) {
      buf.append(BR);
      appendHeading(buf, InspectionLocalize.inspectionExportResultsUses());
      startList(buf);
      for (RefElement refCallee : refElement.getOutReferences()) {
        appendListItem(buf, refCallee);
      }
      doneList(buf);
    }
  }

  @Override
  @RequiredReadAction
  public void appendListItem(StringBuffer buf, RefElement refElement) {
    startListItem(buf);
    buf.append(CLOSE_TAG);
    appendElementReference(buf, refElement, true);
    appendAdditionalListItemInfo(buf, refElement);
    doneListItem(buf);
  }

  protected void appendAdditionalListItemInfo(StringBuffer buf, RefElement refElement) {
    // Default appends nothing.
  }

  protected void appendResolution(StringBuffer buf, RefEntity where, String[] quickFixes) {
    if (myExporter != null) return;
    if (where instanceof RefElement && !where.isValid()) return;
    if (quickFixes != null) {
      boolean listStarted = false;
      for (int i = 0; i < quickFixes.length; i++) {
        final String text = quickFixes[i];
        if (text == null) continue;
        if (!listStarted) {
          appendHeading(buf, InspectionLocalize.inspectionProblemResolution());
          startList(buf);
          listStarted = true;
        }
        startListItem(buf);
        appendQuickFix(buf, text, i);
        doneListItem(buf);
      }

      if (listStarted) {
        doneList(buf);
      }
    }
  }


  @Override
  public void startList(@NonNls final StringBuffer buf) {
    buf.append("<ul>");
    myListStackTop++;
    myListStack[myListStackTop] = 0;
  }

  @Override
  public void doneList(@NonNls StringBuffer buf) {
    buf.append("</ul>");
    if (myListStack[myListStackTop] != 0) {
      buf.append("<table cellpadding=\"0\" border=\"0\" cellspacing=\"0\"><tr><td>&nbsp;</td></tr></table>");
    }
    myListStackTop--;
  }

  @Override
  public void startListItem(@NonNls StringBuffer buf) {
    myListStack[myListStackTop]++;
    buf.append("<li>");
  }

  public static void doneListItem(@NonNls StringBuffer buf) {
    buf.append("</li>");
  }

  @Override
  public void appendNoProblems(StringBuffer buf) {
    buf.append(BR);
    appendAfterHeaderIndention(buf);
    buf.append(B_OPENING);
    buf.append(InspectionLocalize.inspectionExportResultsNoProblemsFound());
    buf.append(B_CLOSING).append(BR);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getExtension(final Key<T> key) {
    return (T)myExtensions.get(key);
  }
}
