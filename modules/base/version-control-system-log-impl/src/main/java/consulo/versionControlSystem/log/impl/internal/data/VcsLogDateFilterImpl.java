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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.util.DateFormatUtil;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import consulo.versionControlSystem.log.VcsLogDateFilter;
import consulo.versionControlSystem.log.VcsLogDetailsFilter;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Date;

public class VcsLogDateFilterImpl implements VcsLogDateFilter, VcsLogDetailsFilter {

  private final @Nullable Date myAfter;
  private final @Nullable Date myBefore;

  public VcsLogDateFilterImpl(@Nullable Date after, @Nullable Date before) {
    myAfter = after;
    myBefore = before;
  }

  @Override
  public boolean matches(VcsCommitMetadata details) {
    Date date = new Date(details.getCommitTime());  // Git itself also filters by commit time, not author time
    boolean matches = true;
    if (myAfter != null) {
      matches &= date.after(myAfter);
    }
    if (myBefore != null) {
      matches &= date.before(myBefore);
    }
    return matches;
  }

  @Override
  public @Nullable Date getAfter() {
    return myAfter;
  }

  @Override
  public @Nullable Date getBefore() {
    return myBefore;
  }

  @Override
  public String getDisplayText() {
    if (myBefore != null && myAfter != null) {
      String after = DateFormatUtil.formatDate(myAfter);
      String before = DateFormatUtil.formatDate(myBefore);
      return VersionControlSystemLogLocalize.filterDateDisplayNameBetween(after, before).get();
    }
    else if (myAfter != null) {
      return VersionControlSystemLogLocalize.filterDateDisplayNameAfter(DateFormatUtil.formatDate(myAfter)).get();
    }
    else if (myBefore != null) {
      return VersionControlSystemLogLocalize.filterDateDisplayNameBefore(DateFormatUtil.formatDate(myBefore)).get();
    }
    return "";
  }

  public static String getDisplayTextWithPrefix(VcsLogDateFilter filter) {
    Date after = filter.getAfter();
    Date before = filter.getBefore();
    if (before != null && after != null) {
      return VersionControlSystemLogLocalize.filterPresentationDateWithPrefixMadeBetween(
        DateFormatUtil.formatDate(after),
        DateFormatUtil.formatDate(before)
      ).get();
    }
    else if (after != null) {
      return VersionControlSystemLogLocalize.filterPresentationDateWithPrefixMadeAfter(DateFormatUtil.formatDate(after)).get();
    }
    else if (before != null) {
      return VersionControlSystemLogLocalize.filterPresentationDateWithPrefixMadeBefore(DateFormatUtil.formatDate(before)).get();
    }
    return "";
  }
}
