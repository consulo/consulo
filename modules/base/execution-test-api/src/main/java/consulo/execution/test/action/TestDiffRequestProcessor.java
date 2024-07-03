// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.test.action;

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.ListSelection;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffPlaces;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.chain.SimpleDiffRequestChain;
import consulo.diff.content.DiffContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.stacktrace.DiffHyperlink;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.Objects;

public class TestDiffRequestProcessor {
  @Nonnull
  public static DiffRequestChain createRequestChain(@Nullable Project project, @Nonnull ListSelection<? extends DiffHyperlink> requests) {
    ListSelection<DiffRequestProducer> producers = requests.map(hyperlink -> new DiffHyperlinkRequestProducer(project, hyperlink));

    SimpleDiffRequestChain chain = SimpleDiffRequestChain.fromProducers(producers.getList(), producers.getSelectedIndex());
    chain.putUserData(DiffUserDataKeys.PLACE, DiffPlaces.TESTS_FAILED_ASSERTIONS);
    chain.putUserData(DiffUserDataKeys.DO_NOT_IGNORE_WHITESPACES, true);
    chain.putUserData(DiffUserDataKeys.DIALOG_GROUP_KEY, "#consulo.ide.impl.idea.execution.junit2.states.ComparisonFailureState$DiffDialog");  // NON-NLS
    return chain;
  }

  private static class DiffHyperlinkRequestProducer implements DiffRequestProducer {
    private final Project myProject;
    private final DiffHyperlink myHyperlink;

    private DiffHyperlinkRequestProducer(@Nullable Project project, @Nonnull DiffHyperlink hyperlink) {
      myProject = project;
      myHyperlink = hyperlink;
    }

    @Override
    public
    @Nls
    @Nonnull
    String getName() {
      String testName = myHyperlink.getTestName();
      if (testName != null) return testName;
      return myHyperlink.getDiffTitle();
    }

    @Override
    @Nonnull
    public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator) throws DiffRequestProducerException {
      String windowTitle = myHyperlink.getDiffTitle();

      String text1 = myHyperlink.getLeft();
      String text2 = myHyperlink.getRight();
      VirtualFile file1 = findFile(myHyperlink.getFilePath());
      VirtualFile file2 = findFile(myHyperlink.getActualFilePath());

      DiffContent content1 = createContentWithTitle(myProject, text1, file1, file2);
      DiffContent content2 = createContentWithTitle(myProject, text2, file2, file1);

      LocalizeValue title1 = file1 != null
        ? ExecutionLocalize.diffContentExpectedTitleWithFileUrl(file1.getPresentableUrl())
        : ExecutionLocalize.diffContentExpectedTitle();
      LocalizeValue title2 = file2 != null
        ? ExecutionLocalize.diffContentActualTitleWithFileUrl(file2.getPresentableUrl())
        : ExecutionLocalize.diffContentActualTitle();

      return new SimpleDiffRequest(windowTitle, content1, content2, title1.get(), title2.get());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DiffHyperlinkRequestProducer producer = (DiffHyperlinkRequestProducer)o;
      return Objects.equals(myHyperlink, producer.myHyperlink);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myHyperlink);
    }
  }

  @Nullable
  private static VirtualFile findFile(@Nullable String path) {
    if (path == null) return null;
    VirtualFileSystem fs = path.contains(URLUtil.JAR_SEPARATOR) ? StandardFileSystems.zip() : LocalFileSystem.getInstance();
    return fs.refreshAndFindFileByPath(path);
  }

  @Nonnull
  private static DiffContent createContentWithTitle(@Nullable Project project, @Nonnull String content, @Nullable VirtualFile contentFile, @Nullable VirtualFile highlightFile) {
    if (contentFile != null) {
      return DiffContentFactory.getInstance().create(project, contentFile);
    }
    else {
      return DiffContentFactory.getInstance().create(project, content, highlightFile);
    }
  }
}
