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
import { TerminalMixin, TerminalAddon } from './xterm-element';

export interface IConsoleMixin extends TerminalMixin {
	escapeEnabled: Boolean;
	insertMode: Boolean;
	readonly currentLine: string;
	prompt: string;
}

class ConsoleAddon extends TerminalAddon<IConsoleMixin> {

	__yPrompt : Number = -1;
	
	get currentLine() : string {
		let inputHandler = ((this.$core) as any)._inputHandler;
		let buffer = inputHandler._bufferService.buffer;
		let range = buffer.getWrappedRangeForLine(buffer.y + buffer.ybase);
		let line = "";
		for (let i=range.first; i<=range.last; i++) {
			line += buffer.lines.get(i).translateToString();
		}
		line = line.replace(/\s+$/,"");
		if (this.$.prompt && this.__yPrompt==range.first) line = line.substring(this.$.prompt.length);
		return line;
	}
	
	activateCallback(terminal: Terminal): void {
		
		var inputHandler = ((this.$core) as any)._inputHandler;
		
		let promptLength = () => this.$.prompt ? this.$.prompt.length : 0;
		 
		let scanEOL = (function(this: any) {
			let buffer = this._bufferService.buffer;
			let col = this._bufferService.buffer.lines.get(buffer.ybase+buffer.y).getTrimmedLength();
			this._activeBuffer.x = Math.min(col, terminal.cols);
		}).bind(inputHandler);
		
		let cursorForwardWrapped = (function(this: any, params: any = [0]) {
			let buffer = this._bufferService.buffer;
			let x = buffer.x;
			let y = buffer.y;
			for (let i=(params[0] || 1); i>0; --i) {
				let apply=false;
				if (buffer.x>=this._bufferService.cols-1) {
					let next = buffer.lines.get(buffer.y+buffer.ybase+1);
					if (next && next.isWrapped) {
						this.cursorNextLine({params:1});
						apply=true;
					} else if (i==1) {
						buffer.x=this._bufferService.cols;
						apply=true;
					}
				} else if (buffer.x < buffer.lines.get(buffer.y+buffer.ybase).getTrimmedLength()) {
					this.cursorForward({params:[1]});
					apply=true;
				}
				
				if (!apply) {
					buffer.x=x;
					buffer.y=y;
					return false;
				}
			}
			return true;
		}).bind(inputHandler);
			
		let cursorBackwardWrapped = (function(this: any, params : any = [0]) {
			let buffer = this._bufferService.buffer;
			let x = buffer.x;
			let y = buffer.y;
			let apply=false;
			
			for (let i=0; i< (params[0] || 1); i++) {
				let line = buffer.lines.get(buffer.y+buffer.ybase);
				if (!line.isWrapped && buffer.x< 1+promptLength()) {
					break;
				} else if (buffer.x>0) {
					apply=true;
					this.cursorBackward({params:[1]});
				} else if (line.isWrapped) {
					apply=true;
					this.cursorPrecedingLine({params:[1]});
					scanEOL();
					--this._activeBuffer.x;
				} else {
					apply=false;
					break;
				}
			}
			if (!apply) {
				buffer.x=x;
				buffer.y=y;
			}
			return apply;
		}).bind(inputHandler);
		
		let deleteChar = (function(this: any, params: any = [0]) {
			let buffer = this._bufferService.buffer;
			let x = buffer.x;
			let y = buffer.y;
			for (let j=0; j< (params[0] || 1); j++) {
				this.deleteChars({params:[1]});
				let line = buffer.lines.get(buffer.ybase+buffer.y);
				let range = buffer.getWrappedRangeForLine(buffer.y + buffer.ybase)
				for (let i=buffer.y+buffer.ybase; i<range.last; i++) {
					let next = buffer.lines.get(buffer.ybase+buffer.y+1);
					line.set(line.length-1, next.get(0));
					this.cursorNextLine({params:1});
					this.deleteChars({params:[1]});
					line = next;
				}
				if (line.isWrapped && line.getTrimmedLength()==0) {
					line.isWrapped=false;
					if (y==range.last) {
						y--;
						x=this._bufferService.cols;
					}
				}
				buffer.y=y;
				buffer.x=x;
			}
		}).bind(inputHandler);

		let cursorEnd = (function(this: any) {
			let buffer = this._bufferService.buffer;
			let y, range = buffer.getWrappedRangeForLine(y = buffer.y + buffer.ybase);
			if (range.last!=y) this.cursorNextLine({params:[range.last-y]});
			scanEOL();
		}).bind(inputHandler);
		
		let cursorHome = (function(this: any) {
			let buffer = this._bufferService.buffer;
			let y, range = buffer.getWrappedRangeForLine(y = buffer.y + buffer.ybase);
			if (range.first!=y) {
				this.cursorPrecedingLine({params:[y-range.first]});
			} else {
				this.cursorCharAbsolute({params:[1]});
			}
			promptLength() && cursorForwardWrapped([promptLength()]);
		}).bind(inputHandler);
		
		let backspace = (function(this: any) {
			let buffer = this._bufferService.buffer;
			let line = buffer.lines.get(buffer.ybase+buffer.y);
			if (cursorBackwardWrapped()) deleteChar();
			if (buffer.x==this._bufferService.cols-1 && line.getTrimmedLength() == 0) {
				line.isWrapped = false;
			}
		}).bind(inputHandler);
		
		const node = this.$node;
		let linefeed = () => {
			node.dispatchEvent(new CustomEvent('line', {detail: this.currentLine}));
		};
		
		let eraseInLine = (function(this: any, params: any) {
			let buffer = this._bufferService.buffer;
			let x = buffer.x;
			let y = buffer.y;

			this.eraseInLine({params});
			let range = buffer.getWrappedRangeForLine(buffer.y + buffer.ybase);
			
			if (params[0] == 1 || params[0] == 2) {
				//Start of line through cursor 
				for (let i=range.first; i<y; i++) {
					buffer.y = i; 
					this.eraseInLine({params: [2]});
				}
			}
			
			if (params[0] == 0 || params[0] == 2) {
				//Cursor to end of line
				for (let i=y+1; i<=range.last; i++) {
					buffer.y = i; 
					this.eraseInLine({params: [2]});
					buffer.lines.get(buffer.ybase+buffer.y).isWrapped=false;
				}
			}

			buffer.x = x;
			buffer.y = y;
		}).bind(inputHandler);
		
		let hasModifiers = (ev:KeyboardEvent) => ev.shiftKey || ev.altKey || ev.metaKey || ev.ctrlKey;

		function probe_others<T>(callback: ((params: T[]) => void)): ((params: T[]) => boolean) {
			// execute callback and also probe for other handlers
			return params => {callback(params); return false;}
		};
		
		this._disposables = [
		terminal.parser.registerCsiHandler({prefix: '<', final: 'H'}, probe_others(cursorHome)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='Home' && !hasModifiers(ev), ()=> terminal.write('\x1b[<H')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'E'}, probe_others(cursorEnd)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='End' && !hasModifiers(ev), ()=> terminal.write('\x1b[<E')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'L'}, probe_others(cursorBackwardWrapped)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowLeft' && !hasModifiers(ev), ()=> terminal.write('\x1b[<L')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'R'}, probe_others(cursorForwardWrapped)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowRight' && !hasModifiers(ev), ()=> terminal.write('\x1b[<R')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'B'}, probe_others(backspace)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='Backspace' && !hasModifiers(ev), ()=> terminal.write('\x1b[<B')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'D'}, probe_others(deleteChar)),
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='Delete' && !hasModifiers(ev), ()=> terminal.write('\x1b[<D')),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'K'}, probe_others(eraseInLine)),
		
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='Insert' && !hasModifiers(ev), ev=>{
			this.$.insertMode = !this.$.insertMode;
		}),
		
		this.$node.customKeyEventHandlers.register(ev=> ev.key=='Enter' && !hasModifiers(ev), ()=>{
			terminal.write('\x1b[<N\n');
		}),
		
		this.$node.customKeyEventHandlers.register(ev=> [
			'ArrowUp',
			'ArrowDown',
			'F1', 'F2', 'F3', 'F4', 'F7', 'F8', 'F9', 'F10', 'F11',
		].includes(ev.key), ev=>{ev.preventDefault(); return false;}),
		
		this.$node.customKeyEventHandlers.register(ev=> [
			'Escape'
		].includes(ev.key), () => this.$.escapeEnabled),
		
		this.$node.customKeyEventHandlers.register(ev=> [
			'F5',
			'F6',
			'F12'
		].includes(ev.key), undefined),
		
		terminal.parser.registerCsiHandler({prefix: '<', final: 'N'}, probe_others(linefeed))

		];
		
	}
	
	writePrompt() {
		if (!this.$.prompt) return;
		
		let inputHandler = ((this.$core) as any)._inputHandler;
		let buffer = inputHandler._bufferService.buffer;
		let range = buffer.getWrappedRangeForLine(buffer.y + buffer.ybase);
		
		let prepare = "";
		let restore = this.$.insertMode ? "\x1b[4h" : "\x1b[4l"
		
		if (this.__yPrompt == range.first) {
			//prompt has been written in this line
			prepare+="\x1b[4l"; //Override mode
		} else {
			//prompt has not been written in this line
			this.__yPrompt = range.first;
			prepare+="\x1b[s";  //Save cursor position
			prepare+="\x1b[4h"; //Insert mode
			restore+="\x1b[u";  //Restore cursor position
			restore+="\x1b[<"+this.$.prompt.length+"R"; //cursor forward wrapped
		}
		
		prepare+="\x1b[<H\x1b[G"; //Cursor Home Logical, Cursor Horizontal Absolute
		this.$.prompt && this.$.node.terminal.write(prepare+this.$.prompt+restore);
	}
}

type Constructor<T = {}> = new (...args: any[]) => T;
export function XTermConsoleMixin<TBase extends Constructor<TerminalMixin>>(Base: TBase) {
  return class XTermConsoleMixin extends Base implements IConsoleMixin {
	
	_consoleAddon? : ConsoleAddon;
	escapeEnabled!: Boolean;
	prompt!: string;
	
	connectedCallback() {
		super.connectedCallback();
		
		this._consoleAddon = new ConsoleAddon();
		this._consoleAddon.$=this;
		this.node.terminal.loadAddon(this._consoleAddon);
	}
	
	get insertMode(): Boolean {
		return this.node.terminal.modes.insertMode;
	}

	set insertMode(value: Boolean) {
		if (value) {
			this.node.terminal.write('\x1b[4h\x1b[3 q');
		} else {
			this.node.terminal.write('\x1b[4l\x1b[2 q');
		}
	}

	get currentLine() : string {
		return this._consoleAddon!.currentLine;
	}

	writePrompt() {
		//execute writePrompt with blocking semantics 
		this.node.terminal.write('', ()=>this._consoleAddon!.writePrompt());
	}
	
 }
}