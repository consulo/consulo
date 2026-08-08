/*-
 * #%L
 * XTerm Selection Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	  http://www.apache.org/licenses/LICENSE-2.0
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
import { IConsoleMixin } from './xterm-console-mixin';

interface ISelectionMixin extends TerminalMixin {
	keyboardSelectionEnabled: boolean;
}

class SelectionAddon extends TerminalAddon<ISelectionMixin> {

	__selectionLength!: number;
	__selectionAnchor?: number;
	__selectionRight:  boolean = true;
	
	activateCallback(terminal: Terminal): void {
		
		var inputHandler = ((this.$core) as any)._inputHandler;
				
		let resetSelection = () => {
			this.__selectionAnchor = undefined;
		}
		
		let clearSelection = () => {
			if (!this.$.keyboardSelectionEnabled) return;
			resetSelection();
			terminal.clearSelection();
		}
		
		let ensureSelection = () => {
			if (this.__selectionAnchor === undefined) {
				let buffer = inputHandler._bufferService.buffer;
				this.__selectionAnchor = buffer.y * terminal.cols + buffer.x;;
				this.__selectionLength = 0;
			}
		};
		
		let moveSelection = (dx:number, dy:number=0) => {
			if (!this.$.keyboardSelectionEnabled) return;
			ensureSelection();
			
			let newSelectionLength = this.__selectionLength;
			if (this.__selectionRight) {
				newSelectionLength += dx + dy * terminal.cols;
			} else {
				newSelectionLength -= dx + dy * terminal.cols;
			}
			
			if (newSelectionLength<0) {
				newSelectionLength = -newSelectionLength;
				this.__selectionRight = !this.__selectionRight;
			}

			let newSelectionStart = this.__selectionAnchor!;
			if (!this.__selectionRight) {
				newSelectionStart -= newSelectionLength;
			}
						
			if (newSelectionStart<0) return;
			if (newSelectionStart+newSelectionLength>terminal.buffer.active.length*terminal.cols) return;
			
			let row  = Math.floor(newSelectionStart / terminal.cols);
			let col  = newSelectionStart % terminal.cols;
		 	
			this.__selectionLength = newSelectionLength;
			terminal.select(col,row,newSelectionLength);
		};
		
		let selectLeft  = () => moveSelection(-1);
		let selectRight = () => moveSelection(+1);
		let selectUp	= () => moveSelection(0,-1);
		let selectDown  = () => moveSelection(0,+1);
		
		let promptLength = () => (this.$ as unknown as IConsoleMixin).prompt?.length || 0;
		
		let selectHome  = () => {
			if (!this.$.keyboardSelectionEnabled) return;
			
			let buffer = (terminal.buffer.active as any)._buffer;
			let range = buffer.getWrappedRangeForLine(buffer.ybase+buffer.y);
			
			let pos = terminal.getSelectionPosition() || {start: {y: buffer.ybase+buffer.y, x: buffer.x}};
			
			resetSelection();
			ensureSelection();
			let dx = range.first * terminal.cols - this.__selectionAnchor!;
			if (pos.start.y != range.first || pos.start.x != promptLength()) {
				dx+= promptLength();
			}
			
			moveSelection(dx);
		};
		
		let selectEnd = () => {
			if (!this.$.keyboardSelectionEnabled) return;
			
			let buffer = (terminal.buffer.active as any)._buffer;
			let range = buffer.getWrappedRangeForLine(buffer.ybase+buffer.y);
			
			resetSelection();
			ensureSelection();
			moveSelection(range.last * terminal.cols + buffer.lines.get(range.last).getTrimmedLength() - this.__selectionAnchor!);
		};
		
		let deleteSelection = (ev: KeyboardEvent) => {
			if (!this.$.keyboardSelectionEnabled) return;
			if (this.__selectionAnchor!==undefined) {
				let buffer = (terminal.buffer.active as any)._buffer;
				let range = buffer.getWrappedRangeForLine(buffer.ybase+buffer.y);				
				let pos = terminal.getSelectionPosition();					
				if (pos && pos.start.y>=range.first && pos.end.y<=range.last) {
					if (!this.__selectionRight) {
						//cursor backward wrapped
						terminal.write("\x1b[<" + this.__selectionLength + "L");
					}
					//delete characters wrapped
					terminal.write("\x1b[<" + this.__selectionLength + "D");
					ev.stopImmediatePropagation();
				}
			}
			clearSelection();
		};
			
		let hasModifiers = (ev:KeyboardEvent) => ev.shiftKey || ev.altKey || ev.metaKey || ev.ctrlKey;
		
		this._disposables = [
			(this.$core as any).coreService.onUserInput(() => clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowLeft'  && ev.shiftKey, selectLeft),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowRight' && ev.shiftKey, selectRight),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowUp'	 && ev.shiftKey, selectUp),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowDown'  && ev.shiftKey, selectDown),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='Home'	     && ev.shiftKey, selectHome),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='End'		 && ev.shiftKey, selectEnd),
			
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowLeft'  && !hasModifiers(ev), clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowRight' && !hasModifiers(ev), clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowUp'	 && !hasModifiers(ev), clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='ArrowDown'  && !hasModifiers(ev), clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='Home'	     && !hasModifiers(ev), clearSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='End'		 && !hasModifiers(ev), clearSelection),
			
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='Delete'	 && !hasModifiers(ev), deleteSelection),
			this.$node.customKeyEventHandlers.register(ev=> ev.key=='Backspace'  && !hasModifiers(ev), deleteSelection),
		];
		
	}

}

type Constructor<T = {}> = new (...args: any[]) => T;
export function XTermSelectionMixin<TBase extends Constructor<TerminalMixin>>(Base: TBase) {
  return class XTermSelectionMixin extends Base implements ISelectionMixin {

	keyboardSelectionEnabled: boolean = true;
	
	connectedCallback() {
		super.connectedCallback();
		let addon = new SelectionAddon();
		addon.$=this;
		this.node.terminal.loadAddon(addon);
	}
	
 }
}
