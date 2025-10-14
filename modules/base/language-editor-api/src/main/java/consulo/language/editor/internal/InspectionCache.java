/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.internal;

import consulo.component.util.localize.BundleBase;
import consulo.language.Language;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2025-10-13
 */
public class InspectionCache {
    private record InspectionAndHighlightDisplayKey(InspectionTool tool, HighlightDisplayKey highlightDisplayKey) {
    }

    private static final Logger LOG = Logger.getInstance(InspectionCache.class);

    private final Map<String, HighlightDisplayKey> myHighlightDisplayNameToKeyMap = new HashMap<>();
    private final Map<String, HighlightDisplayKey> myHighlightDisplayIdToKeyMap = new HashMap<>();
    private final Map<HighlightDisplayKey, LocalizeValue> myHighlightDisplayKeyToDisplayNameMap = new HashMap<>();
    private final Map<HighlightDisplayKey, String> myHighlightDisplayKeyToAlternativeIDMap = new HashMap<>();

    private final List<InspectionAndHighlightDisplayKey> myTools = new ArrayList<>();

    private final Map<Language, Map<ProblemHighlightType, HighlightInfoType>> myLikeToolMapping = new HashMap<>();

    public void eat(InspectionTool inspectionTool) {
        if (inspectionTool instanceof LocalInspectionTool localInspectionTool) {
            HighlightDisplayKey key = register(localInspectionTool.getShortName(),
                localInspectionTool.getDisplayName(),
                localInspectionTool.getID(),
                localInspectionTool.getAlternativeID());

            if (key == null) {
                return;
            }

            String errorMessage = checkTool(localInspectionTool);
            if (errorMessage != null) {
                return;
            }

            myTools.add(new InspectionAndHighlightDisplayKey(inspectionTool, key));

            checkRuler(key, localInspectionTool);
        }
        else if (inspectionTool instanceof GlobalInspectionTool globalInspectionTool) {
            HighlightDisplayKey key = register(inspectionTool.getShortName(), inspectionTool.getDisplayName());
            if (key == null) {
                return;
            }

            myTools.add(new InspectionAndHighlightDisplayKey(inspectionTool, key));
        }
        else {
            LOG.warn("Unknown inspection tool implementation: " + inspectionTool.getClass());
        }
    }

    public InspectionCache lock() {
        // FIXME maybe make immutable version of this object?
        return this;
    }

    public List<InspectionToolWrapper<?>> wrapTools() {
        List<InspectionToolWrapper<?>> wrappers = new ArrayList<>(myTools.size());
        for (InspectionAndHighlightDisplayKey entry : myTools) {
            InspectionTool key = entry.tool();
            HighlightDisplayKey value = entry.highlightDisplayKey();

            if (key instanceof LocalInspectionTool localInspectionTool) {
                wrappers.add(new LocalInspectionToolWrapper(localInspectionTool, value));
            } else if (key instanceof GlobalInspectionTool globalInspectionTool) {
                wrappers.add(new GlobalInspectionToolWrapper(globalInspectionTool, value));
            }
        }
        return wrappers;
    }

    @Nonnull
    public HighlightInfoType getControlledHighlightType(@Nonnull Language language, @Nonnull ProblemHighlightType type) {
        Map<ProblemHighlightType, HighlightInfoType> map = myLikeToolMapping.get(language);
        if (map != null) {
            HighlightInfoType infoType = map.get(type);
            if (infoType != null) {
                return infoType;
            }
        }

        HighlightInfoType infoType = ProblemHighlightTypeInspectionRuler.REGISTRY.get(type);
        return Objects.requireNonNull(infoType);
    }

    private void checkRuler(HighlightDisplayKey key, LocalInspectionTool localInspectionTool) {
        if (!(localInspectionTool instanceof ProblemHighlightTypeInspectionRuler ruler)) {
            return;
        }

        ProblemHighlightType type = ruler.getControllableHighlightType();
        Language language = localInspectionTool.getLanguage();

        if (language == null) {
            LOG.warn("ProblemHighlightTypeInspectionRuler return null for #getLanguage() - see " + ruler);
            return;
        }

        HighlightInfoType rawType = ProblemHighlightTypeInspectionRuler.REGISTRY.get(type);
        if (rawType == null) {
            LOG.warn("ProblemHighlightTypeInspectionRuler return unsupported ProblemHighlightType" + ruler);

            return;
        }

        myLikeToolMapping.computeIfAbsent(language, l -> new HashMap<>())
            .put(type, new HighlightInfoTypeSeverityByKey(key, rawType.getAttributesKey()));
    }

    private static String checkTool(@Nonnull LocalInspectionTool localInspectionTool) {
        String message = null;
        try {
            String id = localInspectionTool.getID();
            if (!LocalInspectionTool.isValidID(id)) {
                message = BundleBase.format("Inspection ''{0}'' is disabled: ID ''{1}'' not matches ''{2}'' pattern.", localInspectionTool.getShortName(), id, LocalInspectionTool.VALID_ID_PATTERN);
            }
        }
        catch (Throwable t) {
            message = BundleBase.format("Inspection ''{0}'' is disabled: {1}.", localInspectionTool.getShortName(), t.getMessage());
        }
        if (message != null) {
            LOG.error(message);
        }
        return message;
    }

    @Nullable
    public HighlightDisplayKey register(@Nonnull String name, @Nonnull LocalizeValue displayName) {
        return register(name, displayName, name);
    }

    public HighlightDisplayKey find(@Nonnull String name) {
        return myHighlightDisplayNameToKeyMap.get(name);
    }

    @Nullable
    public HighlightDisplayKey findById(@Nonnull String id) {
        HighlightDisplayKey key = myHighlightDisplayIdToKeyMap.get(id);
        if (key != null) {
            return key;
        }
        key = myHighlightDisplayNameToKeyMap.get(id);
        if (key != null && key.getID().equals(id)) {
            return key;
        }
        return null;
    }

    @Nullable
    public HighlightDisplayKey register(
        @Nonnull String name,
        @Nonnull LocalizeValue displayName,
        @Nonnull String id,
        @Nullable String alternativeID
    ) {
        HighlightDisplayKey key = register(name, displayName, id);
        if (alternativeID != null) {
            myHighlightDisplayKeyToAlternativeIDMap.put(key, alternativeID);
        }
        return key;
    }

    @Nonnull
    public LocalizeValue getDisplayNameByKey(@Nullable HighlightDisplayKey key) {
        if (key == null) {
            return LocalizeValue.of();
        }
        else {
            LocalizeValue computable = myHighlightDisplayKeyToDisplayNameMap.get(key);
            return computable == null ? LocalizeValue.of() : computable;
        }
    }

    @Nullable
    public HighlightDisplayKey register(@Nonnull String name, @Nonnull LocalizeValue displayName, @Nonnull String id) {
        if (find(name) != null) {
            LOG.info("Key with name \'" + name + "\' already registered");
            return null;
        }
        HighlightDisplayKey highlightDisplayKey = new HighlightDisplayKey(name, id);

        myHighlightDisplayKeyToDisplayNameMap.put(highlightDisplayKey, displayName);

        myHighlightDisplayNameToKeyMap.put(name, highlightDisplayKey);

        if (!Comparing.equal(id, name)) {
            myHighlightDisplayIdToKeyMap.put(id, highlightDisplayKey);
        }

        return highlightDisplayKey;
    }
}
