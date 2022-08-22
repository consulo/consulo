package consulo.language.editor.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import jakarta.inject.Singleton;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DocumentationManagerUtil {
  public static DocumentationManagerUtil getInstance() {
    return Application.get().getInstance(DocumentationManagerUtil.class);
  }

  protected void createHyperlinkImpl(StringBuilder buffer, PsiElement refElement, String refText, String label, boolean plainLink) {
    buffer.append("<a href=\"");
    buffer.append(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL); // :-)
    buffer.append(refText);
    buffer.append("\">");
    if (!plainLink) {
      buffer.append("<code>");
    }
    buffer.append(label);
    if (!plainLink) {
      buffer.append("</code>");
    }
    buffer.append("</a>");
  }

  public static void createHyperlink(StringBuilder buffer, String refText, String label, boolean plainLink) {
    getInstance().createHyperlinkImpl(buffer, null, refText, label, plainLink);
  }

  public static void createHyperlink(StringBuilder buffer, PsiElement refElement, String refText, String label, boolean plainLink) {
    getInstance().createHyperlinkImpl(buffer, refElement, refText, label, plainLink);
  }
}
