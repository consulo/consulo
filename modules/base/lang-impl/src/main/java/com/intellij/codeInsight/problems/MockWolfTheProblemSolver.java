/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInsight.problems;

import consulo.disposer.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author cdr
 */
public class MockWolfTheProblemSolver extends WolfTheProblemSolver {
  private WolfTheProblemSolver myDelegate;

  @Override
  public boolean isProblemFile(VirtualFile virtualFile) {
    return myDelegate != null && myDelegate.isProblemFile(virtualFile);
  }

  @Override
  public void weHaveGotProblems(@Nonnull final VirtualFile virtualFile, @Nonnull final List<Problem> problems) {
    if (myDelegate != null) myDelegate.weHaveGotProblems(virtualFile, problems);
  }

  @Override
  public void weHaveGotNonIgnorableProblems(@Nonnull VirtualFile virtualFile, @Nonnull List<Problem> problems) {
    if (myDelegate != null) myDelegate.weHaveGotNonIgnorableProblems(virtualFile, problems);
  }

  @Override
  public boolean hasProblemFilesBeneath(@Nonnull final Condition<VirtualFile> condition) {
    return false;
  }

  @Override
  public boolean hasSyntaxErrors(final VirtualFile file) {
    return myDelegate != null && myDelegate.hasSyntaxErrors(file);
  }

  @Override
  public boolean hasProblemFilesBeneath(@Nonnull Module scope) {
    return false;
  }

  @Override
  public void addProblemListener(@Nonnull ProblemListener listener, @Nonnull Disposable parentDisposable) {
    if (myDelegate != null) myDelegate.addProblemListener(listener, parentDisposable);
  }

  @Override
  public void queue(VirtualFile suspiciousFile) {
    if (myDelegate != null) myDelegate.queue(suspiciousFile);
  }

  @Override
  public void clearProblems(@Nonnull VirtualFile virtualFile) {
    if (myDelegate != null) myDelegate.clearProblems(virtualFile);
  }

  @Override
  public Problem convertToProblem(VirtualFile virtualFile, int line, int column, String[] message) {
    return myDelegate == null ? null : myDelegate.convertToProblem(virtualFile, line, column, message);
  }

  public void setDelegate(final WolfTheProblemSolver delegate) {
    myDelegate = delegate;
  }

  @Override
  public void reportProblems(final VirtualFile file, Collection<Problem> problems) {
    if (myDelegate != null) myDelegate.reportProblems(file,problems);
  }
}
