import { css } from 'lit';
import {customElement} from 'lit/decorators/custom-element.js';
import { XTermElement } from './xterm-element';
import { XTermFitMixin } from './xterm-fit-mixin';

/**
 * A terminal attached to a pty, as opposed to the line oriented console the addon builds by default.
 *
 * A pty echoes what it is sent, so the element must not, and every keystroke has to reach the process
 * rather than being collected into a line first.
 */
@customElement('consulo-pty-term')
export class ConsuloPtyTerm extends XTermFitMixin(XTermElement) {

  /**
   * A custom element is inline by default, and a resize observer is required to ignore inline elements - the
   * terminal would never hear that its tool window changed size. Being a block also lets the full height it is
   * given resolve against the container.
   */
  static styles = css`
    :host {
      display: block;
      height: 100%;
    }

    /* xterm opens with a palette of its own, and showing it would flash black before the theme of the ide
       arrives from the server */
    :host([unthemed]) {
      visibility: hidden;
    }
  `;

  private _resizeObserver?: ResizeObserver;

  /** the process writes from the moment it starts, the terminal can only be written to once it is opened */
  private _opened: boolean = false;
  private _pending: string = '';

  connectedCallback() {
    super.connectedCallback();

    // keeps the keymap of the ide off the keys the process needs
    this.setAttribute('consulo-keyboard-capture', '');
    this.setAttribute('unthemed', '');

    this.addEventListener('terminal-initialized', () => this._flush());

    // the fit addon of the base only watches the window, and a tool window changes size without the
    // window ever doing so
    this._resizeObserver = new ResizeObserver(() => this.fit());
    this._resizeObserver.observe(this);

    const term = this.terminal;

    term.onData((data: string) => {
      this.dispatchEvent(new CustomEvent('pty-data', {detail: data}));
    });

    term.onBinary((data: string) => {
      this.dispatchEvent(new CustomEvent('pty-data', {detail: data}));
    });

    term.onResize((size: {cols: number, rows: number}) => {
      this.dispatchEvent(new CustomEvent('pty-resize', {detail: {cols: size.cols, rows: size.rows}}));
    });
  }

  disconnectedCallback() {
    this._resizeObserver?.disconnect();
    this._resizeObserver = undefined;
    this._opened = false;
    super.disconnectedCallback();
  }

  applyTheme(theme: any): void {
    this.terminal.options.theme = theme;
    this.removeAttribute('unthemed');
  }

  writeText(data: string): void {
    if (!this._opened) {
      this._pending += data;
      return;
    }
    this.terminal.write(data);
  }

  private _flush(): void {
    this._opened = true;
    if (this._pending) {
      const pending = this._pending;
      this._pending = '';
      this.terminal.write(pending);
    }
  }

  /**
   * The base element writes what the user typed straight back into the screen. The process on the other end
   * is what decides how the input looks, so the keystroke leaves here and comes back as output.
   */
  _onData(e: string): void {
  }
}
