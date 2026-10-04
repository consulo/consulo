// The in-cell editor of <consulo-data-grid>: one RevoGrid editor (`grid.editors.consulo`) which hosts a native field - the
// grid cell editors decide everything else on the server (DataGridController.startEditing / commitEditing). The field
// only shows the text and reports the gestures: the server parses, refuses, writes and moves to the next cell.
import type { ColumnDataSchemaModel, EditCell, EditorBase, HyperFunc } from '@revolist/revogrid';

type VNode = ReturnType<typeof import('@revolist/revogrid').h>;

/** GridCellEditorPresentation.Kind: TEXT, MULTILINE_TEXT, LIST (NONE never reaches the client). */
export type EditKind = 'text' | 'multiline' | 'list';

/** Why the server refused the commit (UnparsedValue.ParsingError). */
export type EditError = { message: string; offset: number };

/** The answer to consulo-grid-edit-start: the presentation of the editor the server created for the cell. */
export type EditSpec = {
  kind: EditKind;
  text: string;
  typed: string;                 // the key which started the edit, if the field takes it - it goes after `text`
  selectAll: boolean;
  readOnly: boolean;
  readOnlyHint: string;
  align: 'left' | 'center' | 'right';
  placeholder: string;
  options: string[];
  selected: number;
  error: EditError | null;
};

export type CommitReason = 'enter' | 'navigate' | 'blur';

/** One edit the element opened. Every message about it carries `seq`, so an answer for an edit which is over is dropped. */
export type EditSession = {
  readonly seq: number;
  readonly row: number;
  readonly col: number;
  readonly typed: string;        // the key which started the edit in RevoGrid ('' for a double click, Enter, F2, Java)
  spec: EditSpec | null;         // null until the server answers
  editor: ConsuloCellEditor | null;
  done: boolean;                 // over for the element: accepted, cancelled, moved away from, or closed by the server
  commitOnApply: boolean;        // Enter pressed while the field waited for the server
};

/** What the editor needs of <consulo-data-grid>. */
export interface EditHost {
  readonly element: HTMLElement;
  readonly session: EditSession | null;
  commitEdit(session: EditSession, text: string, option: number, reason: CommitReason): void;
  cancelEdit(session: EditSession): void;
  editInput(session: EditSession, text: string): void;
  /** The editor needs another size: RevoGrid sizes it in beforeeditrender, which only runs when its edit is set again. */
  resizeEditor(session: EditSession): void;
  editorClosed(session: EditSession): void;
}

/** A multi-line editor grows over the rows below the cell, up to this many rows; a list too, up to MAX_LIST_ROWS. */
const MAX_MULTILINE_ROWS = 4;
const MAX_LIST_ROWS = 6;

type Field = HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

function isTextField(field: Field | null): field is HTMLInputElement | HTMLTextAreaElement {
  return field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement;
}

/**
 * The RevoGrid editor - RevoGrid creates it in the cell when an edit opens, and disconnects it when the edit closes (Escape,
 * a move of its focus, or our `close`). It waits for the server's EditSpec, keeping the keys typed meanwhile, then builds
 * the field once: `render()` runs again on every overlay render, so it stays a static box.
 */
export class ConsuloCellEditor implements EditorBase {
  element: Element | null = null;  // revogr-edit: its first child, before componentDidRender
  editCell?: EditCell;             // revogr-edit: before every render()

  private readonly session: EditSession | null;
  private box: HTMLElement | null = null;
  private field: Field | null = null;
  private kind: EditKind | null = null;
  private pendingInput = '';       // keys typed before the field existed
  private userTyped = false;       // keys went into the field before it took the focus
  private rows = 1;
  private mounted = false;
  private closing = false;
  private closeRequested = false;

  constructor(private readonly host: EditHost,
              column: ColumnDataSchemaModel,
              _save: (value?: unknown, preventFocus?: boolean) => void,
              private readonly close: (focusNext?: boolean) => void) {
    const s = host.session;
    this.session = s && !s.done && !s.editor && s.row === column.rowIndex && s.col === column.colIndex ? s : null;
    if (this.session) {
      this.session.editor = this;
    }
  }

