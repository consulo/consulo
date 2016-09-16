/*
 * Copyright 2013-2014 consulo.io
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

namespace * consulo.execution.testframework.thrift.runner

service TestInterface {
  void runStarted();

  void runFinished();

  void suiteStarted(1:string name, 2:string location);

  void suiteTestCount(1:i32 count);

  void suiteFinished(1:string name);

  void testStarted(1:string name, 2:string location);

  void testFailed(1:string name, 2:string message, 3:string trace, 4:bool testError, 5:string actual, 6:string expected)

  void testIgnored(1:string name, 2:string comment, 3:string trace);

  void testOutput(1:string name, 2:string text, 3:bool stdOut);

  void testFinished(1:string name, 2:i64 time);
}