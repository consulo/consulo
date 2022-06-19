// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.performance;

import consulo.annotation.component.Topic;
import consulo.application.util.concurrent.ThreadDump;
import consulo.component.messagebus.TopicImpl;
import javax.annotation.Nonnull;

import java.io.File;

@Topic
public interface IdePerformanceListener {
  /**
   * Invoked after thread state has been dumped to a file.
   */
  default void dumpedThreads(@Nonnull File toFile, @Nonnull ThreadDump dump) {
  }

  /**
   * Invoked when IDE has detected that the UI hasn't responded for some time (5 seconds by default)
   */
  default void uiFreezeStarted() {
  }

  /**
   * Invoked after the UI has become responsive again following a {@link #uiFreezeStarted()} event.
   *
   * @param lengthInSeconds approximate length in seconds of the interval that the IDE was unresponsive
   */
  default void uiFreezeFinished(int lengthInSeconds) {
  }
}
