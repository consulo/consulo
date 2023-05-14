// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.util.query;

import consulo.application.util.query.QueryExecutor;
import consulo.project.Project;

import jakarta.annotation.Nullable;

/**
 * An interface to be (optionally, but preferably) implemented by {@link QueryExecutor} parameters to provide additional data.
 *
 * @author peter
 */
public interface QueryParameters {
  @Nullable
  default Project getProject() {
    return null;
  }

  /**
   * @return whether this query still makes sense (e.g. PSI inside is still valid)
   */
  default boolean isQueryValid() {
    return true;
  }
}
