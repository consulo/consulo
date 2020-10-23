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
package consulo.application;

import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-04
 */
public final class CallChain {
  public static final class Link<OldValue, NewValue> {
    private CallChain myCallChain;

    private Function<OldValue, AsyncResult<NewValue>> myTask;

    private Link(CallChain callChain, Function<OldValue, AsyncResult<NewValue>> task) {
      myCallChain = callChain;
      myTask = task;

      myCallChain.myLinks.add(this);
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkUI(@RequiredUIAccess @Nonnull Runnable runnable) {
      return linkUI(newValue -> {
        runnable.run();
        return null;
      });
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkUI(@RequiredUIAccess @Nonnull Supplier<SubNewValue> function) {
      return linkUI(newValue -> function.get());
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkUI(@RequiredUIAccess @Nonnull Function<NewValue, SubNewValue> function) {
      if (myCallChain.myLocked) {
        throw new IllegalArgumentException("started already");
      }

      if (myCallChain.myUIAccess == null) {
        throw new IllegalArgumentException("ui access is not set");
      }

      return linkAsync(oldValue -> myCallChain.myUIAccess.give(() -> function.apply(oldValue)));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> link(@Nonnull Function<NewValue, SubNewValue> function) {
      return linkAsync(oldValue -> AsyncResult.resolved(function.apply(oldValue)));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkAsync(@Nonnull Supplier<AsyncResult<SubNewValue>> function) {
      return linkAsync(newValue -> function.get());
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkAsync(@Nonnull Function<NewValue, AsyncResult<SubNewValue>> function) {
      if (myCallChain.myLocked) {
        throw new IllegalArgumentException("started already");
      }

      return new Link<>(myCallChain, function);
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkWrite(@RequiredWriteAction @Nonnull Runnable function) {
      return linkAsync(oldValue -> AccessRule.writeAsync(() -> {
        function.run();
        return null;
      }));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkWrite(@Nonnull Supplier<SubNewValue> function) {
      return linkAsync(oldValue -> AccessRule.writeAsync(function::get));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkWrite(@Nonnull Function<NewValue, SubNewValue> function) {
      return linkAsync(oldValue -> AccessRule.writeAsync(() -> function.apply(oldValue)));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkRead(@RequiredWriteAction @Nonnull Runnable function) {
      return linkAsync(oldValue -> AccessRule.readAsync(() -> {
        function.run();
        return null;
      }));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkRead(@Nonnull Supplier<SubNewValue> function) {
      return linkAsync(oldValue -> AccessRule.readAsync(function::get));
    }

    @Nonnull
    public <SubNewValue> Link<NewValue, SubNewValue> linkRead(@Nonnull Function<NewValue, SubNewValue> function) {
      return linkAsync(oldValue -> AccessRule.readAsync(() -> function.apply(oldValue)));
    }

    public void toss() {
      toss(newValue -> {
      });
    }

    public void toss(@Nonnull Consumer<NewValue> consumer) {
      myCallChain.start(consumer);
    }

    @Nonnull
    public AsyncResult<NewValue> tossAsync() {
      AsyncResult<NewValue> result = AsyncResult.undefined();
      toss(result::setDone);
      return result;
    }
  }

  @Nonnull
  public static Link<Void, Void> first() {
    return first(null);
  }

  @Nonnull
  public static Link<Void, Void> first(@Nullable UIAccess uiAccess) {
    CallChain callChain = new CallChain(uiAccess);
    return new Link<>(callChain, (f) -> AsyncResult.resolved());
  }

  private final UIAccess myUIAccess;
  private final List<Link> myLinks = new ArrayList<>();
  private boolean myLocked;

  private CallChain(@Nullable UIAccess access) {
    myUIAccess = access;
  }

  private <Value> void start(@Nonnull Consumer<Value> consumer) {
    if (myLinks.isEmpty()) {
      throw new IllegalArgumentException("empty chain");
    }
    if (myLocked) {
      throw new IllegalArgumentException("started already");
    }

    myLocked = true;

    run(0, null, consumer);
  }

  @SuppressWarnings("unchecked")
  private <Value> void run(int index, Object prevValue, Consumer<Value> result) {
    if (index == myLinks.size()) {
      result.accept((Value)prevValue);
      myLinks.clear();
      return;
    }

    Link<Object, Object> link = myLinks.get(index);

    link.myTask.apply(prevValue).doWhenDone(o -> run(index + 1, o, result));
  }
}