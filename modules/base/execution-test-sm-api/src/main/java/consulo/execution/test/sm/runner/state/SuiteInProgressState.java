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
package consulo.execution.test.sm.runner.state;

import consulo.execution.test.sm.runner.SMTestProxy;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class SuiteInProgressState extends TestInProgressState {
    private final SMTestProxy mySuiteProxy;
    private Boolean isDefectWasReallyFound = null; // null - is unset

    public SuiteInProgressState(@Nonnull SMTestProxy suiteProxy) {
        mySuiteProxy = suiteProxy;
    }

    ///**
    // * If any of child failed proxy also is defect
    // * @return
    // */
    @Override
    public boolean isDefect() {
        if (isDefectWasReallyFound != null) {
            return isDefectWasReallyFound;
        }

        //Test suit fails if any of its tests fails
        List<? extends SMTestProxy> children = mySuiteProxy.getChildren();
        for (SMTestProxy child : children) {
            if (child.isDefect()) {
                isDefectWasReallyFound = true;
                return true;
            }
        }

        //cannot cache because one of child tests may fail in future
        return false;
    }

    @Override
    public boolean wasTerminated() {
        return false;
    }

    @Override
    public Magnitude getMagnitude() {
        return Magnitude.RUNNING_INDEX;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "SUITE PROGRESS";
    }
}
