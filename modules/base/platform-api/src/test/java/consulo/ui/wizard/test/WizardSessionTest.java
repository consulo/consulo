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
package consulo.ui.wizard.test;

import com.intellij.util.ObjectUtil;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-09-05
 */
public class WizardSessionTest extends Assert {
  private static class StepStub implements WizardStep<Object> {
    private String myId;
    private boolean myVisible;

    private boolean myStepEnter, myStepLeave;

    private StepStub(String id, boolean visible) {
      myId = id;
      myVisible = visible;
    }

    @Override
    public void onStepEnter(@Nonnull Object o) {
      myStepEnter = true;
    }

    @Override
    public void onStepLeave(@Nonnull Object o) {
      myStepLeave = true;
    }

    @Override
    public boolean isVisible() {
      return myVisible;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return myId;
    }
  }

  @Test
  public void testVisibleStep() {
    List<WizardStep<Object>> steps = new ArrayList<>();
    steps.add(new StepStub("first", true));
    steps.add(new StepStub("second", true));
    steps.add(new StepStub("third", false));
    steps.add(new StepStub("fourth", true));

    WizardSession<Object> session = new WizardSession<>(ObjectUtil.NULL, steps);

    assertTrue(session.hasNext());

    assertEquals(session.next().toString(), "first");
    assertEquals(session.next().toString(), "second");
    assertEquals(session.next().toString(), "fourth");


    WizardStep<Object> prev = session.prev();

    assertEquals(prev.toString(), "second");
  }

  @Test
  public void testStepEnterAndLeave() {
    StepStub first, second, third, fourth;

    List<WizardStep<Object>> steps = new ArrayList<>();
    steps.add(first = new StepStub("first", true));
    steps.add(second = new StepStub("second", true));
    steps.add(third = new StepStub("third", false));
    steps.add(fourth = new StepStub("fourth", true));

    WizardSession<Object> session = new WizardSession<>(ObjectUtil.NULL, steps);

    assertTrue(session.hasNext());

    session.next();

    assertTrue(first.myStepEnter);

    session.next();

    assertTrue(first.myStepLeave);

    assertTrue(second.myStepEnter);

    session.next();

    assertTrue(second.myStepLeave);

    assertTrue(fourth.myStepEnter);

    assertFalse(third.myStepEnter);

    assertFalse(third.myStepLeave);
  }
}
