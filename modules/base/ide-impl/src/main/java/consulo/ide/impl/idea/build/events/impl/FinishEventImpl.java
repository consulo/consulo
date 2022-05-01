/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.build.events.impl;

import consulo.ide.impl.idea.build.events.BuildEventsNls;
import consulo.ide.impl.idea.build.events.EventResult;
import consulo.ide.impl.idea.build.events.FinishEvent;
import consulo.ide.impl.idea.build.events.SuccessResult;
import consulo.language.LangBundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class FinishEventImpl extends AbstractBuildEvent implements FinishEvent {

  private final EventResult myResult;

  public FinishEventImpl(@Nonnull Object eventId,
                         @Nullable Object parentId,
                         long eventTime,
                         @Nonnull @BuildEventsNls.Message String message,
                         @Nonnull EventResult result) {
    super(eventId, parentId, eventTime, message);
    myResult = result;
    if(myResult instanceof SuccessResult && ((SuccessResult)myResult).isUpToDate()) {
      setHint(LangBundle.message("build.event.message.up.to.date"));
    }
  }

  @Override
  public EventResult getResult() {
    return myResult;
  }
}
