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
package consulo.ai;

import java.util.List;

/**
 * A question the model asks the user in the middle of an answer.
 *
 * @param question    the full question
 * @param header      a short label of the question
 * @param options     the choices offered; the user may also answer in their own words
 * @param multiSelect whether more than one choice may be picked
 * @author VISTALL
 * @since 2026-10-13
 */
public record AIQuestion(String question, String header, List<AIQuestionOption> options, boolean multiSelect) {
}