  /** Runs on every overlay render (a refresh of the rows, a scroll) - static; the field is built once, imperatively. */
  render(createElement: HyperFunc<VNode>): VNode {
    return createElement('div', { class: 'consulo-cell-editor' });
  }

  componentDidRender(): void {
    if (this.mounted || !(this.element instanceof HTMLElement)) {
      return;
    }
    this.mounted = true;
    const s = this.session;
    if (!s || s.done) {
      // no edit of the element (or one the server closed already) - RevoGrid opened this by itself
      this.dismissLater();
      return;
    }
    const box = this.element;
    this.box = box;
    box.addEventListener('keydown', e => this.onKeyDown(e));
    box.addEventListener('focusout', e => this.onFocusOut(e));
    if (s.spec) {
      this.apply(s.spec);
    }
    // else: the box stays empty (and transparent) until beginEdit - keys come through appendPendingInput meanwhile
  }

  /** The server's answer: build the field. componentDidRender calls it when the answer came first. */
  apply(spec: EditSpec): void {
    const box = this.box;
    const s = this.session;
    if (!box || !s || this.field || this.closing) {
      return;
    }
    this.kind = spec.kind;
    const field = createField(spec);
    let caretAtEnd = false;
    const selectAll = spec.selectAll && !spec.error;
    if (isTextField(field)) {
      let text = spec.text;
      if (!spec.readOnly) {
        // the key which started the edit (when the server's field takes it), and those typed until now - the key goes
        // into the emptied field
        const typedLater = this.typedSinceStart() + this.pendingInput;
        if (typedLater && spec.typed === '' && selectAll) {
          // the field opens with its text selected, so the keys typed while this one waited replace the text
          text = '';
        }
        const extra = spec.typed + typedLater;
        text += extra;
        caretAtEnd = extra.length > 0;
      }
      field.value = text;
      field.addEventListener('input', () => this.onInput());
    }
    else {
      field.addEventListener('click', e => this.onListClick(e));
    }
    this.pendingInput = '';
    box.replaceChildren(field);
    this.field = field;
    if (isTextField(field) && selectAll && !caretAtEnd) {
      // selected before the field holds the focus as well: a key RevoGrid forwards meanwhile (appendPendingInput) replaces it
      field.select();
    }
    this.rows = this.preferredRows();
    if (spec.error) {
      this.showError(spec.error);
    }

    // after Stencil's render pass, as RevoGrid's TextEditor focuses
    setTimeout(() => {
      if (this.field !== field || this.closing) {
        return;
      }
      const active = document.activeElement;
      if (active && active !== document.body && !this.host.element.contains(active)) {
        // the focus left the grid while the field waited for the server
        this.focusLeft();
        return;
      }
      field.focus({ preventScroll: true });
      if (isTextField(field)) {
        const end = field.value.length;
        if (caretAtEnd || this.userTyped) {
          field.setSelectionRange(end, end);
        }
        else if (spec.error) {
          // opened again after a refused move: at the error, as an editor which kept its caret
          const offset = Math.max(0, Math.min(spec.error.offset, end));
          field.setSelectionRange(offset, offset);
        }
        else if (spec.selectAll) {
          field.select();
        }
        else {
          field.setSelectionRange(0, 0);
        }
      }
    });

    if (this.rows > 1) {
      this.host.resizeEditor(s);
    }
    if (s.commitOnApply) {
      s.commitOnApply = false;
      this.commit('enter');
    }
  }

  /** How many rows the editor covers - RevoGrid's beforeeditrender reads it. */
  preferredRows(): number {
    const f = this.field;
    if (this.kind === 'list' && f instanceof HTMLSelectElement) {
      return Math.min(Math.max(f.options.length, 2), MAX_LIST_ROWS);
    }
    if (this.kind === 'multiline' && f instanceof HTMLTextAreaElement) {
      return Math.min(f.value.split('\n').length, MAX_MULTILINE_ROWS);
    }
    return 1;
  }

