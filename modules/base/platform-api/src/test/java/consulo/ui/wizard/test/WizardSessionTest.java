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
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
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
  private static class StepStub<T> implements WizardStep<T> {
    private String myId;
    private boolean myVisible;

    private boolean myStepEnter, myStepLeave;

    private StepStub(String id, boolean visible) {
      myId = id;
      myVisible = visible;
    }

    @Override
    public void onStepEnter(@Nonnull T o) {
      myStepEnter = true;
    }

    @Override
    public void onStepLeave(@Nonnull T o) {
      myStepLeave = true;
    }

    @Override
    public boolean isVisible(@Nonnull T o) {
      return myVisible;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent(@Nonnull T context, @Nonnull Disposable uiDisposable) {
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
    steps.add(new StepStub<>("first", true));
    steps.add(new StepStub<>("second", true));
    steps.add(new StepStub<>("third", false));
    steps.add(new StepStub<>("fourth", true));

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
    StepStub<Object> first, second, third, fourth;

    List<WizardStep<Object>> steps = new ArrayList<>();
    steps.add(first = new StepStub<>("first", true));
    steps.add(second = new StepStub<>("second", true));
    steps.add(third = new StepStub<>("third", false));
    steps.add(fourth = new StepStub<>("fourth", true));

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

  @Test
  public void testPrevPrev() {
    List<WizardStep<Object>> steps = new ArrayList<>();
    steps.add(new StepStub<>("first", true));
    steps.add(new StepStub<>("second", true));
    steps.add(new StepStub<>("third", false));
    steps.add(new StepStub<>("fourth", true));

    WizardSession<Object> session = new WizardSession<>(ObjectUtil.NULL, steps);

    assertTrue(session.hasNext());

    session.next();
    session.next();

    WizardStep<Object> _3step = session.next();

    assertEquals(_3step.toString(), "fourth");

    WizardStep<Object> prev = session.prev();

    assertEquals(prev.toString(), "second");

    WizardStep<Object> prev2 = session.prev();

    assertEquals(prev2.toString(), "first");
  }

  private static class InvisibleContext {
    private boolean mySecondStepVisible = true;
  }
  
  private static class Step implements WizardStep<InvisibleContext> {
    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent(@Nonnull InvisibleContext context, @Nonnull Disposable uiDisposable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible(@Nonnull InvisibleContext invisibleContext) {
      return invisibleContext.mySecondStepVisible;
    }
  }

  @Test
  public void testVisibleAfterStepLeave() {
    List<WizardStep<InvisibleContext>> steps = new ArrayList<>();
    steps.add(new WizardStep<InvisibleContext>() {
      @RequiredUIAccess
      @Nonnull
      @Override
      public Component getComponent(@Nonnull InvisibleContext context, @Nonnull Disposable uiDisposable) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void onStepLeave(@Nonnull InvisibleContext invisibleContext) {
        invisibleContext.mySecondStepVisible = false;
      }
    });

    steps.add(new WizardStep<InvisibleContext>() {
      @RequiredUIAccess
      @Nonnull
      @Override
      public Component getComponent(@Nonnull InvisibleContext context, @Nonnull Disposable uiDisposable) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isVisible(@Nonnull InvisibleContext invisibleContext) {
        return invisibleContext.mySecondStepVisible;
      }
    });

    steps.add(new StepStub<>("end", true));

    WizardSession<InvisibleContext> session = new WizardSession<>(new InvisibleContext(), steps);

    assertTrue(session.hasNext());

    session.next();

    WizardStep<InvisibleContext> endStep = session.next();

    assertEquals(endStep.toString(), "end");
  }
}
