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
package consulo.ide.tipOfDay.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.tipOfDay.TipOfDayContributor;
import consulo.ide.tipOfDay.impl.localize.TipOfDayLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-03-16
 */
@ExtensionImpl
public class DefaultTipOfDayContributor implements TipOfDayContributor {
    @Override
    public void contribute(@Nonnull Consumer<LocalizeValue> consumer) {
        consumer.accept(TipOfDayLocalize.welcome());
        consumer.accept(TipOfDayLocalize.gotoclass());

        consumer.accept(TipOfDayLocalize.codecompletion());

        consumer.accept(TipOfDayLocalize.findusages());
        consumer.accept(TipOfDayLocalize.quickjavadoc());

        consumer.accept(TipOfDayLocalize.gotodeclaration());

        consumer.accept(TipOfDayLocalize.filestructurepopup());

        consumer.accept(TipOfDayLocalize.rename());
        consumer.accept(TipOfDayLocalize.overrideimplementmethods());

        consumer.accept(TipOfDayLocalize.smarttypecompletion());

        consumer.accept(TipOfDayLocalize.tabinlookups());

        consumer.accept(TipOfDayLocalize.tabineditorclose());
        consumer.accept(TipOfDayLocalize.altinsertineditor());

        consumer.accept(TipOfDayLocalize.selectin());

        consumer.accept(TipOfDayLocalize.speedsearch());

        consumer.accept(TipOfDayLocalize.escape());
        consumer.accept(TipOfDayLocalize.surroundwith());

        consumer.accept(TipOfDayLocalize.smarttypeafternew());

        consumer.accept(TipOfDayLocalize.gotoimplementation());

        consumer.accept(TipOfDayLocalize.ctrlw());

        consumer.accept(TipOfDayLocalize.introducevariable());

        consumer.accept(TipOfDayLocalize.commentcode());


        consumer.accept(TipOfDayLocalize.externaljavadoc());

        consumer.accept(TipOfDayLocalize.smarttypecasting());

        consumer.accept(TipOfDayLocalize.ctrld());
        consumer.accept(TipOfDayLocalize.livetemplates());
        consumer.accept(TipOfDayLocalize.variablenamecompletion());

        consumer.accept(TipOfDayLocalize.parameterinfo());
        consumer.accept(TipOfDayLocalize.jumptolastedit());
        consumer.accept(TipOfDayLocalize.highlightusagesinfile());
        consumer.accept(TipOfDayLocalize.layoutcode());
        consumer.accept(TipOfDayLocalize.localvcs());
        consumer.accept(TipOfDayLocalize.contextinfo());
        consumer.accept(TipOfDayLocalize.recentfiles());

        consumer.accept(TipOfDayLocalize.nextpreverror());
        consumer.accept(TipOfDayLocalize.insertlivetemplate());
        consumer.accept(TipOfDayLocalize.methodseparators());
        consumer.accept(TipOfDayLocalize.codecompletionmiddle());

        consumer.accept(TipOfDayLocalize.methodupdown());
        consumer.accept(TipOfDayLocalize.joinlines());
        consumer.accept(TipOfDayLocalize.copyclass());

        consumer.accept(TipOfDayLocalize.clipboardstack());
        consumer.accept(TipOfDayLocalize.hierarchybrowser());
        consumer.accept(TipOfDayLocalize.breakpointspeedmenu());
        consumer.accept(TipOfDayLocalize.evaluateexpressionineditor());
        consumer.accept(TipOfDayLocalize.wordcompletion());
        consumer.accept(TipOfDayLocalize.quickjavadocinlookups());

        consumer.accept(TipOfDayLocalize.dotetcinlookups());

        consumer.accept(TipOfDayLocalize.menuitemsdescriptions());
        consumer.accept(TipOfDayLocalize.wildcardsinnavigationpopups());
        consumer.accept(TipOfDayLocalize.moveinnertoupper());
        consumer.accept(TipOfDayLocalize.introducevariableincompletecode());

        consumer.accept(TipOfDayLocalize.gotosymbol());
        consumer.accept(TipOfDayLocalize.recentchanges());

        consumer.accept(TipOfDayLocalize.imagefilecompletion());
        consumer.accept(TipOfDayLocalize.createpropertytag());
        consumer.accept(TipOfDayLocalize.quickswitchscheme());

        consumer.accept(TipOfDayLocalize.completestatement());
        consumer.accept(TipOfDayLocalize.camelprefixesinnavigationpopups());
        consumer.accept(TipOfDayLocalize.ctrlshifti());

        consumer.accept(TipOfDayLocalize.completioninhtml());
        consumer.accept(TipOfDayLocalize.copypastereference());
        consumer.accept(TipOfDayLocalize.moveupdown());
        consumer.accept(TipOfDayLocalize.selectrundebugconfiguration());
        consumer.accept(TipOfDayLocalize.ctrlshiftiforlookup());

        consumer.accept(TipOfDayLocalize.propertiescompletion());
        consumer.accept(TipOfDayLocalize.showappliedstyles());
        consumer.accept(TipOfDayLocalize.imageslookup());
        consumer.accept(TipOfDayLocalize.renamecssselector());
        consumer.accept(TipOfDayLocalize.navbar());
        consumer.accept(TipOfDayLocalize.changesview());
        consumer.accept(TipOfDayLocalize.antivirus());
        consumer.accept(TipOfDayLocalize.mavenquickopen());
        consumer.accept(TipOfDayLocalize.movetochangelist());
        consumer.accept(TipOfDayLocalize.eclipsequickopen());
        consumer.accept(TipOfDayLocalize.showusages());
        consumer.accept(TipOfDayLocalize.gotoaction());
        consumer.accept(TipOfDayLocalize.gotoinspection());
        consumer.accept(TipOfDayLocalize.searchinsettings());
        consumer.accept(TipOfDayLocalize.completemethod());
        consumer.accept(TipOfDayLocalize.highlightimplements());
        consumer.accept(TipOfDayLocalize.recentsearch());
        consumer.accept(TipOfDayLocalize.codecompletioninsearch());
        consumer.accept(TipOfDayLocalize.highlightmethodexitpoint());
        consumer.accept(TipOfDayLocalize.highlightthrows());
        consumer.accept(TipOfDayLocalize.quickfixrightarrow());
        consumer.accept(TipOfDayLocalize.navigatetofilepath());
        consumer.accept(TipOfDayLocalize.issuenavigation());
        consumer.accept(TipOfDayLocalize.umlclassdiagram());
        consumer.accept(TipOfDayLocalize.coloreditingincss());
        consumer.accept(TipOfDayLocalize.createtestintentionaction());
        consumer.accept(TipOfDayLocalize.columnselection());
        consumer.accept(TipOfDayLocalize.colorfiles());
        consumer.accept(TipOfDayLocalize.copywithnoselection());
        consumer.accept(TipOfDayLocalize.movefiletochangelist());
        consumer.accept(TipOfDayLocalize.spellchecker());
        consumer.accept(TipOfDayLocalize.spellcheckerdictionaries());
        consumer.accept(TipOfDayLocalize.vcsquicklist());
        consumer.accept(TipOfDayLocalize.switcher());
        consumer.accept(TipOfDayLocalize.dragtoopen());
        consumer.accept(TipOfDayLocalize.closeothers());
        consumer.accept(TipOfDayLocalize.enterdirectoryingotofile());
        consumer.accept(TipOfDayLocalize.gotolineinfile());
        consumer.accept(TipOfDayLocalize.annotationsanddiffs());
        consumer.accept(TipOfDayLocalize.dirdiff());
        consumer.accept(TipOfDayLocalize.jardiff());
        consumer.accept(TipOfDayLocalize.showhidesidebars());
        consumer.accept(TipOfDayLocalize.excludefromproject());
        consumer.accept(TipOfDayLocalize.codecompletionnoshift());
        consumer.accept(TipOfDayLocalize.commitctrlk());
        consumer.accept(TipOfDayLocalize.findreplacetoggle());
        consumer.accept(TipOfDayLocalize.scopesintodo());
        consumer.accept(TipOfDayLocalize.previewtodo());
        consumer.accept(TipOfDayLocalize.fixdoccomment());
        consumer.accept(TipOfDayLocalize.selecttasks());
        consumer.accept(TipOfDayLocalize.runconfigfolders());
        consumer.accept(TipOfDayLocalize.speedsearchinlivetemplates());
        consumer.accept(TipOfDayLocalize.editregexp());

        consumer.accept(TipOfDayLocalize.emmet());
        consumer.accept(TipOfDayLocalize.escapecharactersinresourcebundle());
        consumer.accept(TipOfDayLocalize.lineendings());
        consumer.accept(TipOfDayLocalize.lineendingsfolder());
        consumer.accept(TipOfDayLocalize.refactorthis());
        consumer.accept(TipOfDayLocalize.favoritestoolwindow1());
        consumer.accept(TipOfDayLocalize.favoritestoolwindow2());
    }
}
