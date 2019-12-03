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
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import consulo.packaging.artifacts.ArtifactPointerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class AbstractArtifactsBeforeRunTask<T extends AbstractArtifactsBeforeRunTask> extends BeforeRunTask<T> {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  @NonNls public static final String ARTIFACT_ELEMENT = "artifact";
  private List<ArtifactPointer> myArtifactPointers = new ArrayList<ArtifactPointer>();
  private final Project myProject;

  public AbstractArtifactsBeforeRunTask(Project project, Key<T> id) {
    super(id);
    myProject = project;
  }

  @Override
  public void readExternal(Element element) {
    super.readExternal(element);
    final List<Element> children = element.getChildren(ARTIFACT_ELEMENT);
    final ArtifactPointerManager pointerManager = ArtifactPointerUtil.getPointerManager(myProject);
    for (Element child : children) {
      myArtifactPointers.add(pointerManager.create(child.getAttributeValue(NAME_ATTRIBUTE)));
    }
  }

  @Override
  public void writeExternal(Element element) {
    super.writeExternal(element);
    for (ArtifactPointer pointer : myArtifactPointers) {
      element.addContent(new Element(ARTIFACT_ELEMENT).setAttribute(NAME_ATTRIBUTE, pointer.getName()));
    }
  }

  @Override
  public BeforeRunTask clone() {
    final AbstractArtifactsBeforeRunTask task = (AbstractArtifactsBeforeRunTask)super.clone();
    task.myArtifactPointers = new ArrayList<ArtifactPointer>(myArtifactPointers);
    return task;
  }

  @Override
  public int getItemsCount() {
    return myArtifactPointers.size();
  }

  public List<ArtifactPointer> getArtifactPointers() {
    return Collections.unmodifiableList(myArtifactPointers);
  }

  public void setArtifactPointers(List<ArtifactPointer> artifactPointers) {
    myArtifactPointers = new ArrayList<ArtifactPointer>(artifactPointers);
  }

  public void addArtifact(Artifact artifact) {
    final ArtifactPointer pointer = ArtifactPointerUtil.getPointerManager(myProject).create(artifact);
    if (!myArtifactPointers.contains(pointer)) {
      myArtifactPointers.add(pointer);
    }
  }

  public void removeArtifact(@Nonnull Artifact artifact) {
    removeArtifact(ArtifactPointerUtil.getPointerManager(myProject).create(artifact));
  }

  public void removeArtifact(final @Nonnull ArtifactPointer pointer) {
    myArtifactPointers.remove(pointer);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    AbstractArtifactsBeforeRunTask that = (AbstractArtifactsBeforeRunTask)o;

    if (!myArtifactPointers.equals(that.myArtifactPointers)) return false;
    if (!myProject.equals(that.myProject)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myArtifactPointers.hashCode();
    result = 31 * result + myProject.hashCode();
    return result;
  }
}
