package consulo.versionControlSystem.log.impl.internal.ui.render;

import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.graph.PrintElement;

import java.util.Collection;

public class GraphCommitCell {

  
  private final String myText;
  
  private final Collection<VcsRef> myRefsToThisCommit;
  
  private final Collection<? extends PrintElement> myPrintElements;

  public GraphCommitCell(String text,
                         Collection<VcsRef> refsToThisCommit,
                         Collection<? extends PrintElement> printElements) {
    myText = text;
    myRefsToThisCommit = refsToThisCommit;
    myPrintElements = printElements;
  }

  
  public String getText() {
    return myText;
  }

  
  public Collection<VcsRef> getRefsToThisCommit() {
    return myRefsToThisCommit;
  }

  
  public Collection<? extends PrintElement> getPrintElements() {
    return myPrintElements;
  }

  @Override
  public String toString() {
    return myText;
  }
}
