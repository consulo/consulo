/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.mock;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class MockInspectionProfile extends InspectionProfileImpl {
  private InspectionToolWrapper[] myInspectionTools = new InspectionToolWrapper[0];
  private final Set<InspectionToolWrapper> myDisabledTools = new HashSet<InspectionToolWrapper>();

  public MockInspectionProfile() {
    super("a");
  }

  public void setEnabled(@Nonnull InspectionToolWrapper tool, boolean enabled) {
    if (enabled) {
      myDisabledTools.remove(tool);
    }
    else {
      myDisabledTools.add(tool);
    }
  }

  @Override
  public boolean isToolEnabled(final HighlightDisplayKey key, PsiElement element) {
    final InspectionToolWrapper entry = ContainerUtil.find(myInspectionTools, it -> key.equals(HighlightDisplayKey.find(it.getShortName())));
    assert entry != null;
    return !myDisabledTools.contains(entry);
  }

  public void setInspectionTools(final InspectionToolWrapper... entries) {
    myInspectionTools = entries;
  }

  @Override
  @Nonnull
  public InspectionToolWrapper[] getInspectionTools(PsiElement element) {
    return myInspectionTools;
  }
}
