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

import jakarta.annotation.Nullable;

/**
 * @author Roman Chernyatchik
 */
public class TestErrorState extends TestFailedState {
    public TestErrorState(@Nullable String localizedMessage, @Nullable String stackTrace) {
        super(localizedMessage, stackTrace);
    }

    @Override
    public Magnitude getMagnitude() {
        return Magnitude.ERROR_INDEX;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "TEST ERROR";
    }
}
