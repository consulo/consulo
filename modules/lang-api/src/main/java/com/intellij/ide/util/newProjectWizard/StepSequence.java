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
 * User: anna
 * Date: 08-Jul-2007
 */
package com.intellij.ide.util.newProjectWizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StepSequence {
  private final List<ModuleWizardStep> myCommonSteps = new ArrayList<ModuleWizardStep>();
  private final List<Pair<ModuleWizardStep, Set<String>>> myCommonFinishingSteps = new ArrayList<Pair<ModuleWizardStep, Set<String>>>();
  private final MultiMap<String, ModuleWizardStep> mySpecificSteps = new MultiMap<String, ModuleWizardStep>();
  @NonNls private List<String> myTypes = new ArrayList<String>();
  private List<ModuleWizardStep> mySelectedSteps;

  public StepSequence(ModuleWizardStep... commonSteps) {
    myCommonSteps.addAll(Arrays.asList(commonSteps));
  }

  public void addCommonStep(@NotNull ModuleWizardStep step){
    myCommonSteps.add(step);
  }

  public void addCommonFinishingStep(@NotNull ModuleWizardStep step, @NotNull Set<String> suitableTypes) {
    myCommonFinishingSteps.add(Pair.create(step, suitableTypes));
  }

  public void addSpecificStep(String id, ModuleWizardStep step) {
    mySpecificSteps.putValue(id, step);
  }

  public List<ModuleWizardStep> getSelectedSteps() {
    if (mySelectedSteps == null) {
      mySelectedSteps = new ArrayList<ModuleWizardStep>();
      mySelectedSteps.addAll(myCommonSteps);
      for (String type : myTypes) {
        Collection<ModuleWizardStep> steps = mySpecificSteps.get(type);
        mySelectedSteps.addAll(steps);
      }
      for (Pair<ModuleWizardStep, Set<String>> pair : myCommonFinishingSteps) {
        if (ContainerUtil.intersects(myTypes, pair.getSecond())) {
          mySelectedSteps.add(pair.getFirst());
        }
      }
      ContainerUtil.removeDuplicates(mySelectedSteps);
    }

    return mySelectedSteps;
  }

  @Nullable
  public ModuleWizardStep getNextStep(ModuleWizardStep step) {
    final List<ModuleWizardStep> steps = getSelectedSteps();
    final int i = steps.indexOf(step);
    return i < steps.size() - 1 ? steps.get(i + 1) : null;
  }

  @Nullable
  public ModuleWizardStep getPreviousStep(ModuleWizardStep step) {
    final List<ModuleWizardStep> steps = getSelectedSteps();
    final int i = steps.indexOf(step);
    return i > 0 ? steps.get(i - 1) : null;
  }

  public void setTypes(Collection<String> types) {
    myTypes.clear();
    myTypes.addAll(types);
    mySelectedSteps = null;
  }

  public void setType(@Nullable @NonNls final String type) {
    if(type == null) {
      return;
    }
    setTypes(Collections.singletonList(type));
  }

  public String getSelectedType() {
    return ContainerUtil.getFirstItem(myTypes);
  }

  public List<ModuleWizardStep> getAllSteps() {
    final List<ModuleWizardStep> result = new ArrayList<ModuleWizardStep>();
    result.addAll(myCommonSteps);
    result.addAll(mySpecificSteps.values());
    for (Pair<ModuleWizardStep, Set<String>> pair : myCommonFinishingSteps) {
      result.add(pair.getFirst());
    }
    ContainerUtil.removeDuplicates(result);
    return result;
  }

  public ModuleWizardStep getFirstStep() {
    return myCommonSteps.get(0);
  }
}
