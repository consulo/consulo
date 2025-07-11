/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.concurrency;

import java.util.concurrent.ForkJoinPool;

/**
 * @author max
 */
public abstract class JobSchedulerImpl {
  /**
   * a number of CPU cores
   */
  public static int getCPUCoresCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  /**
   * A number of concurrent threads in the thread pool the JobLauncher uses to execute tasks.
   * By default it's CORES_COUNT - 1, but can be adjusted
   * via "java.util.concurrent.ForkJoinPool.common.parallelism property", e.g. "-Djava.util.concurrent.ForkJoinPool.common.parallelism=8"
   */
  public static int getJobPoolParallelism() {
    return ForkJoinPool.getCommonPoolParallelism();
  }
}
