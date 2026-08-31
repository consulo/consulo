/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2025 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.xterm;

import com.flowingcode.vaadin.addons.xterm.utils.StateMemoizer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Add-on which preserves the client-side state when the component is removed
 * from the UI then reattached later on. The problem here is that when the
 * {@link XTerm} server-side component is detached from the UI, the xterm.js client-side
 * component is destroyed along with its state. When the {@link XTerm} component
 * is later re-attached to the UI, a new unconfigured xterm.js is created on the
 * client-side.
 * <p></p>
 * To use this addon, simply create the addon then make sure to call all {@link ITerminal}
 * and {@link ITerminalOptions} methods via this addon:
 * <pre>
 * final XTerm xterm = new XTerm();
 * final PreserveStateAddon addon = new PreserveStateAddon(xterm);
 * addon.writeln("Hello!");
 * addon.setPrompt("$ ");
 * addon.writePrompt();
 * </pre>
 */
public class PreserveStateAddon extends TerminalAddon
    implements ITerminal, ITerminalOptions {

    /**
     * The xterm to delegate all calls to.
     */
    private final XTerm xterm;
    /**
     * Remembers everything that was printed into the xterm and what the user typed in.
     */
    private final StringBuilder scrollbackBuffer = new StringBuilder();
    /**
     * All commands are properly applied before the first attach; they're just
     * not preserved after subsequent detach/attach.
     */
    private boolean wasDetachedOnce = false;
    /**
     * Used to re-apply all options to the xterm after it has been reattached back to the UI.
     * Otherwise, the options would not be applied to the client-side xterm.js component.
     */
    private final StateMemoizer optionsMemoizer;

    /**
     * Delegate all option setters through this delegate, which is the {@link #optionsMemoizer} proxy.
     * That will allow us to re-apply the settings when the xterm is re-attached.
     * <p></p>
     * For example, calling {@link ITerminalOptions#setBellSound(String)}
     * on this addon will pass through the call to this delegate, which in turn passes
     * the call to {@link #optionsMemoizer} which remembers the call and passes
     * it to {@link #xterm}.
     * <p></p>
     * After the xterm.js is re-attached, we simply call {@link StateMemoizer#apply()}
     * to apply all changed setters again to xterm.js, to make sure xterm.js is
     * configured.
     */
    private final ITerminalOptions optionsDelegate;

    public PreserveStateAddon(XTerm xterm) {
        super(xterm);
        this.xterm = Objects.requireNonNull(xterm);
        optionsMemoizer = new StateMemoizer(xterm, ITerminalOptions.class);
        optionsDelegate = (ITerminalOptions) optionsMemoizer.getProxy();
        xterm.addAttachListener(e -> {
            if (wasDetachedOnce) {
                optionsMemoizer.apply();
                xterm.write(scrollbackBuffer.toString());
                xterm.writePrompt();
            }
        });
        xterm.addDetachListener(e -> wasDetachedOnce = true);
        xterm.addLineListener(e -> {
            // add the prompt to the scrollback buffer
            scrollbackBuffer.append(xterm.getPrompt());
            // also make sure that any user input ends up in the scrollback buffer.
            scrollbackBuffer.append(e.getLine());
            scrollbackBuffer.append('\n');
        });
    }

    @Override
    public void blur() {
        xterm.blur();
    }

    @Override
    public void focus() {
        xterm.focus();
    }

    @Override
    public CompletableFuture<Boolean> hasSelection() {
        return xterm.hasSelection();
    }

    @Override
    public CompletableFuture<String> getSelection() {
        return xterm.getSelection();
    }

    @Override
    public void clearSelection() {
        xterm.clearSelection();
    }

    @Override
    public void select(int column, int row, int length) {
        xterm.select(column, row, length);
    }

    @Override
    public void selectAll() {
        xterm.selectAll();
    }

    @Override
    public void selectLines(int start, int end) {
        xterm.selectLines(start, end);
    }

    @Override
    public void scrollLines(int amount) {
        xterm.scrollLines(amount);
    }

    @Override
    public void scrollPages(int pageCount) {
        xterm.scrollPages(pageCount);
    }

    @Override
    public void scrollToTop() {
        xterm.scrollToTop();
    }

    @Override
    public void scrollToBottom() {
        xterm.scrollToBottom();
    }

    @Override
    public void scrollToLine(int line) {
        xterm.scrollToLine(line);
    }

    @Override
    public void clear() {
        xterm.clear();
        scrollbackBuffer.delete(0, scrollbackBuffer.length());
    }

    @Override
    public void write(String data) {
        xterm.write(data);
        scrollbackBuffer.append(data);
    }

    @Override
    public void writeln(String data) {
        xterm.writeln(data);
        scrollbackBuffer.append(data);
        scrollbackBuffer.append('\n');
    }

    @Override
    public void paste(String data) {
        xterm.paste(data);
    }

    @Override
    public void refresh(int start, int end) {
        xterm.refresh(start, end);
    }

    @Override
    public void reset() {
        xterm.reset();
        scrollbackBuffer.delete(0, scrollbackBuffer.length());
    }

    @Override
    public void resize(int columns, int rows) {
        xterm.resize(columns, rows);
    }

    @Override
    public void setBellSound(String value) {
        optionsDelegate.setBellSound(value);
    }

    @Override
    public void setBellStyle(BellStyle value) {
        optionsDelegate.setBellStyle(value);
    }

    @Override
    public void setCursorBlink(boolean value) {
        optionsDelegate.setCursorBlink(value);
    }

    @Override
    public void setCursorStyle(CursorStyle value) {
        optionsDelegate.setCursorStyle(value);
    }

    @Override
    public void setCursorWidth(int value) {
        optionsDelegate.setCursorWidth(value);
    }

    @Override
    public void setDrawBoldTextInBrightColors(boolean value) {
        optionsDelegate.setDrawBoldTextInBrightColors(value);
    }

    @Override
    public void setFastScrollModifier(FastScrollModifier value) {
        optionsDelegate.setFastScrollModifier(value);
    }

    @Override
    public void setFastScrollSensitivity(int number) {
        optionsDelegate.setFastScrollSensitivity(number);
    }

    @Override
    public void setFontSize(int number) {
        optionsDelegate.setFontSize(number);
    }

    @Override
    public void setFontFamily(String fontFamily) {
        optionsDelegate.setFontFamily(fontFamily);
    }

    @Override
    public void setFontWeight(int value) {
        optionsDelegate.setFontWeight(value);
    }

    @Override
    public void setFontWeightBold(int value) {
        optionsDelegate.setFontWeightBold(value);
    }

    @Override
    public void setLetterSpacing(int value) {
        optionsDelegate.setLetterSpacing(value);
    }

    @Override
    public void setLineHeight(int value) {
        optionsDelegate.setLineHeight(value);
    }

    @Override
    public void setMacOptionIsMeta(boolean value) {
        optionsDelegate.setMacOptionIsMeta(value);
    }

    @Override
    public void setMacOptionClickForcesSelection(boolean value) {
        optionsDelegate.setMacOptionClickForcesSelection(value);
    }

    @Override
    public void setMinimumContrastRatio(int value) {
        optionsDelegate.setMinimumContrastRatio(value);
    }

    @Override
    public void setTheme(TerminalTheme theme) {
        optionsDelegate.setTheme(theme);
    }

    @Override
    public void setRendererType(RendererType value) {
        optionsDelegate.setRendererType(value);
    }

    @Override
    public void setRightClickSelectsWord(boolean value) {
        optionsDelegate.setRightClickSelectsWord(value);
    }

    @Override
    public void setScreenReaderMode(boolean value) {
        optionsDelegate.setScreenReaderMode(value);
    }

    @Override
    public void setScrollback(int value) {
        optionsDelegate.setScrollback(value);
    }

    @Override
    public void setScrollSensitivity(int value) {
        optionsDelegate.setScrollSensitivity(value);
    }

    @Override
    public void setTabStopWidth(int value) {
        optionsDelegate.setTabStopWidth(value);
    }

    @Override
    public void setWordSeparator(String value) {
        optionsDelegate.setWordSeparator(value);
    }

    /**
     * {@link ITerminalConsole#setPrompt(String)}
     */
    public void setPrompt(String prompt) {
        xterm.setPrompt(prompt);
    }

    /**
     * {@link ITerminalConsole#getPrompt()}
     */
    public String getPrompt() {
        return xterm.getPrompt();
    }

    public void writePrompt() {
        xterm.writePrompt();
    }

    public String getScrollbackBuffer() {
        return scrollbackBuffer.toString();
    }

    public XTerm getXTerm() {
        return xterm;
    }
}
