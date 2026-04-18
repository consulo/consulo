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

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;

import org.jspecify.annotations.Nullable;
import java.util.EventObject;

/**
 * Provides information about a change in the PSI tree of a project.
 *
 * @see PsiTreeChangeListener
 */
public abstract class PsiTreeChangeEvent extends EventObject {
  public static final String PROP_FILE_NAME = "fileName";
  public static final String PROP_DIRECTORY_NAME = "directoryName";
  public static final String PROP_WRITABLE = "writable";

  public static final String PROP_ROOTS = "roots";

  public static final String PROP_FILE_TYPES = "propFileTypes";
  public static final String PROP_UNLOADED_PSI = "propUnloadedPsi";

  protected @Nullable PsiElement myParent = null;
  protected @Nullable PsiElement myOldParent = null;
  protected @Nullable PsiElement myNewParent = null;
  protected @Nullable PsiElement myChild = null;
  protected @Nullable PsiElement myOldChild = null;
  protected @Nullable PsiElement myNewChild = null;

  protected @Nullable PsiFile myFile = null;
  protected int myOffset;
  protected int myOldLength;

  protected @Nullable PsiElement myElement = null;
  protected @Nullable String myPropertyName = null;
  protected @Nullable Object myOldValue = null;
  protected @Nullable Object myNewValue = null;

  protected PsiTreeChangeEvent(PsiManager manager) {
    super(manager);
  }

  public @Nullable PsiElement getParent() {
    return myParent;
  }

  public @Nullable PsiElement getOldParent() {
    return myOldParent;
  }

  public @Nullable PsiElement getNewParent() {
    return myNewParent;
  }

  public @Nullable PsiElement getChild() {
    return myChild;
  }

  public @Nullable PsiElement getOldChild() {
    return myOldChild;
  }

  public @Nullable PsiElement getNewChild() {
    return myNewChild;
  }

  public @Nullable PsiElement getElement() {
    return myElement;
  }

  public @Nullable String getPropertyName() {
    return myPropertyName;
  }

  public @Nullable Object getOldValue() {
    return myOldValue;
  }

  public @Nullable Object getNewValue() {
    return myNewValue;
  }

  public @Nullable PsiFile getFile() {
    return myFile;
  }
}

