// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.versionControlSystem.distributed;

import consulo.logging.Logger;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MultiMessage<Aspect> {
    private static final Logger LOG = Logger.getInstance(MultiMessage.class);

    private final Collection<Aspect> myAllValues;
    private final Function<Aspect, String> myLogPresentation;
    private final Function<Aspect, String> myShortPresentation;
    private final boolean myAspectInPrefix;
    private final Map<Aspect, String> myMessages = new LinkedHashMap<>();
    private final String myLineSeparator;

    public MultiMessage(Collection<Aspect> allValues,
                        Function<Aspect, String> logPresentation,
                        Function<Aspect, String> shortPresentation,
                        boolean aspectInPrefix) {
        this(allValues, logPresentation, shortPresentation, aspectInPrefix, true);
    }

    public MultiMessage(Collection<Aspect> allValues,
                        Function<Aspect, String> logPresentation,
                        Function<Aspect, String> shortPresentation,
                        boolean aspectInPrefix,
                        boolean html) {
        myAllValues = allValues;
        myLogPresentation = logPresentation;
        myShortPresentation = shortPresentation;
        myAspectInPrefix = aspectInPrefix;
        myLineSeparator = html ? "<br/>\n" : "\n";
    }

    public MultiMessage<Aspect> append(Aspect aspect, String message) {
        if (!myAllValues.contains(aspect)) {
            LOG.error("The aspect value " + myLogPresentation.apply(aspect) + " is unexpected: " + myAllValues);
            return this;
        }
        if (myMessages.containsKey(aspect)) {
            LOG.error("Duplicate aspect value " + myLogPresentation.apply(aspect) + " reporting message [" + message + "]");
        }
        myMessages.put(aspect, message);
        return this;
    }

    public String asString() {
        if (myMessages.isEmpty()) {
            return "";
        }
        if (myAllValues.size() == 1) {
            return myMessages.values().iterator().next();
        }
        Map<String, List<Aspect>> grouped = new LinkedHashMap<>();
        for (Map.Entry<Aspect, String> entry : myMessages.entrySet()) {
            grouped.computeIfAbsent(entry.getValue().trim(), key -> new ArrayList<>()).add(entry.getKey());
        }
        if (grouped.size() == 1 && myAllValues.size() == myMessages.size()) {
            return myMessages.values().iterator().next();
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<Aspect>> entry : grouped.entrySet()) {
            List<String> presentableNames = new ArrayList<>();
            for (Aspect aspect : entry.getValue()) {
                presentableNames.add(myShortPresentation.apply(aspect));
            }
            String names = DvcsUtil.joinWithAnd(presentableNames, 5);
            lines.add(myAspectInPrefix
                ? DistributedVcsLocalize.multiMessageLinePrefixForm(names, entry.getKey()).get()
                : DistributedVcsLocalize.multiMessageLineSuffixForm(names, entry.getKey()).get());
        }
        return String.join(myLineSeparator, lines);
    }

    @Override
    public String toString() {
        return myMessages.toString();
    }
}
