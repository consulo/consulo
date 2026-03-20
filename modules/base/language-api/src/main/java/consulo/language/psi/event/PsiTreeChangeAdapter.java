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
package consulo.language.psi.event;

/**
 * Default empty implementation of {@link PsiTreeChangeListener}.
 */
public abstract class PsiTreeChangeAdapter implements PsiTreeChangeListener {
  @Override
  public void beforeChildAddition(PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildRemoval(PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildReplacement(PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildMovement(PsiTreeChangeEvent event) {
  }

  @Override
  public void beforeChildrenChange(PsiTreeChangeEvent event) {
  }

  @Override
  public void beforePropertyChange(PsiTreeChangeEvent event) {
  }

  @Override
  public void childAdded(PsiTreeChangeEvent event) {
  }

  @Override
  public void childRemoved(PsiTreeChangeEvent event) {
  }

  @Override
  public void childReplaced(PsiTreeChangeEvent event) {
  }

  @Override
  public void childMoved(PsiTreeChangeEvent event) {
  }

  @Override
  public void childrenChanged(PsiTreeChangeEvent event) {
  }

  @Override
  public void propertyChanged(PsiTreeChangeEvent event) {
  }
}
