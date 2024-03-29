// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.application.AllIcons;
import consulo.build.ui.BuildBundle;
import consulo.build.ui.ExecutionNode;
import consulo.build.ui.event.*;
import consulo.application.util.NullableLazyValue;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.AnimatedIcon;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class ExecutionNodeImpl extends ExecutionNode<ExecutionNodeImpl> {
  private static final Image NODE_ICON_OK = AllIcons.RunConfigurations.TestPassed;
  private static final Image NODE_ICON_ERROR = AllIcons.RunConfigurations.TestError;
  private static final Image NODE_ICON_WARNING = AllIcons.General.Warning;
  private static final Image NODE_ICON_INFO = AllIcons.General.Information;
  private static final Image NODE_ICON_SKIPPED = AllIcons.RunConfigurations.TestIgnored;
  private static final Image NODE_ICON_STATISTICS = Image.empty(16);
  private static final Image NODE_ICON_SIMPLE = Image.empty(16);
  private static final Image NODE_ICON_DEFAULT = Image.empty(16);
  private static final Image NODE_ICON_RUNNING = new AnimatedIcon.Default();

  private final List<ExecutionNodeImpl> myChildrenList = new ArrayList<>(); // Accessed from the async model thread only.
  private List<ExecutionNodeImpl> myVisibleChildrenList = null;  // Accessed from the async model thread only.
  private final AtomicInteger myErrors = new AtomicInteger();
  private final AtomicInteger myWarnings = new AtomicInteger();
  private final AtomicInteger myInfos = new AtomicInteger();
  private final ExecutionNodeImpl myParentNode;
  private volatile long startTime;
  private volatile long endTime;
  @Nullable
  private @BuildEventsNls.Title String myTitle;
  @Nullable
  private @BuildEventsNls.Hint String myHint;
  @Nullable
  private volatile EventResult myResult;
  private final boolean myAutoExpandNode;
  private final Supplier<Boolean> myIsCorrectThread;
  @Nullable
  private volatile Navigatable myNavigatable;
  @Nullable
  private volatile NullableLazyValue<Image> myPreferredIconValue;
  private Predicate<? super ExecutionNodeImpl> myFilter;
  private boolean myAlwaysLeaf;
  private boolean myAlwaysVisible;

  public ExecutionNodeImpl(Project aProject, ExecutionNodeImpl parentNode, boolean isAutoExpandNode, @Nonnull Supplier<Boolean> isCorrectThread) {
    super(parentNode);
    myName = "";
    myParentNode = parentNode;
    myAutoExpandNode = isAutoExpandNode;
    myIsCorrectThread = isCorrectThread;
  }

  private boolean nodeIsVisible(ExecutionNodeImpl node) {
    return node.myAlwaysVisible || myFilter == null || myFilter.test(node);
  }

  @Override
  protected void update(@Nonnull PresentationData presentation) {
    assert myIsCorrectThread.get();
    setIcon(getCurrentIcon());
    presentation.setPresentableText(myName);
    presentation.setIcon(getIcon());
    if (StringUtil.isNotEmpty(myTitle)) {
      presentation.addText(myTitle + ": ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    String hint = getCurrentHint();
    boolean isNotEmptyName = StringUtil.isNotEmpty(myName);
    if (isNotEmptyName && myTitle != null || hint != null) {
      presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    if (StringUtil.isNotEmpty(hint)) {
      if (isNotEmptyName) {
        hint = " " + hint;
      }
      presentation.addText(hint, SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }

  //@ApiStatus.Internal
  void applyFrom(@Nonnull BuildEventPresentationData buildEventPresentationData) {
    myAlwaysVisible = true;
    setIconProvider(() -> buildEventPresentationData.getNodeIcon());
  }

  @Override
  public String getName() {
    return myName;
  }

  public void setName(String name) {
    assert myIsCorrectThread.get();
    myName = name;
  }

  @Nullable
  public String getTitle() {
    assert myIsCorrectThread.get();
    return myTitle;
  }

  public void setTitle(@BuildEventsNls.Title @Nullable String title) {
    assert myIsCorrectThread.get();
    myTitle = title;
  }

  public void setHint(@BuildEventsNls.Hint @Nullable String hint) {
    assert myIsCorrectThread.get();
    myHint = hint;
  }

  public void add(@Nonnull ExecutionNodeImpl node) {
    assert myIsCorrectThread.get();
    myChildrenList.add(node);
    node.setFilter(myFilter);
    if (myVisibleChildrenList != null) {
      if (nodeIsVisible(node)) {
        myVisibleChildrenList.add(node);
      }
    }
  }

  void removeChildren() {
    assert myIsCorrectThread.get();
    myChildrenList.clear();
    if (myVisibleChildrenList != null) {
      myVisibleChildrenList.clear();
    }
    myErrors.set(0);
    myWarnings.set(0);
    myInfos.set(0);
    myResult = null;
  }

  // Note: invoked from the EDT.
  @Nullable
  public
  @Nls
  String getDuration() {
    if (startTime == endTime) return null;
    if (isRunning()) {
      long duration = startTime == 0 ? 0 : System.currentTimeMillis() - startTime;
      if (duration > 1000) {
        duration -= duration % 1000;
      }
      return StringUtil.formatDurationApproximate(duration);
    }
    else {
      return isSkipped(myResult) ? null : StringUtil.formatDuration(endTime - startTime);
    }
  }

  public long getStartTime() {
    assert myIsCorrectThread.get();
    return startTime;
  }

  public void setStartTime(long startTime) {
    assert myIsCorrectThread.get();
    this.startTime = startTime;
  }

  public long getEndTime() {
    assert myIsCorrectThread.get();
    return endTime;
  }

  public ExecutionNodeImpl setEndTime(long endTime) {
    assert myIsCorrectThread.get();
    this.endTime = endTime;
    return reapplyParentFilterIfRequired(null);
  }

  private ExecutionNodeImpl reapplyParentFilterIfRequired(@Nullable ExecutionNodeImpl result) {
    assert myIsCorrectThread.get();
    if (myParentNode != null) {
      List<ExecutionNodeImpl> parentVisibleChildrenList = myParentNode.myVisibleChildrenList;
      if (parentVisibleChildrenList != null) {
        Predicate<? super ExecutionNodeImpl> filter = myParentNode.myFilter;
        if (myAlwaysVisible || filter != null) {
          boolean wasPresent = parentVisibleChildrenList.contains(this);
          boolean shouldBePresent = myAlwaysVisible || filter.test(this);
          if (shouldBePresent != wasPresent) {
            if (shouldBePresent) {
              myParentNode.maybeReapplyFilter();
            }
            else {
              parentVisibleChildrenList.remove(this);
            }
            result = myParentNode;
          }
        }
      }
      return myParentNode.reapplyParentFilterIfRequired(result);
    }
    return result;
  }

  @Nonnull
  public List<ExecutionNodeImpl> getChildList() {
    assert myIsCorrectThread.get();
    List<ExecutionNodeImpl> visibleList = myVisibleChildrenList;
    return Objects.requireNonNullElse(visibleList, myChildrenList);
  }

  @Nullable
  public ExecutionNodeImpl getParent() {
    return myParentNode;
  }

  @Override
  public ExecutionNodeImpl getElement() {
    return this;
  }

  public Predicate<? super ExecutionNodeImpl> getFilter() {
    assert myIsCorrectThread.get();
    return myFilter;
  }

  public void setFilter(@Nullable Predicate<? super ExecutionNodeImpl> filter) {
    assert myIsCorrectThread.get();
    myFilter = filter;
    for (ExecutionNodeImpl node : myChildrenList) {
      node.setFilter(myFilter);
    }
    if (filter == null) {
      myVisibleChildrenList = null;
    }
    else {
      if (myVisibleChildrenList == null) {
        myVisibleChildrenList = Collections.synchronizedList(new ArrayList<>());
      }
      maybeReapplyFilter();
    }
  }

  private void maybeReapplyFilter() {
    assert myIsCorrectThread.get();
    if (myVisibleChildrenList != null) {
      myVisibleChildrenList.clear();
      myChildrenList.stream().filter(it -> nodeIsVisible(it)).forEachOrdered(myVisibleChildrenList::add);
    }
  }

  public boolean isRunning() {
    return endTime <= 0 && !isSkipped(myResult) && !isFailed(myResult);
  }

  public boolean hasWarnings() {
    return myWarnings.get() > 0 || (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.WARNING);
  }

  public boolean hasInfos() {
    return myInfos.get() > 0 || (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.INFO);
  }

  public boolean isFailed() {
    return isFailed(myResult) || myErrors.get() > 0 || (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.ERROR);
  }

  @Nullable
  public EventResult getResult() {
    return myResult;
  }

  public ExecutionNodeImpl setResult(@Nullable EventResult result) {
    assert myIsCorrectThread.get();
    myResult = result;
    return reapplyParentFilterIfRequired(null);
  }

  public boolean isAutoExpandNode() {
    return myAutoExpandNode;
  }

  //@ApiStatus.Experimental
  public boolean isAlwaysLeaf() {
    return myAlwaysLeaf;
  }

  //@ApiStatus.Experimental
  public void setAlwaysLeaf(boolean alwaysLeaf) {
    myAlwaysLeaf = alwaysLeaf;
  }

  public void setNavigatable(@Nullable Navigatable navigatable) {
    assert myIsCorrectThread.get();
    myNavigatable = navigatable;
  }

  @Override
  @Nonnull
  public List<Navigatable> getNavigatables() {
    if (myNavigatable != null) {
      return Collections.singletonList(myNavigatable);
    }
    if (myResult == null) return Collections.emptyList();

    if (myResult instanceof FailureResult) {
      List<Navigatable> result = new SmartList<>();
      for (Failure failure : ((FailureResult)myResult).getFailures()) {
        ContainerUtil.addIfNotNull(result, failure.getNavigatable());
      }
      return result;
    }
    return Collections.emptyList();
  }

  public void setIconProvider(@Nonnull Supplier<? extends Image> iconProvider) {
    myPreferredIconValue = NullableLazyValue.createValue(iconProvider::get);
  }

  /**
   * @return the top most node whose parent structure has changed. Returns null if only node itself needs to be updated.
   */
  @Nullable
  public ExecutionNodeImpl reportChildMessageKind(MessageEvent.Kind kind) {
    assert myIsCorrectThread.get();
    if (kind == MessageEvent.Kind.ERROR) {
      myErrors.incrementAndGet();
    }
    else if (kind == MessageEvent.Kind.WARNING) {
      myWarnings.incrementAndGet();
    }
    else if (kind == MessageEvent.Kind.INFO) {
      myInfos.incrementAndGet();
    }
    return reapplyParentFilterIfRequired(null);
  }

  @Nullable
    //@ApiStatus.Experimental
  ExecutionNodeImpl findFirstChild(@Nonnull Predicate<? super ExecutionNodeImpl> filter) {
    assert myIsCorrectThread.get();
    //noinspection SSBasedInspection
    return myChildrenList.stream().filter(filter).findFirst().orElse(null);
  }

  private @BuildEventsNls.Hint String getCurrentHint() {
    assert myIsCorrectThread.get();
    int warnings = myWarnings.get();
    int errors = myErrors.get();
    if (warnings > 0 || errors > 0) {
      String errorHint = errors > 0 ? BuildBundle.message("build.event.message.errors", errors) : "";
      String warningHint = warnings > 0 ? BuildBundle.message("build.event.message.warnings", warnings) : "";
      String issuesHint = !errorHint.isEmpty() && !warningHint.isEmpty() ? errorHint + ", " + warningHint : errorHint + warningHint;
      ExecutionNodeImpl parent = getParent();
      if (parent == null || parent.getParent() == null) {
        if (isRunning()) {
          return StringUtil.notNullize(myHint) + "  " + issuesHint;
        }
        else {
          return BuildBundle.message("build.event.message.with", StringUtil.notNullize(myHint), issuesHint);
        }
      }
      else {
        return StringUtil.notNullize(myHint) + " " + issuesHint;
      }
    }
    else {
      return myHint;
    }
  }

  private Image getCurrentIcon() {
    if (myPreferredIconValue != null) {
      return myPreferredIconValue.getValue();
    }
    else if (myResult instanceof MessageEventResult) {
      return getIcon(((MessageEventResult)myResult).getKind());
    }
    else {
      return isRunning()
             ? NODE_ICON_RUNNING
             : isFailed(myResult) ? NODE_ICON_ERROR : isSkipped(myResult) ? NODE_ICON_SKIPPED : myErrors.get() > 0 ? NODE_ICON_ERROR : myWarnings.get() > 0 ? NODE_ICON_WARNING : NODE_ICON_OK;
    }
  }

  public static boolean isFailed(@Nullable EventResult result) {
    return result instanceof FailureResult;
  }

  public static boolean isSkipped(@Nullable EventResult result) {
    return result instanceof SkippedResult;
  }

  public static Image getEventResultIcon(@Nullable EventResult result) {
    if (result == null) {
      return NODE_ICON_RUNNING;
    }
    if (isFailed(result)) {
      return NODE_ICON_ERROR;
    }
    if (isSkipped(result)) {
      return NODE_ICON_SKIPPED;
    }
    return NODE_ICON_OK;
  }

  private static Image getIcon(MessageEvent.Kind kind) {
    switch (kind) {
      case ERROR:
        return NODE_ICON_ERROR;
      case WARNING:
        return NODE_ICON_WARNING;
      case INFO:
        return NODE_ICON_INFO;
      case STATISTICS:
        return NODE_ICON_STATISTICS;
      case SIMPLE:
        return NODE_ICON_SIMPLE;
    }
    return NODE_ICON_DEFAULT;
  }
}
