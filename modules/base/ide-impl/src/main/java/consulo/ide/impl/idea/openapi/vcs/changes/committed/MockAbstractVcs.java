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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.configurable.Configurable;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.CommittedChangesProvider;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.ChangeProvider;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;

import jakarta.annotation.Nullable;

public class MockAbstractVcs extends AbstractVcs {
  private static final String NAME = "mock";
  private static final VcsKey ourKey = createKey(NAME);
  private boolean myMarkExternalChangesAsCurrent = false;
  private CheckinEnvironment myCheckinEnvironment;
  private CommittedChangesProvider myCommittedChangesProvider;
  private DiffProvider myDiffProvider;
  private ChangeProvider myChangeProvider;
  private boolean myAllowNestedRoots;

  public MockAbstractVcs(Project project){
    super(project, NAME);
    myAllowNestedRoots = false;
  }

  public MockAbstractVcs(final Project project, final String name) {
    super(project, name);
  }

  public CheckinEnvironment getCheckinEnvironment() {
    return myCheckinEnvironment;
  }

  public CommittedChangesProvider getCommittedChangesProvider() {
    return myCommittedChangesProvider;
  }

  public DiffProvider getDiffProvider() {
    return myDiffProvider;
  }

  public String getDisplayName() {
    return getName();
  }

  public Configurable getConfigurable() {
    return null;
  }

  public ChangeProvider getChangeProvider() {
    return myChangeProvider;
  }

  public boolean markExternalChangesAsUpToDate() {
    return myMarkExternalChangesAsCurrent ;
  }

  public void setMarkExternalChangesAsCurrent(boolean value){
    myMarkExternalChangesAsCurrent = value;
  }

  public void setCheckinEnvironment(CheckinEnvironment ce) {
    myCheckinEnvironment = ce;
  }

  public void setCommittedChangesProvider(final CommittedChangesProvider committedChangesProvider) {
    myCommittedChangesProvider = committedChangesProvider;
  }

  public void setDiffProvider(final DiffProvider diffProvider) {
    myDiffProvider = diffProvider;
  }

  @Override
  @Nullable
  public VcsRevisionNumber parseRevisionNumber(final String revisionNumberString) {
    return new VcsRevisionNumber.Int(Integer.parseInt(revisionNumberString));
  }

  @Override
  public String getRevisionPattern() {
    return ourIntegerPattern;
  }

  public void setChangeProvider(final ChangeProvider changeProvider) {
    myChangeProvider = changeProvider;
  }

  public static VcsKey getKey() {
    return ourKey;
  }

  @Override
  public boolean allowsNestedRoots() {
    return myAllowNestedRoots;
  }

  public void setAllowNestedRoots(boolean allowNestedRoots) {
    myAllowNestedRoots = allowNestedRoots;
  }
}
