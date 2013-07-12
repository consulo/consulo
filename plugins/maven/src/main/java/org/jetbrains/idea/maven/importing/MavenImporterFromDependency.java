/*
 * Copyright 2013 Consulo.org
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
package org.jetbrains.idea.maven.importing;

import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;

import java.util.List;

/**
 * @author VISTALL
 * @since 17:14/12.07.13
 */
public abstract class MavenImporterFromDependency extends MavenImporter {
  private final String myGroupId;
  private final String myArtifactId;

  public MavenImporterFromDependency(String groupId, String artifactId) {
    myGroupId = groupId;
    myArtifactId = artifactId;
  }

  @Override
  public boolean isApplicable(MavenProject mavenProject) {
    final List<MavenArtifact> dependencies = mavenProject.findDependencies(myGroupId, myArtifactId);
    return !dependencies.isEmpty();
  }
}
