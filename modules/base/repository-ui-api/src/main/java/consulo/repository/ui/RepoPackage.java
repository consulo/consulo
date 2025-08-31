// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.repository.ui;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

public class RepoPackage implements Comparable<RepoPackage> {
  private final String myName;
  @Nullable
  final String myRepoUrl;
  @Nullable
  final String myLatestVersion;
  private final Collection<String> myKeywords;

  public RepoPackage(String name, String repoUrl) {
    this(name, repoUrl, null);
  }

  public RepoPackage(String name, @Nullable String repoUrl, @Nullable String latestVersion) {
    this(name, repoUrl, latestVersion, Collections.emptyList());
  }

  public RepoPackage(String name, @Nullable String repoUrl, @Nullable String latestVersion, Collection<String> keywords) {
    myName = name;
    myRepoUrl = repoUrl;
    myLatestVersion = latestVersion;
    myKeywords = keywords;
  }

  public String getName() {
    return myName;
  }

  @Nullable
  public String getRepoUrl() {
    return myRepoUrl;
  }

  @Nullable
  public String getLatestVersion() {
    return myLatestVersion;
  }

  public Collection<String> getKeywords() {
    return myKeywords;
  }

  @Override
  public int compareTo(RepoPackage o) {
    return myName.compareTo(o.getName());
  }
}