/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
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
import { Terminal } from 'xterm'
import { TerminalMixin, TerminalAddon} from './xterm-element';

interface IClipboardMixin extends TerminalMixin {
	useSystemClipboard: String;
	pasteWithRightClick: Boolean;
	pasteWithMiddleClick: Boolean;
	copySelection: Boolean;
}

class ClipboardAddon extends TerminalAddon<IClipboardMixin> {
	   
    activateCallback(terminal: Terminal): void {
		let _internalClipboard : string | undefined = undefined;
		
		//function (String) : void
		let writeText = (s: string) => {
			_internalClipboard = s;
			if (this.$.useSystemClipboard== 'readwrite' || this.$.useSystemClipboard=='write') {
				try {
					navigator.clipboard.writeText(s);
				} catch(error) {
					console.error(error);
				}
			}
		}

		//function () : Promise<String>
		let readText = () => {
			if (this.$.useSystemClipboard=='readwrite') {
				try {
					return navigator.clipboard.readText();
				} catch(error) {
					console.error(error);
				}
			}
			return Promise.resolve(_internalClipboard);
		}
		
		let onEvent = (event: string, listener:any) => {
			terminal.element!.addEventListener(event, listener);
			return {dispose : () => terminal.element!.removeEventListener(event, listener)};
		};
		
		let paste = (text: string | undefined) => {
			if (text!==undefined) {
				terminal.paste(text);
			}
		};
		
		let initializer = ()=>{
			//paste with right click
			this._disposables.push(onEvent('contextmenu', (ev:any) => {
				if (this.$.pasteWithRightClick && !terminal.options.rightClickSelectsWord) {
					ev.preventDefault();
					if (_internalClipboard!==undefined) readText().then(paste);
				}
			})); 
			
			//paste with middle click
			this._disposables.push(onEvent('auxclick', (ev:any) => {
					if (this.$.pasteWithMiddleClick && ev.button == 1) {
						if (_internalClipboard!==undefined) readText().then(paste);
					}
			}));
		};
		
		this.$node.addEventListener('terminal-initialized', initializer);
		
		this._disposables = [
		{dispose: () => this.$node.removeEventListener('terminal-initialized', initializer)},
		
		//ctrl-C ctrl-V
		this.$node.customKeyEventHandlers.register({key:'c', ctrlKey:true}, undefined),
		this.$node.customKeyEventHandlers.register({key:'v', ctrlKey:true}, undefined),
		
		//copy selection
		terminal.onSelectionChange(() => {
			if (this.$.copySelection && terminal.hasSelection()) {
				writeText(terminal.getSelection());
			}
		}),
			
		
		];
			   
    }

}

type Constructor<T = {}> = new (...args: any[]) => T;
export function XTermClipboardMixin<TBase extends Constructor<TerminalMixin>>(Base: TBase) {
  return class XTermClipboardMixin extends Base implements IClipboardMixin {
	
	useSystemClipboard: String = "write";
	pasteWithRightClick!: Boolean;
	pasteWithMiddleClick!: Boolean;
	copySelection!: Boolean;
	
	connectedCallback() {
		super.connectedCallback();		
		
		let addon = new ClipboardAddon();
		addon.$=this;
		this.node.terminal.loadAddon(addon);		
	}
		
  };
}
