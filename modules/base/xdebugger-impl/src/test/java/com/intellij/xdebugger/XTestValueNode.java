package com.intellij.xdebugger;

import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodePresentationConfigurator;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValuePresentationUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Semaphore;

public class XTestValueNode extends XValueNodePresentationConfigurator.ConfigurableXValueNodeImpl {
  public String myName;
  public String myType;
  public String myValue;
  public boolean myHasChildren;

  public XFullValueEvaluator myFullValueEvaluator;

  private final Semaphore myFinished = new Semaphore(0);

  @Override
  public void applyPresentation(@Nullable Image icon,
                                @Nonnull XValuePresentation valuePresentation,
                                boolean hasChildren) {
    myType = valuePresentation.getType();
    myValue = XValuePresentationUtil.computeValueText(valuePresentation);
    myHasChildren = hasChildren;

    myFinished.release();
  }

  @Override
  public void setFullValueEvaluator(@Nonnull XFullValueEvaluator fullValueEvaluator) {
    myFullValueEvaluator = fullValueEvaluator;
  }

  @Override
  public boolean isObsolete() {
    return false;
  }

  public void waitFor(long timeoutInMillis) throws InterruptedException {
    if (!XDebuggerTestUtil.waitFor(myFinished, timeoutInMillis)) throw new AssertionError("Waiting timed out");
  }
}
