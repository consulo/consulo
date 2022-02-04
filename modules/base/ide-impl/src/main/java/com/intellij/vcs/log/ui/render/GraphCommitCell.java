package com.intellij.vcs.log.ui.render;

import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.graph.PrintElement;
import javax.annotation.Nonnull;

import java.util.Collection;

public class GraphCommitCell {

  @Nonnull
  private final String myText;
  @Nonnull
  private final Collection<VcsRef> myRefsToThisCommit;
  @Nonnull
  private final Collection<? extends PrintElement> myPrintElements;

  public GraphCommitCell(@Nonnull String text,
                         @Nonnull Collection<VcsRef> refsToThisCommit,
                         @Nonnull Collection<? extends PrintElement> printElements) {
    myText = text;
    myRefsToThisCommit = refsToThisCommit;
    myPrintElements = printElements;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  @Nonnull
  public Collection<VcsRef> getRefsToThisCommit() {
    return myRefsToThisCommit;
  }

  @Nonnull
  public Collection<? extends PrintElement> getPrintElements() {
    return myPrintElements;
  }

  @Override
  public String toString() {
    return myText;
  }
}
