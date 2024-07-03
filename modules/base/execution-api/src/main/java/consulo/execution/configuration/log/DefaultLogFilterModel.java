/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.configuration.log;

import consulo.execution.localize.ExecutionLocalize;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class DefaultLogFilterModel extends LogFilterModel {
  private final Project myProject;
  private boolean myCheckStandartFilters = true;
  private String myPrevType = null;

  public DefaultLogFilterModel(Project project) {
    myProject = project;
  }

  protected LogConsolePreferences getPreferences() {
    return LogConsolePreferences.getInstance(myProject);
  }

  public boolean isCheckStandartFilters() {
    return myCheckStandartFilters;
  }

  public void setCheckStandartFilters(boolean checkStandartFilters) {
    myCheckStandartFilters = checkStandartFilters;
  }

  @Override
  public void updateCustomFilter(String filter) {
    super.updateCustomFilter(filter);
    getPreferences().updateCustomFilter(filter);
  }

  @Override
  public String getCustomFilter() {
    return getPreferences().CUSTOM_FILTER;
  }

  @Override
  public void addFilterListener(LogFilterListener listener) {
    getPreferences().addFilterListener(listener);
  }

  @Override
  public boolean isApplicable(String line) {
    if (!super.isApplicable(line)) return false;
    return getPreferences().isApplicable(line, myPrevType, myCheckStandartFilters);
  }

  @Override
  public void removeFilterListener(LogFilterListener listener) {
    getPreferences().removeFilterListener(listener);
  }

  @Override
  public List<LogFilter> getLogFilters() {
    LogConsolePreferences prefs = getPreferences();
    final ArrayList<LogFilter> filters = new ArrayList<LogFilter>();
    if (myCheckStandartFilters) {
      addStandartFilters(filters, prefs);
    }
    filters.addAll(prefs.getRegisteredLogFilters());
    return filters;
  }

  private void addStandartFilters(ArrayList<LogFilter> filters, final LogConsolePreferences prefs) {
    filters.add(new MyFilter(ExecutionLocalize.logConsoleFilterShowAll().get(), prefs) {
      @Override
      public void selectFilter() {
        prefs.FILTER_ERRORS = false;
        prefs.FILTER_INFO = false;
        prefs.FILTER_WARNINGS = false;
        prefs.FILTER_DEBUG = false;
      }

      @Override
      public boolean isSelected() {
        return !prefs.FILTER_ERRORS && !prefs.FILTER_INFO && !prefs.FILTER_WARNINGS && !prefs.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(ExecutionLocalize.logConsoleFilterShowErrorsWarningsAndInfos().get(), prefs) {
      @Override
      public void selectFilter() {
        prefs.FILTER_ERRORS = false;
        prefs.FILTER_INFO = false;
        prefs.FILTER_WARNINGS = false;
        prefs.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !prefs.FILTER_ERRORS && !prefs.FILTER_INFO && !prefs.FILTER_WARNINGS && prefs.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(ExecutionLocalize.logConsoleFilterShowErrorsAndWarnings().get(), prefs) {
      @Override
      public void selectFilter() {
        prefs.FILTER_ERRORS = false;
        prefs.FILTER_INFO = true;
        prefs.FILTER_WARNINGS = false;
        prefs.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !prefs.FILTER_ERRORS && prefs.FILTER_INFO && !prefs.FILTER_WARNINGS && prefs.FILTER_DEBUG;
      }
    });
    filters.add(new MyFilter(ExecutionLocalize.logConsoleFilterShowErrors().get(), prefs) {
      @Override
      public void selectFilter() {
        prefs.FILTER_ERRORS = false;
        prefs.FILTER_INFO = true;
        prefs.FILTER_WARNINGS = true;
        prefs.FILTER_DEBUG = true;
      }

      @Override
      public boolean isSelected() {
        return !prefs.FILTER_ERRORS && prefs.FILTER_INFO && prefs.FILTER_WARNINGS && prefs.FILTER_DEBUG;
      }
    });
  }

  @Override
  public boolean isFilterSelected(LogFilter filter) {
    return getPreferences().isFilterSelected(filter);
  }

  @Override
  public void selectFilter(LogFilter filter) {
    getPreferences().selectOnlyFilter(filter);
  }

  @Override
  @Nonnull
  public MyProcessingResult processLine(String line) {
    final String type = LogConsolePreferences.getType(line);
    Key contentType = type != null
                      ? LogConsolePreferences.getProcessOutputTypes(type)
                      : (LogConsolePreferences.ERROR.equals(myPrevType) ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT);
    if (type != null) {
      myPrevType = type;
    }
    final boolean applicable = isApplicable(line);
    return new MyProcessingResult(contentType, applicable, null);
  }

  private abstract class MyFilter extends IndependentLogFilter {
    private final LogConsolePreferences myPrefs;

    protected MyFilter(String name, LogConsolePreferences prefs) {
      super(name);
      myPrefs = prefs;
    }

    @Override
    public boolean isAcceptable(String line) {
      return myPrefs.isApplicable(line, myPrevType, myCheckStandartFilters);
    }
  }
}
