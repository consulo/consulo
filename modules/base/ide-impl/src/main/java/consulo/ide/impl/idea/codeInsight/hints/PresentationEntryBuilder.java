// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.component.util.PluginExceptionUtil;
import consulo.language.editor.inlay.InlayActionData;
import consulo.language.editor.inlay.InlayActionPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PresentationEntryBuilder {
    private final TinyTree<?> state;
    private final Class<?> providerClass;
    private final List<InlayPresentationEntry> entries = new ArrayList<>();
    private InlayMouseArea currentClickArea;
    private byte parentIndexToSwitch = -1;
    private byte indexOfClosestParentList = -1;

    public PresentationEntryBuilder(TinyTree<?> state, Class<?> providerClass) {
        this.state = state;
        this.providerClass = providerClass;
    }

    public InlayPresentationEntry[] buildPresentationEntries() {
        buildSubtreeForIdOnly((byte) 0);
        if (entries.isEmpty()) {
            throw PluginExceptionUtil.createByClass(
                "No entries in the tree",
                new RuntimeException(providerClass.getCanonicalName()),
                providerClass
            );
        }
        assert !entries.isEmpty();
        return entries.toArray(new InlayPresentationEntry[0]);
    }

    private void buildSubtreeForIdOnly(byte index) {
        state.processChildren(index, new Predicate<Byte>() {
            @Override
            public boolean test(Byte childIndex) {
                buildNode(childIndex);
                return true;
            }
        });
    }

    private void buildNode(byte childIndex) {
        byte tag = state.getBytePayload(childIndex);
        switch (tag) {
            case InlayTags.LIST_TAG:
                buildSubtreeForIdOnly(childIndex);
                break;
            case InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG:
            case InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG:
                selectFromList(childIndex, false);
                break;
            case InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG:
            case InlayTags.COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG:
                selectFromList(childIndex, true);
                break;
            case InlayTags.COLLAPSE_BUTTON_TAG: {
                byte savedIndexToSwitch = parentIndexToSwitch;
                try {
                    parentIndexToSwitch = indexOfClosestParentList;
                    buildSubtreeForIdOnly(childIndex);
                }
                finally {
                    parentIndexToSwitch = savedIndexToSwitch;
                }
                break;
            }
            case InlayTags.TEXT_TAG: {
                Object dataPayload = state.getDataPayload(childIndex);
                if (dataPayload instanceof String) {
                    InlayMouseArea area = currentClickArea;
                    TextInlayPresentationEntry entry =
                        new TextInlayPresentationEntry((String) dataPayload, parentIndexToSwitch, area);
                    addEntry(entry);
                    if (area != null) {
                        area.getEntries().add(entry);
                    }
                }
                else if (dataPayload instanceof ActionWithContent) {
                    ActionWithContent awc = (ActionWithContent) dataPayload;
                    InlayMouseArea area = currentClickArea != null
                        ? currentClickArea
                        : mouseAreaIfNotZombie(awc.getActionData());
                    TextInlayPresentationEntry entry =
                        new TextInlayPresentationEntry((String) awc.getContent(), parentIndexToSwitch, area);
                    addEntry(entry);
                    if (area != null) {
                        area.getEntries().add(entry);
                    }
                }
                else {
                    throw new IllegalStateException("Illegal payload for text tag: " + dataPayload);
                }
                break;
            }
            case InlayTags.CLICK_HANDLER_SCOPE_TAG: {
                InlayActionData actionData = (InlayActionData) state.getDataPayload(childIndex);
                InlayMouseArea clickArea = new InlayMouseArea(actionData);
                InlayMouseArea saved = currentClickArea;
                this.currentClickArea = clickArea;
                state.processChildren(childIndex, new Predicate<Byte>() {
                    @Override
                    public boolean test(Byte ch) {
                        buildNode(ch);
                        return true;
                    }
                });
                this.currentClickArea = saved;
                break;
            }
            default:
                throw new IllegalStateException("Unknown tag: " + tag);
        }
    }

    private InlayMouseArea mouseAreaIfNotZombie(InlayActionData actionData) {
        InlayActionPayload payload = actionData.getPayload();
        if (payload instanceof InlayActionPayload.PsiPointerInlayActionPayload
            && ((InlayActionPayload.PsiPointerInlayActionPayload) payload).getPointer() instanceof ZombieSmartPointer) {
            return null;
        }
        return new InlayMouseArea(actionData);
    }

    private void selectFromList(byte index, boolean collapsed) {
        byte savedIndexOfClosestParentList = indexOfClosestParentList;
        indexOfClosestParentList = index;
        try {
            byte branchTag = collapsed
                ? InlayTags.COLLAPSIBLE_LIST_COLLAPSED_BRANCH_TAG
                : InlayTags.COLLAPSIBLE_LIST_EXPANDED_BRANCH_TAG;
            state.processChildren(index, new Predicate<Byte>() {
                @Override
                public boolean test(Byte childIndex) {
                    if (state.getBytePayload(childIndex) == branchTag) {
                        buildSubtreeForIdOnly(childIndex);
                    }
                    return true;
                }
            });
        }
        finally {
            indexOfClosestParentList = savedIndexOfClosestParentList;
        }
    }

    private void addEntry(InlayPresentationEntry presentation) {
        entries.add(presentation);
    }
}
