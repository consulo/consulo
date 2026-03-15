package consulo.language.editor.refactoring.extractMethod;

import consulo.language.psi.PsiElement;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ktisha
 */
public class SimpleMatch {
  PsiElement myStartElement;
  PsiElement myEndElement;
  private final Map<String, String> myChangedParameters;
  private String myChangedOutput;

  public SimpleMatch(PsiElement start, PsiElement endElement) {
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

  public void changeParameter(String from, String to) {
    myChangedParameters.put(from, to);
  }

  public void changeOutput(String to) {
    myChangedOutput = to;
  }

  public String getChangedOutput() {
    return myChangedOutput;
  }

}
