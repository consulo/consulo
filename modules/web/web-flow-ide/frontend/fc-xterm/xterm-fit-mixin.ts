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
import { Terminal, IDisposable } from 'xterm'
import { TerminalMixin } from './xterm-element';
import { FitAddon as FitAddonBase } from 'xterm-addon-fit';


interface FitMixin extends TerminalMixin {
	fitOnResize: boolean;	
};

class FitAddon extends FitAddonBase {
  _disposables! : IDisposable[];
  $! : FitMixin;
    
  constructor() {
	super();
	const super_fit = this.fit.bind(this);
	this.fit = () => {
		super_fit();
		requestAnimationFrame(()=>this.__unsetWidth());
	};
  }
  
  __unsetWidth() {
    let viewport = (this.$.node.terminal as any)._core.viewport;
    if (viewport) viewport._viewportElement.style.width='unset';
  }

  activate(terminal: Terminal): void {
  	super.activate(terminal);
           
    let _fitOnResize = () => {
        if (this.$.fitOnResize) this.fit();
    }
         
    window.addEventListener('resize', _fitOnResize);

	this._disposables = [];
    this._disposables.push({dispose : () => {
	  window.removeEventListener('resize', _fitOnResize);
    }});
  }
  
  dispose(): void {
     this._disposables.forEach(d => d.dispose());
	 super.dispose();
  }	
}

type Constructor<T = {}> = new (...args: any[]) => T;
export function XTermFitMixin<TBase extends Constructor<TerminalMixin>>(Base: TBase) {
  return class XTermFitMixin extends Base {
        
    _fitAddon? : FitAddon;
    fitOnResize: boolean = true;
    
    connectedCallback() {
      super.connectedCallback();   
      
      let addon = new FitAddon();
	  addon.$=this;
	  this.node.terminal.loadAddon(addon);
      
	  this._fitAddon = addon;
      this.fit();

      this.node.addEventListener("terminal-initialized", () => this.fit());
    }

    fit() {
      this._fitAddon!.proposeDimensions();
	  window.setTimeout(()=>{ 
	      try {
	        this._fitAddon!.fit();
	      } catch (e) {
	        console.warn(e);
	      }
	  });
    }
       
  };
}
