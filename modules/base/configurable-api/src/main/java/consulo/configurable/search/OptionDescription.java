// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.configurable.search;

import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class OptionDescription implements Comparable<OptionDescription> {
  private final @Nullable String myOption;
  private final @Nullable String myHit;
  private final @Nullable String myPath;
  private final @Nullable String myConfigurableId;
  private final @Nullable String myGroupName;

  public OptionDescription(@Nullable String hit) {
    this(null, hit, null);
  }

  public OptionDescription(@Nullable String option, @Nullable String hit, @Nullable String path) {
    this(option, null, hit, path);
  }

  public OptionDescription(@Nullable String option, @Nullable String configurableId, @Nullable String hit, @Nullable String path) {
    this(option, configurableId, hit, path, null);
  }

  public OptionDescription(@Nullable String option,
                           @Nullable String configurableId,
                           @Nullable String hit,
                           @Nullable String path,
                           @Nullable String groupName) {
    myOption = option;
    myHit = hit;
    myPath = path;
    myConfigurableId = configurableId;
    myGroupName = groupName;
  }

  public @Nullable String getOption() {
    return myOption;
  }

  public @Nullable String getHit() {
    return myHit;
  }

  public @Nullable String getPath() {
    return myPath;
  }

  public @Nullable String getConfigurableId() {
    return myConfigurableId;
  }

  public @Nullable String getGroupName() {
    return myGroupName;
  }

  public @Nullable String getValue() {
    return null;
  }

  public boolean hasExternalEditor() {
    return false;
  }

  public void invokeInternalEditor() {
  }

  public @Nullable String toString() {
    return myHit;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OptionDescription that = (OptionDescription)o;

    if (!Objects.equals(myConfigurableId, that.myConfigurableId)) return false;
    if (!Objects.equals(myHit, that.myHit)) return false;
    if (!Objects.equals(myOption, that.myOption)) return false;
    if (!Objects.equals(myPath, that.myPath)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myOption != null ? myOption.hashCode() : 0);
    result = 31 * result + (myHit != null ? myHit.hashCode() : 0);
    result = 31 * result + (myPath != null ? myPath.hashCode() : 0);
    result = 31 * result + (myConfigurableId != null ? myConfigurableId.hashCode() : 0);
    return result;
  }

  @Override
  public int compareTo(OptionDescription o) {
    if (Comparing.strEqual(myHit, o.getHit())) {
      String option = o.getOption();
      return myOption != null && option != null ? myOption.compareTo(option) : 0;
    }
    if (myHit != null && o.getHit() != null) {
      return myHit.compareTo(o.getHit());
    }
    return 0;
  }
}
