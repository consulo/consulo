/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.index.io.internal;

import consulo.index.io.IndexId;
import consulo.util.lang.LoggerAssert;
import consulo.util.lang.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugAssertions {
  private static final Logger LOG = LoggerFactory.getLogger(DebugAssertions.class);

  public static final ThreadLocal<IndexId<?, ?>> DEBUG_INDEX_ID = new ThreadLocal<>();

  @SuppressWarnings("StaticNonFinalField")
  public static volatile boolean DEBUG = SystemProperties.getBooleanProperty("intellij.idea.indices.debug", false);

  public static final boolean EXTRA_SANITY_CHECKS = false;

  public static void assertTrue(boolean value) {
    if (!value) {
      LoggerAssert.assertTrue(LOG, false);
    }
  }

  public static void assertTrue(boolean value, String message, Object... args) {
    if (!value) {
      error(message, args);
    }
  }

  public static void error(String message, Object... args) {
    LOG.error(String.format(message, args));
  }
}
