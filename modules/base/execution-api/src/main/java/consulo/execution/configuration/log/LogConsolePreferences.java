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

package consulo.execution.configuration.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.logging.Logger;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * @author anna
 * @since 2006-02-06
 */
@Singleton
@State(name = "LogFilters", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class LogConsolePreferences extends LogFilterRegistrar {
  private final SortedMap<LogFilter, Boolean> myRegisteredLogFilters = new TreeMap<LogFilter, Boolean>((o1, o2) -> -1);
  @NonNls
  private static final String FILTER = "filter";
  @NonNls
  private static final String IS_ACTIVE = "is_active";

  public boolean FILTER_ERRORS = false;
  public boolean FILTER_WARNINGS = false;
  public boolean FILTER_INFO = true;
  public boolean FILTER_DEBUG = true;

  public String CUSTOM_FILTER = null;
  public static final String ERROR = "ERROR";
  public static final String WARNING = "WARNING";
  private static final String WARN = "WARN";
  public static final String INFO = "INFO";
  public static final String DEBUG = "DEBUG";
  public static final String CUSTOM = "CUSTOM";

  public final static Pattern ERROR_PATTERN = Pattern.compile(".*(" + ERROR + "|FATAL).*");
  public final static Pattern WARNING_PATTERN = Pattern.compile(".*" + WARNING + ".*");
  public final static Pattern WARN_PATTERN = Pattern.compile(".*" + WARN + ".*");
  public final static Pattern INFO_PATTERN = Pattern.compile(".*" + INFO + ".*");
  public static final Pattern DEBUG_PATTERN = Pattern.compile(".*" + DEBUG + ".*");

  public final static Pattern EXCEPTION_PATTERN = Pattern.compile(".*at .*");

  private final List<LogFilterListener> myListeners = Lists.newLockFreeCopyOnWriteList();
  private static final Logger LOG = Logger.getInstance(LogConsolePreferences.class);

  public static LogConsolePreferences getInstance(Project project) {
    return project.getInstance(LogConsolePreferences.class);
  }

  public void updateCustomFilter(String customFilter) {
    CUSTOM_FILTER = customFilter;
    fireStateChanged();
  }


  public boolean isApplicable(@Nonnull String text, String prevType, boolean checkStandartFilters) {
    for (LogFilter filter : myRegisteredLogFilters.keySet()) {
      if (myRegisteredLogFilters.get(filter).booleanValue() && !filter.isAcceptable(text)) return false;
    }
    if (checkStandartFilters) {
      final String type = getType(text);
      boolean selfTyped = false;
      if (type != null) {
        if (!isApplicable(type)) return false;
        selfTyped = true;
      }
      return selfTyped || prevType == null || isApplicable(prevType);
    }
    return true;
  }

  private boolean isApplicable(final String type) {
    if (type.equals(ERROR)) {
      return !FILTER_ERRORS;
    }
    if (type.equals(WARNING)) {
      return !FILTER_WARNINGS;
    }
    if (type.equals(INFO)) {
      return !FILTER_INFO;
    }
    if (type.equals(DEBUG)) {
      return !FILTER_DEBUG;
    }
    return true;
  }

  public static ConsoleViewContentType getContentType(String type) {
    if (type.equals(ERROR)) return ConsoleViewContentType.ERROR_OUTPUT;
    return ConsoleViewContentType.NORMAL_OUTPUT;
  }

  @Nullable
  public static String getType(@Nonnull String text) {
    String upcased = StringUtil.toUpperCase(text);
    if (ERROR_PATTERN.matcher(upcased).matches()) return ERROR;
    if (WARNING_PATTERN.matcher(upcased).matches() || WARN_PATTERN.matcher(upcased).matches()) return WARNING;
    if (INFO_PATTERN.matcher(upcased).matches()) return INFO;
    if (DEBUG_PATTERN.matcher(upcased).matches()) return DEBUG;
    return null;
  }

  public static Key getProcessOutputTypes(String type) {
    if (type.equals(ERROR)) return ProcessOutputTypes.STDERR;
    if (type.equals(WARNING) || type.equals(INFO) || type.equals(DEBUG)) return ProcessOutputTypes.STDOUT;
    return null;
  }

  @Override
  public Element getState() {
    @NonNls Element element = new Element("LogFilters");
    try {
      for (LogFilter filter : myRegisteredLogFilters.keySet()) {
        Element filterElement = new Element(FILTER);
        filterElement.setAttribute(IS_ACTIVE, myRegisteredLogFilters.get(filter).toString());
        filter.writeExternal(filterElement);
        element.addContent(filterElement);
      }
      DefaultJDOMExternalizer.writeExternal(this, element);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return element;
  }

  @Override
  public void loadState(final Element object) {
    try {
      final List children = object.getChildren(FILTER);
      for (Object child : children) {
        Element filterElement = (Element)child;
        final LogFilter filter = new LogFilter();
        filter.readExternal(filterElement);
        setFilterSelected(filter, Boolean.parseBoolean(filterElement.getAttributeValue(IS_ACTIVE)));
      }
      DefaultJDOMExternalizer.readExternal(this, object);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  @Override
  public void registerFilter(LogFilter filter) {
    myRegisteredLogFilters.put(filter, Boolean.FALSE);
  }

  @Override
  public List<LogFilter> getRegisteredLogFilters() {
    return new ArrayList<LogFilter>(myRegisteredLogFilters.keySet());
  }

  @Override
  public boolean isFilterSelected(LogFilter filter) {
    final Boolean isSelected = myRegisteredLogFilters.get(filter);
    if (isSelected != null) {
      return isSelected.booleanValue();
    }
    if (filter instanceof IndependentLogFilter) {
      return ((IndependentLogFilter)filter).isSelected();
    }
    return false;
  }

  @Override
  public void setFilterSelected(LogFilter filter, boolean state) {
    if (filter instanceof IndependentLogFilter) {
      ((IndependentLogFilter)filter).selectFilter();
    }
    else if (myRegisteredLogFilters.containsKey(filter)) {
      myRegisteredLogFilters.put(filter, state);
    }
    fireStateChanged(filter);
  }

  public void selectOnlyFilter(LogFilter filter) {
    for (LogFilter logFilter : myRegisteredLogFilters.keySet()) {
      myRegisteredLogFilters.put(logFilter, false);
    }
    if (filter != null) {
      setFilterSelected(filter, true);
    }
  }

  private void fireStateChanged(final LogFilter filter) {
    for (LogFilterListener listener : myListeners) {
      listener.onFilterStateChange(filter);
    }
  }

  private void fireStateChanged() {
    for (LogFilterListener listener : myListeners) {
      listener.onTextFilterChange();
    }
  }

  public void addFilterListener(LogFilterListener l) {
    myListeners.add(l);
  }

  public void removeFilterListener(LogFilterListener l) {
    myListeners.remove(l);
  }

}
