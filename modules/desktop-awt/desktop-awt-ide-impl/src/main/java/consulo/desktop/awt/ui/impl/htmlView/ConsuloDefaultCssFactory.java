/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.htmlView;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import cz.vutbr.web.css.StyleSheet;
import org.cobraparser.css.DefaultCssFactory;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public class ConsuloDefaultCssFactory extends DefaultCssFactory {
    private static final String STD_CSS =
        """
            html, address,
            blockquote,
            body, dd, div,
            dl, dt, fieldset, form,
            frame, frameset,
            h1, h2, h3, h4,
            h5, h6, noframes,
            ol, p, ul, center,
            dir, hr, menu, pre   { display: block; unicode-bidi: embed; }
            li              { display: list-item }
            head            { display: none }
            table           { display: table }
            tr              { display: table-row }
            thead           { display: table-header-group }
            tbody           { display: table-row-group }
            tfoot           { display: table-footer-group }
            col             { display: table-column }
            colgroup        { display: table-column-group }
            td, th          { display: table-cell; }
            caption         { display: table-caption }
            th              { font-weight: bolder; text-align: center }
            caption         { text-align: center }
            body            { margin: 8px; }
            h1              { font-size: 2em; margin: .67em 0 }
            h2              { font-size: 1.5em; margin: .75em 0 }
            h3              { font-size: 1.17em; margin: .83em 0 }
            h4, p,
            blockquote, ul,
            fieldset, form,
            ol, dl, dir,
            menu            { margin: 1em 0 }
            h5              { font-size: .83em; margin: 1.5em 0 }
            h6              { font-size: .75em; margin: 1.67em 0 }
            h1, h2, h3, h4,
            h5, h6, b,
            strong          { font-weight: bolder }
            blockquote      { margin-left: 40px; margin-right: 40px }
            i, cite, em,
            var, address    { font-style: italic }
            pre, tt, code,
            kbd, samp       { font-family: monospace }
            pre             { white-space: pre }
            button, textarea,
            input, select   { display:inline-block; }
            big             { font-size: 1.17em }
            small, sub, sup { font-size: .83em }
            sub             { vertical-align: sub }
            sup             { vertical-align: super }
            table           { border-spacing: 2px; }
            thead, tbody,
            tfoot           { vertical-align: middle }
            td, th, tr      { vertical-align: inherit }
            s, strike, del  { text-decoration: line-through }
            hr              { border: 1px inset }
            ol, ul, dir,
            menu, dd        { margin-left: 40px }
            ol              { list-style-type: decimal }
            ol ul, ul ol,
            ul ul, ol ol    { margin-top: 0; margin-bottom: 0 }
            u, ins          { text-decoration: underline }
            br:before       { content: "\\A" }
            center          { text-align: center }
            :link, :visited { text-decoration: underline }
            :focus          { outline: thin dotted invert }
            BDO[DIR="ltr"]  { direction: ltr; unicode-bidi: bidi-override }
            BDO[DIR="rtl"]  { direction: rtl; unicode-bidi: bidi-override }
            *[DIR="ltr"]    { direction: ltr; unicode-bidi: embed }
            *[DIR="rtl"]    { direction: rtl; unicode-bidi: embed }


            html, body {
                background: var(--consulo-background);
                color: var(--consulo-color);
                font-family: var(--consulo-font-family), Arial, sans-serif;
            }
    """;

    private static final String USER_CSS =
        """
            html   { overflow: auto; }
            a[href]{ cursor: pointer; color: var(--consulo-color-blue); text-decoration: underline; }
            label{cursor:default}
            script { display: none; }
            style  { display: none; }
            option { display: none; }
            br     { display: block; }
            hr     { display: block; margin-top: 1px solid; }
            ul     { margin-left: 0; padding-left: 40px; }
            abbr, acronym   { font-variant: small-caps; letter-spacing: 0.1em }
            table {text-align: left}
            article, aside, footer, header, hgroup, main, nav, section {display:block}
    """;

    private Boolean myDarkColor;
    private StyleSheet myStandardCSS;
    private StyleSheet
        myUserCSS;

    private void validate() {
        boolean dark = StyleManager.get().getCurrentStyle().isDark();
        if (!Objects.equals(myDarkColor, dark)) {
            myStandardCSS = load(STD_CSS);
            myUserCSS = load(USER_CSS);

            myDarkColor = dark;
        }
    }
    
    @Override
    public StyleSheet getStandardCSS(boolean b) {
        validate();
        return myStandardCSS;
    }

    @Override
    public StyleSheet getUserCSS(boolean xhtml) {
        validate();
        return myUserCSS;
    }

    private StyleSheet load(String body) {
        try {
            body = body.replace("var(--consulo-background)", ColorUtil.toHtmlColor(UIUtil.getLabelBackground()));
            body = body.replace("var(--consulo-color)", ColorUtil.toHtmlColor(UIUtil.getLabelForeground()));
            body = body.replace("var(--consulo-color-blue)", ColorUtil.toHtmlColor(JBColor.BLUE));
            body = body.replace("var(--consulo-font-family)", StringUtil.QUOTER.apply(UIUtil.getLabelFont().getFamily()));
            return parseStyle(body);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
