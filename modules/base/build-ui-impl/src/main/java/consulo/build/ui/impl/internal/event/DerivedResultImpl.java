// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.event.DerivedResult;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.FailureResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class DerivedResultImpl implements DerivedResult {

  
  private final Supplier<EventResult> myOnDefault;
  
  private final Supplier<FailureResult> myFail;

  public DerivedResultImpl() {
    this(null, null);
  }

  public DerivedResultImpl(@Nullable Supplier<EventResult> onDefault, @Nullable Supplier<FailureResult> onFail) {
    myOnDefault = onDefault != null ? onDefault : SuccessResultImpl::new;
    myFail = onFail != null ? onFail : FailureResultImpl::new;
  }

  
  @Override
  public FailureResult createFailureResult() {
    FailureResult result = myFail.get();
    if (result == null) {
      return new FailureResultImpl();
    }
    return result;
  }

  
  @Override
  public EventResult createDefaultResult() {
    EventResult result = myOnDefault.get();
    if (result == null) {
      return new SuccessResultImpl();
    }
    return result;
  }
}
