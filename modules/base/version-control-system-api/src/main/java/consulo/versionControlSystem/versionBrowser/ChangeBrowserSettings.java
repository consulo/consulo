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
package consulo.versionControlSystem.versionBrowser;

import consulo.util.lang.Comparing;
import consulo.util.lang.SyncDateFormat;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ChangeBrowserSettings implements JDOMExternalizable {
  public static final String HEAD = "HEAD";

  public interface Filter {
    boolean accepts(CommittedChangeList change);
  }

  public boolean USE_DATE_BEFORE_FILTER = false;
  public boolean USE_DATE_AFTER_FILTER = false;
  public boolean USE_CHANGE_BEFORE_FILTER = false;
  public boolean USE_CHANGE_AFTER_FILTER = false;


  public String DATE_BEFORE = "";
  public String DATE_AFTER = "";

  @NonNls
  public String CHANGE_BEFORE = "";
  public String CHANGE_AFTER = "";

  public boolean USE_USER_FILTER = false;
  public String USER = "";
  public boolean STOP_ON_COPY = false;

  public static final SyncDateFormat DATE_FORMAT = new SyncDateFormat(SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG));

  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
  }

  private static Date parseDate(final String dateStr) {
    if (dateStr == null) return null;
    try {
      return DATE_FORMAT.parse(dateStr);
    }
    catch (Exception e) {
      return null;
    }
  }

  public void setDateBefore(final Date value) {
    if (value == null) {
      DATE_BEFORE = null;
    }
    else {
      DATE_BEFORE = DATE_FORMAT.format(value);
    }
  }

  public Date getDateBefore() {
    return parseDate(DATE_BEFORE);
  }

  public Date getDateAfter() {
    if (USE_DATE_AFTER_FILTER) {
      return parseDate(DATE_AFTER);
    }
    else {
      return null;
    }
  }

  public Long getChangeBeforeFilter() {
    if (USE_CHANGE_BEFORE_FILTER && CHANGE_BEFORE.length() > 0) {
      if (HEAD.equals(CHANGE_BEFORE)) return null;
      return Long.parseLong(CHANGE_BEFORE);
    }
    return null;
  }

  public Date getDateBeforeFilter() {
    if (USE_DATE_BEFORE_FILTER) {
      return parseDate(DATE_BEFORE);
    }
    else {
      return null;
    }
  }

  public Long getChangeAfterFilter() {
    if (USE_CHANGE_AFTER_FILTER && CHANGE_AFTER.length() > 0) {
      return Long.parseLong(CHANGE_AFTER);
    }
    return null;
  }

  public Date getDateAfterFilter() {
    return parseDate(DATE_AFTER);
  }

  public void setDateAfter(final Date value) {
    if (value == null) {
      DATE_AFTER = null;

    }
    else {
      DATE_AFTER = DATE_FORMAT.format(value);
    }
  }

  protected List<Filter> createFilters() {
    final ArrayList<Filter> result = new ArrayList<Filter>();
    addDateFilter(USE_DATE_BEFORE_FILTER, getDateBefore(), result, true);
    addDateFilter(USE_DATE_AFTER_FILTER, getDateAfter(), result, false);

    if (USE_CHANGE_BEFORE_FILTER) {
      try {
        final long numBefore = Long.parseLong(CHANGE_BEFORE);
        result.add(new Filter() {
          public boolean accepts(CommittedChangeList change) {
            return change.getNumber() <= numBefore;
          }
        });
      }
      catch (NumberFormatException e) {
        //ignore
      }
    }

    if (USE_CHANGE_AFTER_FILTER) {
      try {
        final long numAfter = Long.parseLong(CHANGE_AFTER);
        result.add(new Filter() {
          public boolean accepts(CommittedChangeList change) {
            return change.getNumber() >= numAfter;
          }
        });
      }
      catch (NumberFormatException e) {
        //ignore
      }
    }

    if (USE_USER_FILTER) {
      result.add(new Filter() {
        public boolean accepts(CommittedChangeList change) {
          return Comparing.equal(change.getCommitterName(), USER, false);
        }
      });
    }

    return result;
  }

  private static void addDateFilter(final boolean useFilter, final Date date, final ArrayList<Filter> result, final boolean before) {
    if (useFilter) {
      assert date != null;
      result.add(new Filter() {
        public boolean accepts(CommittedChangeList change) {
          final Date changeDate = change.getCommitDate();
          if (changeDate == null) return false;

          return before ? changeDate.before(date) : changeDate.after(date);
        }
      });
    }
  }

  public Filter createFilter() {
    final List<Filter> filters = createFilters();
    return new Filter() {
      public boolean accepts(CommittedChangeList change) {
        for (Filter filter : filters) {
          if (!filter.accepts(change)) return false;
        }
        return true;
      }
    };
  }

  public void filterChanges(final List<? extends CommittedChangeList> changeListInfos) {
    Filter filter = createFilter();
    for (Iterator<? extends CommittedChangeList> iterator = changeListInfos.iterator(); iterator.hasNext(); ) {
      CommittedChangeList changeListInfo = iterator.next();
      if (!filter.accepts(changeListInfo)) {
        iterator.remove();
      }
    }
  }

  public String getUserFilter() {
    if (USE_USER_FILTER) {
      return USER;
    }
    else {
      return null;
    }
  }

  public boolean isAnyFilterSpecified() {
    return USE_CHANGE_AFTER_FILTER || USE_CHANGE_BEFORE_FILTER || USE_DATE_AFTER_FILTER || USE_DATE_BEFORE_FILTER || isNonDateFilterSpecified();
  }

  public boolean isNonDateFilterSpecified() {
    return USE_USER_FILTER;
  }
}
