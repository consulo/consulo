/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.inspection;

import consulo.language.editor.inspection.reference.RefEntity;
import org.jspecify.annotations.Nullable;

/**
 * Where a batch inspection run reports what it found. Unlike
 * {@link ProblemDescriptionsProcessor#addProblemElement(RefEntity, CommonProblemDescriptor...)} the caller says
 * whether suppressed problems still have to be filtered out, which it knows and the sink does not.
 *
 * @author VISTALL
 */
public interface InspectionProblemSink extends ProblemDescriptionsProcessor {
    void addProblemElement(@Nullable RefEntity refEntity, boolean filterSuppressed, CommonProblemDescriptor... descriptors);
}
