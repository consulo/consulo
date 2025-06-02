/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.execution.filters;

import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.InputFilter;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompositeInputFilter implements InputFilter {
  private static final Logger LOG = Logger.getInstance(CompositeInputFilter.class);

  private final List<Pair<InputFilter, Boolean /* is dumb aware */>> myFilters = new ArrayList<>();
  private final DumbService myDumbService;

  public CompositeInputFilter(@Nonnull Project project) {
    myDumbService = DumbService.getInstance(project);
  }

  @Override
  @Nullable
  public List<Pair<String, ConsoleViewContentType>> applyFilter(final String text, final ConsoleViewContentType contentType) {
    boolean dumb = myDumbService.isDumb();
    for (Pair<InputFilter, Boolean> pair : myFilters) {
      if (!dumb || Objects.equals(pair.second, Boolean.TRUE)) {
        long t0 = System.currentTimeMillis();
        InputFilter filter = pair.first;
        List<Pair<String, ConsoleViewContentType>> result = filter.applyFilter(text, contentType);
        t0 = System.currentTimeMillis() - t0;
        if (t0 > 100) {
          LOG.warn(filter.getClass().getSimpleName() + ".applyFilter() took " + t0 + " ms on '''" + text + "'''");
        }
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  public void addFilter(@Nonnull final InputFilter filter) {
    myFilters.add(Pair.create(filter, DumbService.isDumbAware(filter)));
  }
}
