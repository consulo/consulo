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
package consulo.ide.impl.idea.util;

import consulo.component.ComponentManager;
import consulo.module.Module;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @author Sergey Evdokimov
 */
public class DisposeAwareRunnable extends WeakReference<Object> implements Runnable {

  protected final Runnable myDelegate;

  public static Runnable create(@Nonnull Runnable delegate, @Nullable PsiElement disposable) {
    return create(delegate, (Object)disposable);
  }

  public static Runnable create(@Nonnull Runnable delegate, @Nullable Project disposable) {
    return create(delegate, (Object)disposable);
  }

  public static Runnable create(@Nonnull Runnable delegate, @Nullable Module disposable) {
    return create(delegate, (Object)disposable);
  }

  private static Runnable create(@Nonnull Runnable delegate, @Nullable Object disposable) {
    if (disposable == null) {
      return delegate;
    }

    if (delegate instanceof DumbAware) {
      return new DumbAwareRunnable(delegate, disposable);
    }

    if (delegate instanceof PossiblyDumbAware) {
      return new PossiblyDumbAwareRunnable(delegate, disposable);
    }

    return new DisposeAwareRunnable(delegate, disposable);
  }

  private DisposeAwareRunnable(@Nonnull Runnable delegate, @Nonnull Object disposable) {
    super(disposable);
    myDelegate = delegate;
    assert disposable instanceof PsiElement || disposable instanceof ComponentManager : "Unknown type of "+disposable;
  }

  @Override
  public void run() {
    Object res = get();
    if (res == null) return;

    if (res instanceof PsiElement) {
      if (!((PsiElement)res).isValid()) return;
    }
    else if (res instanceof ComponentManager) {
      if (((ComponentManager)res).isDisposed()) return;
    }

    myDelegate.run();
  }

  private static class DumbAwareRunnable extends DisposeAwareRunnable implements DumbAware {
    DumbAwareRunnable(Runnable delegate, Object disposable) {
      super(delegate, disposable);
    }
  }

  private static class PossiblyDumbAwareRunnable extends DisposeAwareRunnable implements PossiblyDumbAware {
    PossiblyDumbAwareRunnable(Runnable delegate, Object disposable) {
      super(delegate, disposable);
    }

    @Override
    public boolean isDumbAware() {
      return ((PossiblyDumbAware)myDelegate).isDumbAware();
    }
  }
}
