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
 
type PrintHandler = (data: Uint32Array, start: number, end: number) => void;

class InsertFixAddon extends TerminalAddon<TerminalMixin> {
	
    activateCallback(terminal: Terminal): void {
		const core = this.$core as any;
		const oldPrintHandler = core._inputHandler._parser._printHandler;
		const newPrintHandler = (data: Uint32Array, start: number, end: number) => this.printHandler(core._inputHandler, data, start, end, oldPrintHandler); 		
		(this.$core as any)._inputHandler._parser.setPrintHandler(newPrintHandler);
		this._disposables.push({dispose : () => {core._inputHandler._parser._printHandler = oldPrintHandler;}});		
	}
	
	printHandler(inputHandler: any, data: Uint32Array, start: number, end: number, printHandler : PrintHandler): void {
   		const wraparoundMode = inputHandler._coreService.decPrivateModes.wraparound;
		const insertMode = inputHandler._coreService.modes.insertMode;
		
		if (insertMode && wraparoundMode) {
			const buffer = inputHandler._bufferService.buffer;
			const bufferRow = buffer.lines.get(buffer.y + buffer.ybase);			
			const printedLength = end-start;
			let  trimmedLength = bufferRow.getTrimmedLength();
			
			//If the inserted characters would overflow the current liner
			if (buffer.x!=trimmedLength && trimmedLength+printedLength > bufferRow.length) {
				let range = buffer.getWrappedRangeForLine(buffer.y + buffer.ybase)
				range.first = buffer.y + buffer.ybase;
				
				let src;
				if (range.first==range.last) {
					src = bufferRow;
				} else {
					src = buffer.lines.get(range.last);
					trimmedLength = src.getTrimmedLength();
				}
				
				//If the inserted characters would overflow the last line in wrapped range
				if (trimmedLength+printedLength > src.length) {
					//Then wrap the next row
					if (range.last == buffer._rows - 1) {
						inputHandler._bufferService.scroll(inputHandler._eraseAttrData(), true);
					}
					const dst = buffer.lines.get(range.last+1);
					dst.isWrapped = true;
					dst.copyCellsFrom(src, trimmedLength-printedLength, 0, printedLength);
					inputHandler._dirtyRowTracker.markDirty(buffer.y+1);
				}
				
				//Allocate space for the characters to be inserted
				//Wrap-move characters in page memory to the next line
				for (let y=range.last;y>range.first;y--) {
					let dst = src;
					src= buffer.lines.get(y-1);
					dst.insertCells(0, printedLength, buffer.getNullCell(inputHandler._eraseAttrData()));
					dst.copyCellsFrom(src, buffer._cols-printedLength, 0, printedLength);
					inputHandler._dirtyRowTracker.markDirty(y);
				}				
			}
		}
		
		printHandler(data, start, end);
	}
	
}

type Constructor<T = {}> = new (...args: any[]) => T;
export function XTermInsertFixMixin<TBase extends Constructor<TerminalMixin>>(Base: TBase) {
  return class XTermInsertFixMixin extends Base {
	connectedCallback() {
		super.connectedCallback();
		let addon = new InsertFixAddon();
		addon.$=this;
		this.node.terminal.loadAddon(addon);		
	}
  }	
}
