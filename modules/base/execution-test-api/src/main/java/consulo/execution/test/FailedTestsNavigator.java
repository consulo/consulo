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
package consulo.execution.test;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.localize.ExecutionLocalize;
import consulo.ui.ex.OccurenceNavigator;

import java.util.ArrayList;
import java.util.List;

public class FailedTestsNavigator implements OccurenceNavigator {
  private TestFrameworkRunningModel myModel;

  @Override
  public boolean hasNextOccurence() {
    return myModel != null && getNextOccurenceInfo().hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return myModel != null && getPreviousOccurenceInfo().hasNextOccurence();
  }

  @Override
  public OccurenceNavigator.OccurenceInfo goNextOccurence() {
    FailedTestInfo result = getNextOccurenceInfo();
    myModel.selectAndNotify(result.getDefect());
    return new OccurenceInfo(TestsUIUtil.getOpenFileDescriptor(result.myDefect, myModel), result.getDefectNumber(),
                             result.getDefectsCount());
  }

  public void setModel(TestFrameworkRunningModel model) {
    myModel = model;
    Disposer.register(myModel, new Disposable() {
      @Override
      public void dispose() {
        myModel = null;
      }
    });
  }

  @Override
  public OccurenceNavigator.OccurenceInfo goPreviousOccurence() {
    FailedTestInfo result = getPreviousOccurenceInfo();
    myModel.selectAndNotify(result.getDefect());
    return new OccurenceInfo(TestsUIUtil.getOpenFileDescriptor(result.myDefect, myModel), result.getDefectNumber(),
                             result.getDefectsCount());
  }

  @Override
  public String getNextOccurenceActionName() {
    return ExecutionLocalize.nextFaledTestActionName().get();
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return ExecutionLocalize.prevFaledTestActionName().get();
  }

  private FailedTestInfo getNextOccurenceInfo() {
    return new NextFailedTestInfo().execute();
  }

  private FailedTestInfo getPreviousOccurenceInfo() {
    return new PreviousFailedTestInfo().execute();
  }

  protected abstract class FailedTestInfo {
    private AbstractTestProxy myDefect = null;
    private List<AbstractTestProxy> myAllTests;
    private List<AbstractTestProxy> myDefects;

    public AbstractTestProxy getDefect() {
      return myDefect;
    }

    private int getDefectNumber() {
      return myDefect == null ? getDefectsCount() : myDefects.indexOf(myDefect) + 1;
    }

    public FailedTestInfo execute() {
      myAllTests = new ArrayList<AbstractTestProxy>(myModel.getRoot().getAllTests());
      myDefects = Filter.DEFECTIVE_LEAF.select(myAllTests);
      AbstractTestProxy selectedTest = myModel.getTreeView().getSelectedTest();
      int selectionIndex = myAllTests.indexOf(selectedTest);
      if (selectionIndex == -1)
        return this;
      AbstractTestProxy defect = findNextDefect(selectionIndex);
      if (defect == null)
        return this;
      if (defect != selectedTest) {
        myDefect = defect;
        return this;
      }
      int defectIndex = myDefects.indexOf(defect);
      if (defectIndex == -1 || defectIndex == getBoundIndex())
        return this;
      myDefect = myDefects.get(nextIndex(defectIndex));
      return this;
    }



    private AbstractTestProxy findNextDefect(int startIndex) {
      for (int i = nextIndex(startIndex); 0 <= i && i < myAllTests.size(); i = nextIndex(i)) {
        AbstractTestProxy nextDefect = myAllTests.get(i);
        if (Filter.DEFECTIVE_LEAF.shouldAccept(nextDefect))
          return nextDefect;
      }
      return null;
    }

    protected abstract int nextIndex(int defectIndex);

    protected abstract int getBoundIndex();

    protected int getDefectsCount() {
      return myDefects.size();
    }

    private boolean hasNextOccurence() {
      return myDefect != null;
    }
  }

  private class NextFailedTestInfo extends FailedTestInfo {
    @Override
    protected int nextIndex(int defectIndex) {
      return defectIndex + 1;
    }

    @Override
    protected int getBoundIndex() {
      return getDefectsCount() - 1;
    }
  }

  private class PreviousFailedTestInfo extends FailedTestInfo {
    @Override
    protected int nextIndex(int defectIndex) {
      return defectIndex - 1;
    }

    @Override
    protected int getBoundIndex() {
      return 0;
    }
  }
}
