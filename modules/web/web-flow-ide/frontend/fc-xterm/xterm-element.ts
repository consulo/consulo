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
import { LitElement, html, TemplateResult } from 'lit';

import { Terminal, ITerminalAddon, IDisposable } from 'xterm';

type integer = number;
type KeyboardEventHandler = (event: KeyboardEvent) => void;
type KeyboardEventPredicate = (event: KeyboardEvent) => boolean;

interface CustomKey {
  key?: string;
  code?: string;
  location?: number;
  ctrlKey?: boolean;
  altKey?: boolean;
  metaKey?: boolean;
  shiftKey?: boolean;
}

interface CustomKeyEventHandler {
  predicate:KeyboardEventPredicate;
  handle?: KeyboardEventHandler;
}

interface CustomKeyEventHandlerRegistryDisposable extends IDisposable {
  id: integer;    
  unshift: () => void;
}

//sparse array of CustomKeyEventHandler
class CustomKeyEventHandlerRegistry {
  private handlers: CustomKeyEventHandler[] = [];
  private indexes: number[] = [];
  private next:integer=0;

  register(customKey: CustomKey, handle?: (event: KeyboardEvent) => void): CustomKeyEventHandlerRegistryDisposable;
  register(predicate: KeyboardEventPredicate, handle?: KeyboardEventHandler) : CustomKeyEventHandlerRegistryDisposable;
  register(arg: CustomKey | KeyboardEventPredicate, handle?: KeyboardEventHandler) : CustomKeyEventHandlerRegistryDisposable {    
    let predicate : KeyboardEventPredicate;
    if ((typeof arg) === 'object') {
        let customKey = arg as any;
        //(C) T.J. Crowder  cc-by-sa 4.0
        //https://stackoverflow.com/a/35430956/1297272
        let properties = ['key', 'code', 'location', 'ctrlKey', 'altKey', 'metaKey', 'shiftKey'];      
        predicate  = (ev : KeyboardEvent) => properties.every(property => customKey[property] === (ev as any)[property] || customKey[property]===undefined);
    } else {
        predicate = arg as KeyboardEventPredicate;    
    }
    
    const id = this.next++;
    if (!handle) handle = () => {};
    this.handlers[id] = {predicate, handle};
    this.indexes.push(id);
    return {id, dispose : () => this.remove(id), unshift : () => this.unshift(id)};
  }

  private unshift(id: integer) : void {
    const i= this.indexes.indexOf(id);
    if (i>=0) {
      this.indexes.splice(i, 1);
      this.indexes.unshift(id);
    }
  }
  
  remove(id: integer) : void {
    delete this.handlers[id];
    const i= this.indexes.indexOf(id);
    if (i>=0) this.indexes.splice(i, 1);
  }

  handle(context: XTermElement, ev: KeyboardEvent) : boolean {
    //invoke all the applicable handlers for event

    let listeners : CustomKeyEventHandler[] = [];
    
    for(var i=0;i<this.indexes.length;i++) {
      const h = this.handlers[this.indexes[i]];
      if (h.predicate(ev)) {
        listeners.push(h);
      }
    }

	let stopImmediatePropagation = ev.stopImmediatePropagation.bind(ev);

	let immediatePropagationStopped = false;
	ev.stopImmediatePropagation= () => {
		stopImmediatePropagation();
		immediatePropagationStopped=true;
	};
	
	let handled = listeners.length>0;
	for (var i=0;i<listeners.length;i++) {
		listeners[i].handle?.call(context, ev);
		if (immediatePropagationStopped) break;
	}
	
	if ((ev as any).requestCustomEvent) {
		context.dispatchEvent(new CustomEvent('CustomKey', {detail: ev}));
	}
	
	//https://github.com/FlowingCode/XTermConsoleAddon/issues/59
	let core = (context.terminal as any)._core;
	(core as any)._keyDownSeen = false;
	return handled;
  }

}
	
export interface TerminalMixin {
  connectedCallback(): void;  
  node: XTermElement;
}

export abstract class TerminalAddon<T extends TerminalMixin> implements ITerminalAddon {
	public $! : T;
	protected $node!: XTermElement;
	protected $core!: any;
	protected _disposables! : IDisposable[];

	public readonly activate = (terminal: Terminal): void => {
		this.$node = this.$.node;
		this.$core = (terminal as any)._core;
		this._disposables=[];
		this.activateCallback(terminal);
    };
	
	public readonly dispose = (): void => {
        this._disposables.forEach(d => d.dispose());
    };

	protected abstract activateCallback(terminal: Terminal) : void;
}
	
	
export class XTermElement extends LitElement implements TerminalMixin {
  
  terminal: Terminal;
  disabled: boolean = false;
  node: XTermElement;
  
  bellSound: string;
  bellStyle: 'none' | 'sound'
  
  customKeyEventHandlers: CustomKeyEventHandlerRegistry;
  
  addons : Object = {};

  render(): TemplateResult {
    return html`
        <slot name="terminal-container" @slotchange=${this._slotchange}></slot>        
    `;
  }

  constructor() {
    super();        
    this.customKeyEventHandlers = new CustomKeyEventHandlerRegistry();
    this.terminal = new Terminal();	
	this.node = this;
    
    //https://gist.github.com/literallylara/7ece1983fab47365108c47119afb51c7
    //(C) Lara Sophie Schütt 2016, CC0 
    for(var i=44100*0.1,d="";i--;)d+=String.fromCharCode(~~((Math.sin(i/44100*2*Math.PI*800)+1)*128)); 
    this.bellSound = "data:Audio/WAV;base64,"+btoa("RIFFdataWAVEfmt "+atob("EAAAAAEAAQBErAAARKwAAAEACABkYXRh/////w==")+d);
    this.bellStyle = 'none';
  }

  _onData(e:string) : void {
    //<N CSI is handled by console-feature
    this.terminal.write(e.replace(/\r/g,'\x1b[<N\n'));
  }
  
  connectedCallback() {
    super.connectedCallback();

    let term = this.terminal;
    term.options.convertEol = true;
    
    //onLineFeed doesn't distinguish lines from user input and lines from terminal.write
    term.onData(e=>this._onData(e));
    
    term.onBell(() => {
      if (this.bellStyle == 'sound') {
        new Audio(this.bellSound).play();
      }
    });

    term.attachCustomKeyEventHandler(ev => {
      if (ev.type!=='keydown') return false;
      return !this.customKeyEventHandlers.handle(this, ev); 
    });   
  }
 
  disconnectedCallback() {
    this.terminal.dispose();
    this.terminal = new Terminal();
    super.disconnectedCallback();
  }

  _slotchange() {
    let slot = this.shadowRoot?.querySelector("slot[name='terminal-container']") as HTMLSlotElement;
    requestAnimationFrame(()=>{
        this.terminal.open(slot.assignedNodes()[0] as HTMLElement);
        this.dispatchEvent(new CustomEvent('terminal-initialized'));
    });
  }

  updated(changedProps : any) {
    if (changedProps.has('disabled')) {
      this._disabledChanged(this.disabled);
    }
  }

  _disabledChanged(disabled: boolean) {
    this.terminal.options.disableStdin=disabled;
    this.terminal.write(disabled?"\x1b[?25l":"\x1b[?25h");
  }
    
  registerCustomKeyListener(customKey: CustomKey) : integer {
    let handler : KeyboardEventHandler = (ev: KeyboardEvent) => (ev as any).requestCustomEvent = true;
    return this.customKeyEventHandlers.register(customKey, handler).id;
  }

}
