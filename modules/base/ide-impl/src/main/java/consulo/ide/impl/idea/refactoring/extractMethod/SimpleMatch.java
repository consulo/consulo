package consulo.ide.impl.idea.refactoring.extractMethod;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
* User: ktisha
*/
public class SimpleMatch {
  PsiElement myStartElement;
  PsiElement myEndElement;
  private final Map<String, String> myChangedParameters;
  private String myChangedOutput;

  public SimpleMatch(@Nonnull final PsiElement start, @Nonnull final PsiElement endElement) {
    myStartElement = start;
    myEndElement = endElement;
    myChangedParameters = new HashMap<String, String>();
  }

  public PsiElement getStartElement() {
    return myStartElement;
  }

  public PsiElement getEndElement() {
    return myEndElement;
  }

  public Map<String, String> getChangedParameters() {
    return myChangedParameters;
  }

  public void changeParameter(@Nonnull final String from, @Nonnull final String to) {
    myChangedParameters.put(from, to);
  }

  public void changeOutput(@Nonnull final String to) {
    myChangedOutput = to;
  }

  public String getChangedOutput() {
    return myChangedOutput;
  }

}
