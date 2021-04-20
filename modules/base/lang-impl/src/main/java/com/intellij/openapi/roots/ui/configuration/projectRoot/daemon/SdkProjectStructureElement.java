package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class SdkProjectStructureElement extends ProjectStructureElement {
  private final Sdk mySdk;

  public SdkProjectStructureElement(Sdk sdk) {
    mySdk = sdk;
  }

  public Sdk getSdk() {
    return mySdk;
  }

  @Nullable
  @Override
  public String getDescription() {
    return mySdk.getVersionString();
  }

  @Override
  public void check(Project project, ProjectStructureProblemsHolder problemsHolder) {
  }

  @Override
  public List<ProjectStructureElementUsage> getUsagesInElement() {
    return Collections.emptyList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SdkProjectStructureElement)) return false;
    return mySdk.equals(((SdkProjectStructureElement)o).mySdk);

  }

  @Override
  public int hashCode() {
    return mySdk.hashCode();
  }

  @Override
  public String getPresentableName() {
    return "SDK '" + mySdk.getName() + "'";
  }

  @Override
  public String getTypeName() {
    return "SDK";
  }

  @Override
  public String getId() {
    return "sdk:" + mySdk.getName();
  }
}
