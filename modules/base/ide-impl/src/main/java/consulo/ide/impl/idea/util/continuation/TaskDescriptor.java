/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.continuation;

import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

public abstract class TaskDescriptor {
  // this also means that it would be called in case of chain cancel()
  private boolean myHaveMagicCure;
  private final String myName;
  @Nonnull
  private final Where myWhere;
  private final Map<Object, Object> mySurviveKit;

  public TaskDescriptor(String name, @Nonnull Where where) {
    myName = name;
    myWhere = where;
    mySurviveKit = new HashMap<Object, Object>();
  }

  public abstract void run(ContinuationContext context);

  public final void addCure(Object disaster, Object cure) {
    mySurviveKit.put(disaster, cure);
  }
  @jakarta.annotation.Nullable
  public final Object hasCure(Object disaster) {
    return mySurviveKit.get(disaster);
  }

  public String getName() {
    return myName;
  }

  @Nonnull
  public Where getWhere() {
    return myWhere;
  }

  public boolean isHaveMagicCure() {
    return myHaveMagicCure;
  }

  public void setHaveMagicCure(boolean haveMagicCure) {
    myHaveMagicCure = haveMagicCure;
  }

  public void canceled() {
  }

  public static TaskDescriptor createForBackgroundableTask(@Nonnull final Task.Backgroundable backgroundable) {
    return new TaskDescriptor(backgroundable.getTitle(), Where.POOLED) {
      @Override
      public void run(ContinuationContext context) {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        try {
          backgroundable.run(indicator);
        } catch (ProcessCanceledException e) {
          //
        }
        final boolean canceled = indicator.isCanceled();
        context.next(new TaskDescriptor("", Where.AWT) {
          @Override
          public void run(ContinuationContext context) {
            if (canceled) {
              backgroundable.onCancel();
            } else {
              backgroundable.onSuccess();
            }
          }
        });
      }
    };
  }
}