  /**
   * A key RevoGrid took while the field did not have the focus (it was not built, or not focused yet): it goes into the
   * field, or waits for it.
   */
  appendPendingInput(value: string): boolean {
    const f = this.field;
    if (isTextField(f)) {
      if (!f.readOnly) {
        f.setRangeText(value, f.selectionStart ?? f.value.length, f.selectionEnd ?? f.value.length, 'end');
        this.userTyped = true;
        this.onInput();
      }
      f.focus({ preventScroll: true });
    }
    else if (!f && !this.closing) {
      this.pendingInput += value;
    }
    return true;
  }

  /** Enter which did not reach the field, because it did not hold the focus yet. */
  enter(): void {
    this.commit('enter');
  }

  /** RevoGrid's autosave asks for it first - beforeAutoSave below sends the value to the server instead. */
  getValue(): string | undefined {
    return isTextField(this.field) ? this.field.value : undefined;
  }

  /**
   * The focus moved inside the grid (Tab, a click on another cell, our own focus change) and RevoGrid closes the editor,
   * with the autosave of `applyOnClose`. RevoGrid would write the value into its row; the commit goes to the server instead
   * (the grid stops the editor before it changes the selection), which reopens the editor when it refuses.
   */
  beforeAutoSave(): boolean {
    this.closing = true;
    const s = this.session;
    if (s && !s.done) {
      s.done = true;
      if (this.kind === 'list') {
        // a list closed without a choice is cancelled
        this.host.cancelEdit(s);
      }
      else if (this.field || this.pendingText()) {
        this.host.commitEdit(s, this.text(), -1, 'navigate');
      }
      else {
        // left before the server answered, with nothing typed
        this.host.cancelEdit(s);
      }
    }
    return false;
  }

  /** RevoGrid is about to drop the editor (may come several times) - from now on a focus change is no gesture. */
  beforeDisconnect(): void {
    this.closing = true;
    // like RevoGrid's TextEditor: no scroll jump when the focused field goes away
    this.field?.blur();
  }

  disconnectedCallback(): void {
    this.closing = true;
    this.field = null;
    this.box = null;
    const s = this.session;
    if (s) {
      if (s.editor === this) {
        s.editor = null;
      }
      this.host.editorClosed(s);
    }
  }

  /** The server ended the edit (or it never belonged to one): close without RevoGrid's autosave. */
  dismiss(): void {
    this.closing = true;
    if (!this.closeRequested) {
      this.closeRequested = true;
      this.close(false);
    }
  }

  /** The server refused the commit: the field stays, with the error, or clears it when there is none. */
  showError(error: EditError | null): void {
    const f = this.field;
    if (!f) {
      return;
    }
    // FormatBasedGridCellEditor.setError: a red outline and the message as the tooltip
    f.classList.toggle('consulo-cell-editor-error', error !== null);
    f.setAttribute('aria-invalid', String(error !== null));
    const spec = this.session?.spec;
    f.title = error ? error.message : spec?.readOnly ? spec.readOnlyHint : '';
    // the caret stays: the error marks the text from the offset on, which a native field cannot show - the tooltip names it
  }

  private dismissLater(): void {
    this.closing = true;
    setTimeout(() => this.dismiss());
  }

  private text(): string {
    return isTextField(this.field) ? this.field.value : this.pendingText();
  }

  /** The text of a field which is not built yet: what it will show. */
  private pendingText(): string {
    const s = this.session;
    return s ? s.typed + this.typedSinceStart() + this.pendingInput : '';
  }

  /**
   * Keys RevoGrid put into the value of its edit between the start and the creation of this editor, after the key which
   * started it.
   */
  private typedSinceStart(): string {
    const s = this.session;
    const cell = this.editCell;
    const val = typeof cell?.val === 'string' ? cell.val : '';
    if (!s || !val.startsWith(s.typed)) {
      return '';
    }
    // RevoGrid hands the text of the cell as the value of an edit nothing was typed into
    if (s.typed === '' && val === String(cell?.value ?? '')) {
      return '';
    }
    return val.substring(s.typed.length);
  }

  private commit(reason: 'enter' | 'blur'): void {
    const s = this.session;
    if (!s || s.done || this.closing) {
      return;
    }
    const f = this.field;
    if (!f) {
      s.commitOnApply = reason === 'enter';
      return;
    }
    const option = f instanceof HTMLSelectElement ? f.selectedIndex : -1;
    this.host.commitEdit(s, this.text(), option, reason);
  }

