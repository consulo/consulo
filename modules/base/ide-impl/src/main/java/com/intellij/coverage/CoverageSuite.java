package com.intellij.coverage;

import consulo.project.Project;
import com.intellij.util.xmlb.JDOMExternalizable;
import com.intellij.rt.coverage.data.ProjectData;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageSuite extends JDOMExternalizable {
  boolean isValid();

  @Nonnull
  String getCoverageDataFileName();

  String getPresentableName();

  long getLastCoverageTimeStamp();

  @Nonnull
  CoverageFileProvider getCoverageDataFileProvider();

  boolean isCoverageByTestApplicable();

  boolean isCoverageByTestEnabled();

  @Nullable
  ProjectData getCoverageData(CoverageDataManager coverageDataManager);

  void setCoverageData(final ProjectData projectData);

  void restoreCoverageData();

  boolean isTrackTestFolders();

  boolean isTracingEnabled();

  CoverageRunner getRunner();

  @Nonnull
  CoverageEngine getCoverageEngine();

  Project getProject();
}
