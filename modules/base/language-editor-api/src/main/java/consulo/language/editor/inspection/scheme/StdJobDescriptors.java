/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.inspection.scheme;

import consulo.language.editor.inspection.localize.InspectionLocalize;

public class StdJobDescriptors {
  public final JobDescriptor BUILD_GRAPH = new JobDescriptor(InspectionLocalize.inspectionProcessingJobDescriptor().get());
  public final JobDescriptor[] BUILD_GRAPH_ONLY = {BUILD_GRAPH};
  public final JobDescriptor FIND_EXTERNAL_USAGES = new JobDescriptor(InspectionLocalize.inspectionProcessingJobDescriptor1().get());
  public final JobDescriptor LOCAL_ANALYSIS = new JobDescriptor(InspectionLocalize.inspectionProcessingJobDescriptor2().get());
  public final JobDescriptor[] LOCAL_ANALYSIS_ARRAY = {LOCAL_ANALYSIS};
}