  private onInput(): void {
    const s = this.session;
    if (!s || s.done || !isTextField(this.field)) {
      return;
    }
    // a change of the text drops the error
    this.showError(null);
    this.host.editInput(s, this.field.value);
    const rows = this.preferredRows();
    if (rows !== this.rows) {
      this.rows = rows;
      this.host.resizeEditor(s);
    }
  }

  private onKeyDown(e: KeyboardEvent): void {
    if (e.isComposing || this.closing || e.key !== 'Enter') {
      // Escape is the host's (it comes before the field exists too), Tab is RevoGrid's: it moves, and the autosave commits
      return;
    }
    const f = this.field;
    if (this.kind === 'multiline' && (e.ctrlKey || e.metaKey || e.shiftKey)) {
      // GridCellEditorTextField: control ENTER in a multi-line editor is the editor's Enter - a browser inserts nothing for it
      // on its own; shift ENTER too (the editor's start-new-line)
      e.preventDefault();
      if (f instanceof HTMLTextAreaElement && !f.readOnly) {
        f.setRangeText('\n', f.selectionStart, f.selectionEnd, 'end');
        this.onInput();
      }
      return;
    }
    if (e.shiftKey || e.altKey) {
      return;
    }
    // ENTER, and control ENTER in a single-line editor: grid.stopEditing() - the overlay skips a prevented key
    e.preventDefault();
    this.commit('enter');
  }

  /** A press on an option chooses it. */
  private onListClick(e: MouseEvent): void {
    const f = this.field;
    if (!(f instanceof HTMLSelectElement) || f.selectedIndex < 0) {
      return;
    }
    const target = e.target;
    // not on the scroll bar of the list
    if (target instanceof HTMLOptionElement || (target === f && e.offsetX < f.clientWidth)) {
      this.commit('enter');
    }
  }

  private onFocusOut(e: FocusEvent): void {
    if (this.closing) {
      return;
    }
    const to = e.relatedTarget instanceof Node ? e.relatedTarget : null;
    if (to && this.host.element.contains(to)) {
      // inside the grid: RevoGrid closes the editor when its focus moves, with the autosave
      return;
    }
    if (!to) {
      // nothing took the focus: another window did (which leaves the edit open), or a press on nothing focusable - known
      // once the focus settled
      setTimeout(() => {
        const active = document.activeElement;
        if (!this.closing && document.hasFocus() && !(active && this.host.element.contains(active))) {
          this.focusLeft();
        }
      });
      return;
    }
    this.focusLeft();
  }

  /** The focus went to another component: the edit is committed, or else discarded. */
  private focusLeft(): void {
    const s = this.session;
    if (!s || s.done) {
      return;
    }
    if (this.kind === 'list') {
      s.done = true;
      this.host.cancelEdit(s);
      this.dismiss();
      return;
    }
    // the server answers endEdit (written, or dropped when refused) - or keeps the field while it asks the user
    this.commit('blur');
  }
}

function createField(spec: EditSpec): Field {
  switch (spec.kind) {
    case 'list': {
      // the list editor of a boolean (DefaultBooleanEditorFactory): a list under the cell, with the first choice highlighted
      const select = document.createElement('select');
      select.size = Math.max(spec.options.length, 2);
      spec.options.forEach((option, index) => select.add(new Option(option, String(index), false, index === spec.selected)));
      select.disabled = spec.readOnly;
      select.title = spec.readOnlyHint;
      return select;
    }
    case 'multiline':
    case 'text': {
      const field = spec.kind === 'multiline' ? document.createElement('textarea') : document.createElement('input');
      if (field instanceof HTMLInputElement) {
        field.type = 'text';
        if (spec.align === 'right') {
          field.inputMode = 'decimal'; // numbers: not type=number, which refuses <null>, 1E3 and big decimals
        }
      }
      else {
        field.rows = 1;
      }
      field.spellcheck = false;
      field.autocomplete = 'off';
      field.readOnly = spec.readOnly;
      field.placeholder = spec.placeholder;
      field.title = spec.readOnly ? spec.readOnlyHint : '';
      field.style.textAlign = spec.align;
      return field;
    }
  }
}
