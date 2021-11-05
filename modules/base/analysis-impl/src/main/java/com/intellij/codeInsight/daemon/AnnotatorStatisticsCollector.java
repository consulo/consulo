// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

public class AnnotatorStatisticsCollector {
  // annotator-related internal statistics
  //private final Map<Annotator, DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics> myAnnotatorStats = new ConcurrentHashMap<>();
  // annotators which produced at least one Annotation during this session

  public void reportAnnotationProduced(@Nonnull Annotator annotator, @Nonnull Annotation annotation) {
    //DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics stat = myAnnotatorStats.computeIfAbsent(annotator, __ -> new DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics(annotator));
    //if (stat.firstAnnotation == null) {
    //  // ignore race condition - it's for statistics only
    //  stat.firstAnnotation = annotation;
    //  stat.firstAnnotationStamp = System.nanoTime();
    //}
    //stat.lastAnnotation = annotation;
    //stat.lastAnnotationStamp = System.nanoTime();
  }

  public void reportAnalysisFinished(@Nonnull Project project, @Nonnull AnnotationSession session, @Nonnull PsiFile file) {
    //for (DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics stat : myAnnotatorStats.values()) {
    //  stat.annotatorFinishStamp = System.nanoTime();
    //}
    //
    //project.getMessageBus().syncPublisher(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC).daemonAnnotatorStatisticsGenerated(session, myAnnotatorStats.values(), file);
    //myAnnotatorStats.clear();
  }

  public void reportNewAnnotatorCreated(@Nonnull Annotator annotator) {
    //DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics stat = myAnnotatorStats.computeIfAbsent(annotator, __ -> new DaemonCodeAnalyzer.DaemonListener.AnnotatorStatistics(annotator));
    //stat.annotatorStartStamp = System.nanoTime();
  }
}
