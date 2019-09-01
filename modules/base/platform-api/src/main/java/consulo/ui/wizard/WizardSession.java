/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.wizard;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public final class WizardSession<CONTEXT> {
  private final List<WizardStep<CONTEXT>> mySteps;
  private final CONTEXT myContext;

  private int myCurrentStepIndex = -1;

  public WizardSession(@Nonnull CONTEXT context, @Nonnull List<WizardStep<CONTEXT>> steps) {
    myContext = context;
    mySteps = new ArrayList<>(steps);
  }

  public boolean hasNext() {
    int nextStep = myCurrentStepIndex + 1;

    for (int i = nextStep; i < mySteps.size(); i++) {
      WizardStep<CONTEXT> step = mySteps.get(i);

      if (step.isVisible()) {
        return true;
      }
    }

    return false;
  }

  @Nonnull
  public WizardStep<CONTEXT> next() {
    if (!hasNext()) {
      throw new IllegalArgumentException("There no visible next step");
    }

    int nextStepIndex = myCurrentStepIndex + 1;
    int prevStepIndex = myCurrentStepIndex - 1;

    if (prevStepIndex >= 0) {
      WizardStep<CONTEXT> prevStep = mySteps.get(prevStepIndex);

      prevStep.onStepLeave(myContext);
    }

    WizardStep<CONTEXT> step = mySteps.get(nextStepIndex);

    myCurrentStepIndex = nextStepIndex;
    step.onStepEnter(myContext);
    return step;
  }

  @Nonnull
  public WizardStep<CONTEXT> prev() {
    if (myCurrentStepIndex == 0) {
      throw new IllegalArgumentException("There no visible prev step");
    }

    int stepToMove = myCurrentStepIndex - 1;

    WizardStep<CONTEXT> currentStep = mySteps.get(myCurrentStepIndex);

    currentStep.onStepLeave(myContext);

    myCurrentStepIndex = stepToMove;

    WizardStep<CONTEXT> step = mySteps.get(myCurrentStepIndex);

    step.onStepEnter(myContext);

    return step;
  }

  @Nonnull
  public WizardStep<CONTEXT> current() {
    if (myCurrentStepIndex == -1) {
      throw new IllegalArgumentException();
    }
    return mySteps.get(myCurrentStepIndex);
  }

  public void dispose() {
    for (WizardStep<CONTEXT> step : mySteps) {
      step.disposeUIResources();
    }

    mySteps.clear();
  }

  public int getCurrentStepIndex() {
    return myCurrentStepIndex;
  }
}
