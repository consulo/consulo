/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: anna
 * Date: 15-Feb-2006
 */
public class InspectionProfileWrapper {
  private static final Logger LOG = Logger.getInstance(InspectionProfileWrapper.class);

  /**
   * Key that is assumed to hold strategy that customizes {@link InspectionProfileWrapper} object to use.
   * <p/>
   * I.e. given strategy (if any) receives {@link InspectionProfileWrapper} object that is going to be used so far and returns
   * {@link InspectionProfileWrapper} object that should be used later.
   */
  public static final Key<Function<InspectionProfileWrapper, InspectionProfileWrapper>> CUSTOMIZATION_KEY
          = Key.create("Inspection Profile Wrapper Customization");
  protected final InspectionProfile myProfile;

  public InspectionProfileWrapper(@Nonnull InspectionProfile profile) {
    myProfile = profile;
  }

  @Nonnull
  public InspectionToolWrapper[] getInspectionTools(PsiElement element){
    return myProfile.getInspectionTools(element);
  }

  // check whether some inspection got registered twice by accident. 've bit once.
  private static boolean alreadyChecked;
  public static void checkInspectionsDuplicates(@Nonnull InspectionToolWrapper[] toolWrappers) {
    if (alreadyChecked) return;
    alreadyChecked = true;
    Set<InspectionProfileEntry> uniqTools = new HashSet<InspectionProfileEntry>(toolWrappers.length);
    for (InspectionToolWrapper toolWrapper : toolWrappers) {
      ProgressManager.checkCanceled();
      if (!uniqTools.add(toolWrapper.getTool())) {
        LOG.error("Inspection " + toolWrapper.getDisplayName() + " (" + toolWrapper.getTool().getClass() + ") already registered");
      }
    }
  }

  public String getName() {
    return myProfile.getName();
  }

  public boolean isToolEnabled(final HighlightDisplayKey key, PsiElement element) {
    return myProfile.isToolEnabled(key, element);
  }

  public InspectionToolWrapper getInspectionTool(final String shortName, PsiElement element) {
    return myProfile.getInspectionTool(shortName, element);
  }

  public void init(@Nonnull Project project) {
    final List<Tools> profileEntries = myProfile.getAllEnabledInspectionTools(project);
    for (Tools profileEntry : profileEntries) {
      for (ScopeToolState toolState : profileEntry.getTools()) {
        toolState.getTool().projectOpened(project);
      }
    }
  }

  public void cleanup(@Nonnull Project project){
    myProfile.cleanup(project);
  }

  @Nonnull
  public InspectionProfile getInspectionProfile() {
    return myProfile;
  }
}
