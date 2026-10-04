// <consulo-data-grid>: a thin light-DOM wrapper over <revo-grid>. DataGridController (Java) is the single source of
// truth: this element only displays what the server sends and reports user gestures back as DOM events. Cell editing too:
// the element hosts the field of an edit in the cell (consulo-cell-editor.ts), the server decides and writes everything.
import { defineCustomElement } from '@revolist/revogrid/standalone/revo-grid.js';
import type {
  BeforeSaveDataDetails, CellProps, ColumnRegular, EditorCtrCallable, PluginProviders, RowHeaders
} from '@revolist/revogrid';
import { ConsuloCellEditor } from './consulo-cell-editor';
import type { CommitReason, EditError, EditHost, EditSession, EditSpec } from './consulo-cell-editor';

defineCustomElement();

// the text styles of a cell (`f` of a row); its colours are css colours (`b`, `c`, `m` of a row)
const FLAG_NULL = 1;
const FLAG_GRAYED = 2;
const FLAG_ERROR = 4;

/** The padding of a cell, in ch of the grid font: the widths from the server are without it. */
const CELL_PADDING_CH = 2;

/** The sort marker of a sortable column which is not sorted - shown while the pointer is over the header. */
const UNSORTED_MARKER = '↕';

/** A double click reaches RevoGrid's beforeeditstart right after the dblclick event. */
const DBLCLICK_EDIT_MS = 500;

/**
 * The menu key and shift F10 open the context menu on keydown here; the contextmenu event a browser may raise for the same
 * key afterwards (on keyup, on some platforms) is dropped within this long.
 */
const MENU_KEY_SUPPRESS_MS = 1000;

const IS_MAC = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);

/** A chunk asked for and still not answered after this long is asked for again - a request can be dropped. */
const REQUEST_RETRY_MS = 2000;

/** A copy which needs rows from the server gives up after this long. */
const COPY_TIMEOUT_MS = 10000;

/**
 * A press on a column header moves the column only once the pointer went further than this, in px - the distance after which
 * RevoGrid stops taking the press for a click.
 */
const COLUMN_DRAG_THRESHOLD_PX = 10;

// the keys the grid handles itself - RevoGrid moves the selection with the arrows (with shift, ctrl or meta) and tab,
// selects all with ctrl a, ctrl c / ctrl insert raise the copy event answered here, and the menu key / shift f10 open the
// context menu of the lead cell. shortcuts.js leaves the keys named in consulo-own-keys to the element instead of the
// keymap; the modifiers are in its order: ALT, CTRL, META, SHIFT
const ARROW_KEYS = ['37', '38', '39', '40'];
const OWN_KEYS = ARROW_KEYS
  .flatMap(key => [key, 'SHIFT+' + key, 'CTRL+' + key, 'CTRL+SHIFT+' + key, 'META+' + key, 'META+SHIFT+' + key])
  .concat(['9', 'SHIFT+9', 'CTRL+65', 'META+65', 'CTRL+67', 'META+67', 'CTRL+45', '93', 'SHIFT+121']);
// while cells can be edited: enter and f2 open the editor of the lead cell (DataGrid.editSelectedCell())
const EDITABLE_KEYS = ['13', '113'];
// while an editor is open the keys of a text field are its own - also in a list (a select is no text field for
// shortcuts.js): escape cancels, enter commits, ctrl enter commits or breaks the line, tab moves, and the clipboard and
// undo strokes work in the field
const EDITING_KEYS = ['27', '13', 'SHIFT+13', 'CTRL+13', 'META+13', '8', '46', 'CTRL+8', 'CTRL+46', '32',
  '33', '34', '35', '36', 'CTRL+35', 'CTRL+36', 'SHIFT+35', 'SHIFT+36',
  'CTRL+86', 'META+86', 'SHIFT+45', 'CTRL+88', 'META+88', 'SHIFT+46',
  'CTRL+90', 'META+90', 'CTRL+SHIFT+90', 'META+SHIFT+90', 'CTRL+89'];

type Config = {
  rowNumbers: boolean;
  horizontalLines: boolean;
  striped: boolean;
  hoverRows: boolean;
  transparentColumnHeader: boolean;
  transparentRowHeader: boolean;
  chunkSize: number;
  editable: boolean;
  rowLines: number;          // the text lines of every row
  moveColumns: boolean;      // a column may be dragged to another place
};
type ColumnInfo = {
  title: string;
  sort: string;              // the sort marker of the header, '' when the column is not sorted
  tooltip: string;
  align: 'left' | 'center' | 'right';
  sortable: boolean;
  widthCh: number;           // in ch of the grid font, without the padding of the cell
};
type Columns = { gen: number; items: ColumnInfo[] };
type RowModel = { gen: number; count: number; rowNumberWidthCh: number };
type Selection = { seq: number; rows: [number, number][]; cols: [number, number][]; lead?: [number, number] };
/** A row as the server sends it: the colours are 1-based indices into the css colours of the response, 0 for none. */
type RowPayload = { n: string; t: string[]; f?: number[]; b?: number[]; c?: number[]; m?: number };
type RowsPayload = { v: string[]; r: RowPayload[] };
/**
 * A row as RevoGrid holds it: the colours are css colour values - `var(--consulo-...)` for a colour of the style, which follows
 * the style, a literal colour for any other one - null for none.
 */
type Row = {
  _n: string;
  _f?: number[];
  _b?: (string | null)[];
  _c?: (string | null)[];
  _m?: string | null;
  _mk?: boolean;             // a row of a selection a rectangle cannot show
  _cls: string;
  _stale: boolean;
  [prop: string]: unknown;
};
type Area = { x: number; y: number; x1: number; y1: number };
type Cell = { x: number; y: number };
/** The editor font of the colour scheme: a family name, and a size in px (0 or less: the default size). */
type Font = { family: string; size: number };
/** What a context menu gesture hit: DataGrid's GridHitArea, and view indices or -1. */
type Hit = { area: 'CELL' | 'COLUMN_HEADER' | 'ROW_HEADER' | 'EMPTY'; row: number; col: number };

const DEFAULT_CONFIG: Config = {
  rowNumbers: true, horizontalLines: true, striped: false, hoverRows: true,
  transparentColumnHeader: false, transparentRowHeader: false, chunkSize: 200, editable: false,
  rowLines: 1, moveColumns: false
};

const NO_HIT: Hit = { area: 'EMPTY', row: -1, col: -1 };

function parse<T>(value: unknown): T {
  return (typeof value === 'string' ? JSON.parse(value) : value) as T;
}

/** Inclusive runs of sorted, distinct indices: [1, 2, 3, 7] -> [[1, 3], [7, 7]]. */
function toRuns(indices: number[]): [number, number][] {
  const runs: [number, number][] = [];
  for (const index of indices) {
    const last = runs[runs.length - 1];
    if (last && last[1] + 1 === index) {
      last[1] = index;
    }
    else {
      runs.push([index, index]);
    }
  }
  return runs;
}

/** The indices of inclusive runs: [[1, 3], [7, 7]] -> [1, 2, 3, 7]. */
function fromRuns(runs: [number, number][]): number[] {
  const indices: number[] = [];
  for (const [start, end] of runs) {
    for (let index = start; index <= end; index++) {
      indices.push(index);
    }
  }
  return indices;
}

function range(from: number, to: number): number[] {
  const indices: number[] = [];
  for (let index = from; index <= to; index++) {
    indices.push(index);
  }
  return indices;
}

/** The css colour a colour index of a row names, or null. */
function colorOf(colors: string[], index: number | undefined): string | null {
  return index !== undefined && index > 0 ? colors[index - 1] ?? null : null;
}

/** An index attribute of a RevoGrid cell (data-rgrow, data-rgcol), or -1. */
function indexAttribute(element: Element, name: string): number {
  const value = element.getAttribute(name);
  const index = value === null ? NaN : Number(value);
  return Number.isInteger(index) ? index : -1;
}

function isCorner(area: Area, cell: Cell): boolean {
  return (cell.x === area.x || cell.x === area.x1) && (cell.y === area.y || cell.y === area.y1);
}

function hasModifier(e: KeyboardEvent): boolean {
  return e.ctrlKey || e.metaKey || e.altKey || e.shiftKey;
}

/** The scroll coordinate which brings an item fully into view by the shortest move, or null when it is in view. */
function nearestCoordinate(providers: PluginProviders, type: 'rgRow' | 'rgCol', index: number): number | null {
  const start = providers.dimension.getViewPortPos({ coordinate: index, dimension: type });
  const state = providers.dimension.stores[type].getCurrentState();
  const size = state.sizes[index] ?? state.originItemSize;
  const viewport = providers.viewport.stores[type];
  const from = viewport.lastCoordinate;
  const extent = viewport.store.get('clientSize');
  if (!extent || start < from) {
    return start;
  }
  if (start + size > from + extent) {
    // an item larger than the viewport keeps its start in view
    return Math.min(start, start + size - extent);
  }
  return null;
}

class ConsuloDataGrid extends HTMLElement implements EditHost {
  private grid: HTMLRevoGridElement | null = null;
  private cfg: Config = DEFAULT_CONFIG;
  private cols: Columns = { gen: -1, items: [] };
  private model: RowModel = { gen: -1, count: 0, rowNumberWidthCh: 5 };
  private sel: Selection | null = null;

  private data: Row[] = [];
  private dataColumnsGen = -1;                      // the column generation the texts of `data` are laid out for
  private requested = new Map<number, number>();    // chunk index -> time it was asked for, in the current row generation
  private retryTimer = 0;
  private marked = new Set<number>();               // rows of a selection a rectangle cannot show
  private markedCols: Set<number> | null = null;    // its columns, or null for all of them (the rows are tinted whole)
  private area: Area | null = null;                 // the selected rectangle, as last reported or applied
  // the width a column was dragged to, by its index: the server keeps it in ch, and while it sends that width back for the
  // column the dragged px stay, so the column does not move by the rounding
  private resized = new Map<number, { title: string; ch: number; px: number }>();
  private chPx = 8;
  private rowBase = 32;
  private linePx = 16;                              // the pitch of the text lines of a row which shows several
  private metricsPending = true;                    // not measured yet, or measured while hidden
  private rendered = false;                         // RevoGrid has rendered the columns and source it was last given
  private selectionPending = false;
  private pendingScroll: [number, number] | null = null;
  private programmatic = 0;
  private suppressScrollUntil = 0;
  private anchorRow = -1;
  private lastReported = '';
  private lastViewport = '';
  private refreshFrame = 0;
  private windowFrame = 0;
  private adjustingFrame = 0;
  private observer: ResizeObserver | null = null;
  private scrollTopPx = 0; // last viewportscroll coordinate of the rows; RevoGrid keeps it over a source change
  private copySeq = 0;
  private copyRequests = new Map<number, (tsv: string) => void>();
  private fontSpec: Font | null = null;
  private suppressContextMenuUntil = 0;
  private columnDragX: { min: number; max: number } | null = null; // how far the pointer went during the column drag

  // cell editing
  session: EditSession | null = null;               // the open edit, for the editor RevoGrid creates in its cell
  private editSeq = 0;
  private focusCell: Cell | null = null;            // RevoGrid's focused cell: where its editor opens, the lead of the selection
  private lastDblClick = -1;
  private seenKeys = new WeakSet<KeyboardEvent>();  // beforekeydown comes once per overlay of the grid
  private work: Promise<void> = Promise.resolve();  // applying a selection and opening an editor, one after the other
  private pendingOpen: { row: number; col: number; typed: string } | null = null;

  private readonly onDocumentCopy = (e: ClipboardEvent) => this.copy(e);
  private readonly onFontsLoaded = () => this.remeasure();

  // region properties set by Flow (JSON strings or objects); Flow re-sends them when the element is re-attached.
  // Within one response Flow applies them in the order of its change tracker (a HashMap) - nothing here may depend on
  // that order

  set config(value: unknown) { this.cfg = { ...DEFAULT_CONFIG, ...parse<Partial<Config>>(value) }; this.applyConfig(); }
  get config(): unknown { return this.cfg; }

  set columns(value: unknown) { this.cols = parse<Columns>(value); this.applyColumns(); }
  get columns(): unknown { return this.cols; }

  set rowModel(value: unknown) { this.model = parse<RowModel>(value); this.applyRowModel(); }
  get rowModel(): unknown { return this.model; }

  set selection(value: unknown) { this.sel = parse<Selection>(value); this.queueSelection(); }
  get selection(): unknown { return this.sel; }

  set font(value: unknown) { this.fontSpec = parse<Font | null>(value) ?? null; this.applyFont(); }
  get font(): unknown { return this.fontSpec; }

  // endregion

  private get rowLines(): number {
    return Math.max(1, Math.floor(this.cfg.rowLines) || 1);
  }

  /**
   * The pitch of every row: a row of one line is as tall as a row of the toolkit (or the line of the font, when that is
   * taller), and every further line adds the pitch of a text line. The same for every row, so a row index is a coordinate
   * divided by it (the chunk and viewport math).
   */
  private get rowSize(): number {
    return this.rowBase + (this.rowLines - 1) * this.linePx + (this.cfg.horizontalLines ? 1 : 0);
  }

  connectedCallback() {
    // a property Flow set before the element was upgraded is an own property shadowing the accessor
    for (const name of ['config', 'columns', 'rowModel', 'selection', 'font'] as const) {
      if (Object.prototype.hasOwnProperty.call(this, name)) {
        const value = (this as any)[name];
        delete (this as any)[name];
        (this as any)[name] = value;
      }
    }
    if (!this.hasAttribute('tabindex')) {
      this.tabIndex = 0;
    }
    this.updateOwnKeys();
    if (!this.grid) {
      this.grid = this.createGrid();
    }
    if (this.grid.parentNode !== this) {
      this.appendChild(this.grid);
    }
    // the size goes from nothing to real when a hidden ancestor is shown - the metrics are only right from then on
    this.observer = new ResizeObserver(() => {
      if (this.metricsPending) {
        this.remeasure();
      }
      this.scheduleWindow();
    });
    this.observer.observe(this);
    document.addEventListener('copy', this.onDocumentCopy, true);
    document.fonts?.addEventListener('loadingdone', this.onFontsLoaded);

    this.setFontVariables();
    this.measure();
    void document.fonts?.ready.then(() => { if (this.isConnected) { this.remeasure(); } });
    this.loadFont();
    this.applyConfig();
    this.applyColumns();
    this.applyRowModel();
    this.queueSelection();
  }

  disconnectedCallback() {
    this.observer?.disconnect();
    this.observer = null;
    document.removeEventListener('copy', this.onDocumentCopy, true);
    document.fonts?.removeEventListener('loadingdone', this.onFontsLoaded);
    cancelAnimationFrame(this.adjustingFrame);
    this.adjustingFrame = 0;
    clearTimeout(this.retryTimer);
    this.retryTimer = 0;
  }

  // region calls from Java (Element.callJsFunction)

  putRows(gen: number, from: number, json: string) {
    if (gen !== this.model.gen) {
      return;
    }
    const payload = JSON.parse(json) as RowsPayload;
    const rows = payload.r ?? [];
    const colors = payload.v ?? [];
    for (let i = 0; i < rows.length; i++) {
      const target = this.data[from + i];
      if (target) {
        this.fill(target, rows[i], colors);
      }
    }
    this.forgetChunks(from, from + rows.length - 1);
    this.scheduleRefresh();
  }

  invalidateRows(gen: number, first: number, last: number) {
    if (gen !== this.model.gen) {
      return;
    }
    for (let row = Math.max(0, first); row <= Math.min(last, this.data.length - 1); row++) {
      this.data[row]._stale = true;
    }
    this.forgetChunks(first, last);
    this.scheduleWindow();
  }

  /** Scrolls a cell into view the shortest way: not at all when it is in view, else just far enough to show it whole. */
  scrollToCell(row: number, column: number) {
    if (!this.grid) {
      return;
    }
    if (!this.rendered) {
      this.pendingScroll = [row, column];
      return;
    }
    void this.scrollNearest(row, column);
  }

  focusGrid() {
    this.focus({ preventScroll: true });
  }

  /** The text of a copy the server was asked for by consulo-grid-copy. */
  resolveCopy(id: number, tsv: string) {
    const resolve = this.copyRequests.get(id);
    if (resolve) {
      this.copyRequests.delete(id);
      resolve(tsv);
    }
  }

  /** The editor the server created for edit `seq` - its field is built now, or as soon as RevoGrid created the editor. */
  beginEdit(seq: number, json: string) {
    const s = this.session;
    if (!s || s.seq !== seq || s.done) {
      return;
    }
    s.spec = JSON.parse(json) as EditSpec;
    s.editor?.apply(s.spec);
  }

  /** The server ended edit `seq`: its commit was accepted, it was cancelled, or the cell is not edited. Nothing is written. */
  endEdit(seq: number) {
    const s = this.session;
    if (!s || s.seq !== seq) {
      return;
    }
    s.done = true;
    if (s.editor) {
      // closes without the autosave; editorClosed() follows
      s.editor.dismiss();
      return;
    }
    // RevoGrid has not created the editor yet - its edit goes, so it does not
    this.finishSession(s);
    void this.grid?.getProviders().then(providers => {
      const edit = providers?.selection.edit;
      if (!this.session && edit && edit.x === s.col && edit.y === s.row) {
        providers.selection.setEdit(false);
      }
    });
  }

  /** The server refused the commit of edit `seq`; the field stays, showing why (an empty json: nothing to show). */
  editError(seq: number, json: string) {
    const s = this.session;
    if (!s || s.seq !== seq || s.done) {
      return;
    }
    const error = json ? JSON.parse(json) as EditError : null;
    if (s.spec) {
      s.spec.error = error;
    }
    s.editor?.showError(error);
  }

  /**
   * Opens the editor of a cell - DataGrid.editSelectedCell(), and the editor of an edit whose move to another cell was
   * refused. After the selection the same response may carry, which focuses the cell first.
   */
  requestEdit(row: number, column: number, typed: string) {
    this.queue(() => this.openEditor(row, column, typed));
  }

  // endregion

  // region EditHost: calls of the cell editor

  get element(): HTMLElement {
    return this;
  }

  commitEdit(session: EditSession, text: string, option: number, reason: CommitReason) {
    this.emit('consulo-grid-edit-commit', { seq: session.seq, text, option, reason });
  }

  cancelEdit(session: EditSession) {
    this.emit('consulo-grid-edit-cancel', { seq: session.seq });
  }

  editInput(session: EditSession, text: string) {
    this.emit('consulo-grid-edit-input', { seq: session.seq, text });
  }

  resizeEditor(session: EditSession) {
    void this.grid?.getProviders().then(providers => {
      const edit = providers?.selection.edit;
      if (this.session === session && !session.done && edit && edit.x === session.col && edit.y === session.row) {
        // a new edit object renders the overlay again, which asks beforeeditrender for the size of the editor
        providers.selection.setEdit(typeof edit.val === 'string' ? edit.val : '');
      }
    });
  }

  editorClosed(session: EditSession) {
    // no cancel here: RevoGrid drops the editor after every commit too - only escape cancels
    this.finishSession(session);
  }

  // endregion

  private createGrid(): HTMLRevoGridElement {
    const grid = document.createElement('revo-grid');
    grid.classList.add('consulo-revo-grid');
    grid.theme = 'consulo';
    grid.readonly = !this.cfg.editable;
    grid.range = true;
    grid.filter = false;
    // RevoGrid sets its column dragging up once, with the grid - whether a drag starts is decided per drag (columndragstart)
    grid.canMoveColumns = true;
    grid.canDrag = false;
    grid.exporting = false;
    grid.resize = true;
    // RevoGrid would copy the texts this element holds, which are blank for rows never scrolled to and stale after a
    // sort - the copy event is answered here instead
    grid.useClipboard = false;
    grid.rowClass = '_cls';
    grid.hideAttribution = true; // MIT license: the corner attribution link is optional
    // a focus move closes the editor with an autosave, which the editor turns into a commit for the server
    grid.applyOnClose = true;
    const editor: EditorCtrCallable = (column, save, close) => new ConsuloCellEditor(this, column, save, close);
    grid.editors = { consulo: editor };

    // header press -> the controller selects the column; a press on the sort marker -> controller sorting (columns are never
    // RevoGrid-sortable). A press which ended a column drag does not come here (RevoGrid drops it)
    grid.addEventListener('headerclick', (e: Event) => {
      const detail = (e as CustomEvent).detail as { prop?: unknown; index?: number; originalEvent?: MouseEvent };
      const column = detail.index ?? -1;
      // the row-number corner is a revogr-header too and reports index 0 with prop '_n'
      if (column < 0 || column >= this.cols.items.length || detail.prop !== 'c' + column) {
        return;
      }
      const oe = detail.originalEvent;
      const target = oe?.target instanceof Element ? oe.target : null;
      if (this.cols.items[column].sortable && target?.closest('.consulo-sort-marker')) {
        this.emit('consulo-grid-header-sort', { column, additive: !!(oe && (oe.altKey || oe.shiftKey)) });
      }
      else {
        this.emit('consulo-grid-header-click', { column, extend: !!oe?.shiftKey, toggle: !!(oe && (oe.ctrlKey || oe.metaKey)) });
      }
    });

    // column drag: only while the data source can move its columns, and never out of an open edit
    grid.addEventListener('columndragstart', (e: Event) => {
      const detail = (e as CustomEvent).detail as { prop?: unknown } | null;
      const prop = detail?.prop;
      this.columnDragX = null;
      if (!this.cfg.moveColumns || (this.session && !this.session.done) || typeof prop !== 'string' || !prop.startsWith('c')) {
        e.preventDefault();
      }
    });
    grid.addEventListener('columndragmousemove', (e: Event) => {
      const me = (e as CustomEvent).detail as MouseEvent | null;
      if (me && typeof me.x === 'number') {
        const seen = this.columnDragX;
        this.columnDragX = seen ? { min: Math.min(seen.min, me.x), max: Math.max(seen.max, me.x) } : { min: me.x, max: me.x };
      }
    });
    // the drop: RevoGrid does not reorder its columns - the server moves the column in the data source, and the columns come
    // back in the new order (or stay, when the move is refused)
    grid.addEventListener('beforecolumndragend', (e: Event) => {
      e.preventDefault();
      const detail = (e as CustomEvent).detail as {
        startPos?: number; startPosition?: { itemIndex: number }; newPosition?: { itemIndex: number }
      };
      // RevoGrid reports a drop for every release after a press on a header: a press released across the border of the next
      // header without a real drag is a click, not a move
      const dragX = this.columnDragX;
      this.columnDragX = null;
      const startX = detail.startPos;
      const dragged = dragX !== null && typeof startX === 'number'
        && Math.max(Math.abs(dragX.min - startX), Math.abs(dragX.max - startX)) > COLUMN_DRAG_THRESHOLD_PX;
      const count = this.cols.items.length;
      const from = detail.startPosition?.itemIndex ?? -1;
      const to = Math.min(detail.newPosition?.itemIndex ?? -1, count - 1);
      if (dragged && from >= 0 && from < count && to >= 0 && from !== to) {
        this.emit('consulo-grid-column-move', { from, to });
      }
    });

    // selection -> controller (rectangles only)
    grid.addEventListener('setrange', (e: Event) => {
      const area = (e as CustomEvent).detail as (Area & { type: string }) | null;
      this.dropUnmountedEdit();
      if (area && area.type === 'rgRow') {
        this.report(area, false);
      }
    });
    grid.addEventListener('focuscell', (e: Event) => {
      const detail = (e as CustomEvent).detail as { range: Area; rowType: string; colType: string };
      this.dropUnmountedEdit();
      if (detail.rowType === 'rgRow' && detail.colType === 'rgCol') {
        this.focusCell = { x: detail.range.x, y: detail.range.y };
        this.report(detail.range, false);
      }
    });
    grid.addEventListener('settemprange', (e: Event) => {
      const detail = (e as CustomEvent).detail as { area: Area; type: string } | null;
      // a null area (the pointer back on the start cell) drops the pending report as well
      cancelAnimationFrame(this.adjustingFrame);
      this.adjustingFrame = 0;
      if (detail?.area) {
        const area = detail.area;
        this.adjustingFrame = requestAnimationFrame(() => {
          this.adjustingFrame = 0;
          this.report(area, true);
        });
      }
    });
    grid.addEventListener('selectall', () => {
      const lastRow = this.model.count - 1;
      const lastCol = this.cols.items.length - 1;
      if (lastRow >= 0 && lastCol >= 0) {
        this.report({ x: 0, y: 0, x1: lastCol, y1: lastRow }, false);
      }
    });

    // a press outside the grid (toolbar, editor) keeps the selection; only our own clears go through
    grid.addEventListener('beforefocuslost', (e: Event) => { if (this.programmatic === 0) { e.preventDefault(); } });

    // RevoGrid listens to keydown on document: act only while this grid holds DOM focus
    grid.addEventListener('beforekeydown', (e: Event) => {
      if (!this.contains(document.activeElement)) {
        e.preventDefault();
        return;
      }
      const original = (e as CustomEvent<{ original?: KeyboardEvent }>).detail?.original;
      if (original) {
        this.onGridKey(e, original);
      }
    });

    // cell editing - the server is the only writer: RevoGrid's own writes are all refused, the editor reports to the server
    grid.addEventListener('beforeeditstart', (e: Event) => this.onBeforeEditStart(e as CustomEvent<BeforeSaveDataDetails>));
    grid.addEventListener('beforeedit', (e: Event) => e.preventDefault());
    grid.addEventListener('beforerangeedit', (e: Event) => e.preventDefault());
    grid.addEventListener('beforeautofill', (e: Event) => e.preventDefault());
    grid.addEventListener('beforeeditrender', (e: Event) => {
      // a multi-line field and a list cover the rows below the cell - as many as their lines need, rows of several lines
      // holding several of them
      const range = (e as CustomEvent<{ range: Area }>).detail.range;
      const s = this.session;
      const lines = s && !s.done && s.editor && range.x === s.col && range.y === s.row ? s.editor.preferredRows() : 1;
      const rows = Math.ceil(lines / this.rowLines);
      if (rows > 1) {
        range.y1 = Math.max(range.y, Math.min(range.y + rows - 1, this.model.count - 1));
      }
    });

    // a focus moved by us must not scroll the grid (or its ancestors)
    grid.addEventListener('beforescrollintoview', (e: Event) => {
      if (performance.now() < this.suppressScrollUntil) {
        e.preventDefault();
      }
    });

    // a width the user dragged goes to the server, in ch of the grid font without the padding of the cell - it keeps the
    // width for the column, over changes of the columns too, and sends it back with the columns
    grid.addEventListener('aftercolumnresize', (e: Event) => {
      const detail = (e as CustomEvent).detail as Record<string, { size?: number }>;
      for (const [key, column] of Object.entries(detail)) {
        const index = Number(key);
        const info = this.cols.items[index];
        if (typeof column.size === 'number' && info) {
          const chars = Math.max(1, Math.round(column.size / this.chPx) - CELL_PADDING_CH);
          info.widthCh = chars;
          this.resized.set(index, { title: info.title, ch: chars, px: column.size });
          this.emit('consulo-grid-column-resize', { column: index, chars });
        }
      }
    });

    grid.addEventListener('viewportscroll', (e: Event) => {
      const detail = (e as CustomEvent).detail as { dimension: string; coordinate: number };
      if (detail.dimension === 'rgRow') {
        this.scrollTopPx = detail.coordinate;
        this.scheduleWindow();
      }
    });

    // RevoGrid sizes its selection to the rows and columns of its last render: a selection or a scroll set before the
    // render of new columns or rows is clamped to the old ones (or lost, before the first render), so they wait for it
    grid.addEventListener('aftergridrender', () => {
      this.rendered = true;
      const scroll = this.pendingScroll;
      this.pendingScroll = null;
      if (scroll) {
        this.scrollToCell(scroll[0], scroll[1]);
      }
      if (this.selectionPending) {
        this.selectionPending = false;
        this.queueSelection();
      }
      const open = this.pendingOpen;
      if (open) {
        this.pendingOpen = null;
        this.queue(() => this.openEditor(open.row, open.col, open.typed));
      }
    });

    // runs before RevoGrid's own mousedown handling (it listens on its descendants)
    this.addEventListener('mousedown', (e: MouseEvent) => this.onMouseDownCapture(e), true);
    // RevoGrid's beforeeditstart does not tell a double click from enter
    this.addEventListener('dblclick', () => { this.lastDblClick = performance.now(); }, true);
    // the context menu: hit-tested here, the server selects what it was opened on and shows the menu
    this.addEventListener('contextmenu', (e: MouseEvent) => this.onContextMenu(e));
    // the menu key and shift f10 - before RevoGrid, which listens to keydown on document
    this.addEventListener('keydown', (e: KeyboardEvent) => this.onMenuKey(e));
    return grid;
  }

  // region context menu

  /** A context menu gesture of the pointer: what it hit goes to the server, at the position relative to the grid. */
  private onContextMenu(e: MouseEvent) {
    const target = e.target instanceof Element ? e.target : null;
    if (target?.closest('revogr-edit')) {
      // the field of an open edit has the menu of the browser (cut, copy, paste)
      e.stopPropagation();
      return;
    }
    e.preventDefault();
    e.stopPropagation();
    if (performance.now() < this.suppressContextMenuUntil) {
      // raised for the menu key handled on keydown already
      return;
    }
    if (e.button !== 2 && e.clientX === 0 && e.clientY === 0 && e.screenX === 0 && e.screenY === 0) {
      // a menu key the browser raised the event for by itself
      this.keyboardContextMenu('ContextMenu');
      return;
    }
    const hit = this.hitTest(target);
    const base = this.menuBase().getBoundingClientRect();
    this.emit('consulo-grid-contextmenu', {
      ...hit,
      x: Math.round(e.clientX - base.left), y: Math.round(e.clientY - base.top), screenX: e.screenX, screenY: e.screenY,
      keyboard: false, key: '', alt: e.altKey, ctrl: e.ctrlKey, shift: e.shiftKey, meta: e.metaKey
    });
  }

  /** The menu key and shift f10: the context menu of the lead cell. */
  private onMenuKey(e: KeyboardEvent) {
    const f10 = e.key === 'F10' && e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey;
    if (e.isComposing || (e.key !== 'ContextMenu' && !f10)) {
      return;
    }
    const target = e.target instanceof Element ? e.target : null;
    if (target?.closest('revogr-edit')) {
      // the field of an open edit has the menu of the browser
      return;
    }
    e.preventDefault();
    e.stopPropagation();
    this.suppressContextMenuUntil = performance.now() + MENU_KEY_SUPPRESS_MS;
    this.keyboardContextMenu(f10 ? 'F10' : 'ContextMenu');
  }

  /** The context menu of the lead cell (the server takes its own lead), placed under the cell when it is on screen. */
  private keyboardContextMenu(key: string) {
    const base = this.menuBase().getBoundingClientRect();
    const own = this.getBoundingClientRect();
    const focus = this.focusCell;
    const cell = focus
      ? this.querySelector(`revogr-data[type="rgRow"][col-type="rgCol"] .rgCell[data-rgrow="${focus.y}"][data-rgcol="${focus.x}"]`)
      : null;
    const rect = cell?.getBoundingClientRect();
    const left = rect && rect.width > 0 ? rect.left : own.left;
    const top = rect && rect.height > 0 ? rect.bottom : own.top;
    this.emit('consulo-grid-contextmenu', {
      area: 'CELL', row: focus ? focus.y : -1, col: focus ? focus.x : -1,
      x: Math.round(left - base.left), y: Math.round(top - base.top),
      screenX: Math.round(window.screenX + left), screenY: Math.round(window.screenY + top),
      keyboard: true, key, alt: false, ctrl: false, shift: key === 'F10', meta: false
    });
  }

  /** What a context menu gesture hit: a cell, a column header, a row number, or nothing of these. */
  private hitTest(target: Element | null): Hit {
    if (!target) {
      return NO_HIT;
    }
    const header = target.closest('.rgHeaderCell');
    if (header) {
      // the header of the row numbers is the corner
      const col = header.closest('revogr-row-headers') ? -1 : indexAttribute(header, 'data-rgcol');
      return col >= 0 && col < this.cols.items.length ? { area: 'COLUMN_HEADER', row: -1, col } : NO_HIT;
    }
    const cell = target.closest('.rgCell[data-rgrow]');
    if (!cell) {
      return NO_HIT;
    }
    const row = indexAttribute(cell, 'data-rgrow');
    if (!(row >= 0 && row < this.model.count)) {
      return NO_HIT;
    }
    if (cell.closest('revogr-row-headers')) {
      return { area: 'ROW_HEADER', row, col: -1 };
    }
    const col = indexAttribute(cell, 'data-rgcol');
    return col >= 0 && col < this.cols.items.length ? { area: 'CELL', row, col } : NO_HIT;
  }

  /** The box the menu is placed relative to: the one which holds the grid and its paging bar, the grid component. */
  private menuBase(): Element {
    return this.parentElement ?? this;
  }

  /** A press which asks for the context menu: the secondary button, or control with the primary one on macOS. */
  private static isContextPress(e: MouseEvent): boolean {
    return e.button === 2 || (IS_MAC && e.button === 0 && e.ctrlKey);
  }

  // endregion

  // region cell editing

  /** RevoGrid opens its editor: a double click, enter, or a typed key (which `val` holds). */
  private onBeforeEditStart(e: CustomEvent<BeforeSaveDataDetails>) {
    const detail = e.detail;
    const open = this.session;
    if (!this.cfg.editable || (open && !open.done) || detail.type !== 'rgRow' || detail.colType !== 'rgCol'
      || !(detail.rowIndex >= 0 && detail.rowIndex < this.model.count)
      || !(detail.colIndex >= 0 && detail.colIndex < this.cols.items.length)) {
      e.preventDefault();
      return;
    }
    const typed = typeof detail.val === 'string' ? detail.val : '';
    const mouse = typed === '' && performance.now() - this.lastDblClick < DBLCLICK_EDIT_MS;
    // whether the cell is edited, and how, is the server's: GridCellEditorFactoryProvider per cell, no column flag
    this.openSession(detail.rowIndex, detail.colIndex, typed, mouse);
  }

  /** A key RevoGrid is about to handle (once per overlay of the grid: several times per key). */
  private onGridKey(e: Event, key: KeyboardEvent) {
    // a key is acted on when the first overlay reports it: the enter which opened the editor in one overlay must not
    // commit it when the next overlay reports the same key
    const first = this.firstSight(key);
    const s = this.session;
    if (s && !s.done) {
      const inEditor = key.target instanceof Element && !!key.target.closest('revogr-edit');
      if (key.key === 'Escape' && !key.isComposing && first) {
        // the one cancel of an edit: RevoGrid drops its editor after a commit as well
        s.done = true;
        this.cancelEdit(s);
        if (s.editor) {
          s.editor.dismiss();
        }
        else {
          this.finishSession(s);
        }
      }
      else if (key.key === 'Enter' && !inEditor && !hasModifier(key) && first) {
        // enter before the field holds the focus: it commits as soon as it can
        if (s.editor) {
          s.editor.enter();
        }
        else {
          s.commitOnApply = true;
        }
      }
      return;
    }
    if (key.key === 'Delete' || key.key === 'Backspace') {
      // RevoGrid would clear the cells (one cell through beforeedit, as a commit of '' would come) - delete goes to the
      // keymap
      e.preventDefault();
      return;
    }
    if (this.cfg.editable && key.key === 'F2' && !hasModifier(key) && first) {
      // the editor of the focused cell - RevoGrid has no f2
      e.preventDefault();
      void this.grid?.getFocused().then(focused => {
        if (focused && focused.rowType === 'rgRow' && focused.colType === 'rgCol') {
          this.queue(() => this.openEditor(focused.cell.y, focused.cell.x, ''));
        }
      });
    }
  }

  private firstSight(key: KeyboardEvent): boolean {
    if (this.seenKeys.has(key)) {
      return false;
    }
    this.seenKeys.add(key);
    return true;
  }

  private openSession(row: number, col: number, typed: string, mouse: boolean): EditSession {
    const s: EditSession = {
      seq: ++this.editSeq, row, col, typed, spec: null, editor: null, done: false, commitOnApply: false
    };
    this.session = s;
    this.updateOwnKeys();
    this.emit('consulo-grid-edit-start', { seq: s.seq, row, col, typed, mouse });
    return s;
  }

  /**
   * Opens RevoGrid's editor at a cell, which gets the focus first when it is not the focused one (setEdit opens at the
   * focused cell and keeps the range, so a multi-cell edit keeps showing its cells). RevoGrid emits no beforeeditstart
   * for this, so the edit is opened here; when no editor can open, the server is told to drop it.
   */
  private async openEditor(row: number, col: number, typed: string): Promise<void> {
    const grid = this.grid;
    if (!grid || !this.cfg.editable) {
      return;
    }
    if (!this.rendered) {
      this.pendingOpen = { row, col, typed };
      return;
    }
    const open = this.session;
    if (open && !open.done && open.row === row && open.col === col) {
      return;
    }
    if (!(row >= 0 && row < this.model.count && col >= 0 && col < this.cols.items.length)) {
      this.abandon(this.openSession(row, col, typed, false));
      return;
    }
    const focused = await grid.getFocused();
    if (!focused || focused.rowType !== 'rgRow' || focused.colType !== 'rgCol' || focused.cell.x !== col || focused.cell.y !== row) {
      // RevoGrid has no editor without its focus on the cell - the server is told the selection this makes
      this.suppressScrollUntil = performance.now() + 300;
      await grid.setCellsFocus({ x: col, y: row }, { x: col, y: row });
      this.focusCell = { x: col, y: row };
      this.report({ x: col, y: row, x1: col, y1: row }, false);
    }
    await this.scrollNearest(row, col);
    const providers = await grid.getProviders();
    const s = this.openSession(row, col, typed, false);
    providers?.selection.setEdit(typed);
    const edit = providers?.selection.edit;
    if (!edit || edit.x !== col || edit.y !== row) {
      this.abandon(s);
    }
  }

  /** An edit no editor opened for - the server drops the edit it has for it. */
  private abandon(s: EditSession) {
    s.done = true;
    this.cancelEdit(s);
    this.finishSession(s);
  }

  /** RevoGrid moved its focus before it created the editor of the open edit, which then never comes. */
  private dropUnmountedEdit() {
    const s = this.session;
    if (s && !s.done && !s.editor) {
      this.abandon(s);
    }
  }

  private finishSession(s: EditSession) {
    s.done = true;
    if (this.session !== s) {
      return;
    }
    this.session = null;
    this.updateOwnKeys();
    // the field took the DOM focus with it - it goes back to the grid, unless the user moved it elsewhere
    queueMicrotask(() => {
      const active = document.activeElement;
      if (this.isConnected && (!active || active === document.body)) {
        this.focus({ preventScroll: true });
      }
    });
  }

  private updateOwnKeys() {
    let keys = OWN_KEYS;
    if (this.cfg.editable) {
      keys = keys.concat(EDITABLE_KEYS);
    }
    if (this.session && !this.session.done) {
      keys = keys.concat(EDITING_KEYS);
    }
    const value = keys.join('\n');
    if (this.getAttribute('consulo-own-keys') !== value) {
      this.setAttribute('consulo-own-keys', value);
    }
  }

  // endregion

  private onMouseDownCapture(e: MouseEvent) {
    const target = e.target as Element | null;
    if (ConsuloDataGrid.isContextPress(e)) {
      // the menu acts on the selection it is opened in - the server selects the cell when it is not selected
      // (consulo-grid-contextmenu) - so RevoGrid neither moves its focus nor starts a column drag (both skip a prevented
      // press). The field of an open edit keeps the press
      if (!target?.closest('revogr-edit')) {
        e.preventDefault();
        if (!this.contains(document.activeElement)) {
          this.focus({ preventScroll: true });
        }
      }
      return;
    }
    if (e.button !== 0) {
      return;
    }
    // RevoGrid prevents the default of mousedown, so DOM focus would never move here on its own
    if (!this.contains(document.activeElement)) {
      this.focus({ preventScroll: true });
    }
    const cell = target?.closest('.rgCell[data-rgrow]');
    if (!cell) {
      return;
    }
    const row = Number(cell.getAttribute('data-rgrow'));
    if (!(row >= 0 && row < this.model.count)) {
      return;
    }
    const inRowHeader = !!cell.closest('revogr-row-headers');
    const toggle = e.ctrlKey || e.metaKey;
    if (!inRowHeader && !toggle) {
      this.anchorRow = row;
      return; // plain or shift press on a cell: RevoGrid's own range selection
    }
    e.stopPropagation();
    e.preventDefault();
    if (toggle) {
      this.anchorRow = row;
      this.emit('consulo-grid-row-toggle', { row });
      return;
    }
    // press on a row number: the whole row, shift extends from the anchor
    const from = e.shiftKey && this.anchorRow >= 0 ? Math.min(this.anchorRow, row) : row;
    const to = e.shiftKey && this.anchorRow >= 0 ? Math.max(this.anchorRow, row) : row;
    if (!e.shiftKey) {
      this.anchorRow = row;
    }
    const lastCol = this.cols.items.length - 1;
    if (lastCol < 0) {
      return;
    }
    const area = { x: 0, y: from, x1: lastCol, y1: to };
    this.suppressScrollUntil = performance.now() + 300;
    void this.grid?.setCellsFocus({ x: area.x, y: area.y }, { x: area.x1, y: area.y1 });
    this.focusCell = { x: area.x, y: area.y };
    this.report(area, false);
  }

  private report(area: Area, adjusting: boolean) {
    // a final report ends the gesture: an adjusting one still queued must not come after it
    if (!adjusting && this.adjustingFrame !== 0) {
      cancelAnimationFrame(this.adjustingFrame);
      this.adjustingFrame = 0;
    }
    const normalized: Area = {
      x: Math.min(area.x, area.x1), y: Math.min(area.y, area.y1),
      x1: Math.max(area.x, area.x1), y1: Math.max(area.y, area.y1)
    };
    // the focused cell is the lead: where RevoGrid opens its editor, and what DataGrid.editSelectedCell() edits
    const focus = this.focusCell;
    const lead = focus && focus.x >= normalized.x && focus.x <= normalized.x1 && focus.y >= normalized.y && focus.y <= normalized.y1
      ? focus : null;
    const key = `${normalized.x},${normalized.y},${normalized.x1},${normalized.y1},${adjusting},${lead?.x},${lead?.y}`;
    if (key === this.lastReported) {
      return;
    }
    this.lastReported = key;
    if (!adjusting) {
      this.area = normalized;
      if (this.marked.size > 0) {
        this.marked.clear();
        this.markedCols = null;
        this.updateRowClasses();
      }
    }
    this.emit(adjusting ? 'consulo-grid-selection-adjusting' : 'consulo-grid-selection', {
      rowStart: normalized.y, rowEnd: normalized.y1, colStart: normalized.x, colEnd: normalized.x1,
      leadRow: lead ? lead.y : -1, leadCol: lead ? lead.x : -1
    });
  }

  private emit(name: string, detail: object) {
    this.dispatchEvent(new CustomEvent(name, { detail }));
  }

  // region copy

  /**
   * Answers the copy event while the grid has the focus: tab separated texts of the selection. The rows the element
   * holds fresh are copied at once; when some are not here (never scrolled to, or stale after a sort or an edit) the
   * server renders the text and the clipboard is written when it arrives.
   */
  private copy(e: ClipboardEvent) {
    // an open editor copies the text of its field
    if (!this.contains(document.activeElement) || (this.session && !this.session.done)) {
      return;
    }
    const columnCount = this.cols.items.length;
    let rows: number[];
    let cols: number[];
    if (this.marked.size > 0) {
      rows = [...this.marked].sort((a, b) => a - b);
      cols = this.markedCols ? [...this.markedCols].sort((a, b) => a - b) : range(0, columnCount - 1);
    }
    else if (this.area) {
      rows = range(this.area.y, this.area.y1);
      cols = range(this.area.x, this.area.x1);
    }
    else {
      return;
    }
    rows = rows.filter(row => row >= 0 && row < this.data.length);
    cols = cols.filter(col => col >= 0 && col < columnCount);
    if (rows.length === 0 || cols.length === 0) {
      return;
    }
    e.preventDefault();
    e.stopPropagation();

    if (rows.every(row => !this.data[row]._stale)) {
      e.clipboardData?.setData('text/plain', this.toTsv(rows, cols));
      return;
    }

    const clipboard = navigator.clipboard;
    if (!clipboard) {
      // no async clipboard (not a secure context): load the rows, so the next copy finds them here
      this.requestRows(rows);
      return;
    }
    const id = ++this.copySeq;
    const text = new Promise<string>((resolve, reject) => {
      this.copyRequests.set(id, resolve);
      setTimeout(() => {
        if (this.copyRequests.delete(id)) {
          reject(new Error('copy timed out'));
        }
      }, COPY_TIMEOUT_MS);
    });
    this.emit('consulo-grid-copy', { id, rows: toRuns(rows), cols: toRuns(cols) });
    // the clipboard item is created inside the copy event, so the browser keeps the user activation for the write
    if (typeof ClipboardItem !== 'undefined' && clipboard.write) {
      const blob = text.then(tsv => new Blob([tsv], { type: 'text/plain' }));
      clipboard.write([new ClipboardItem({ 'text/plain': blob })]).catch(() => {
        void text.then(tsv => clipboard.writeText(tsv)).catch(() => {});
      });
    }
    else {
      void text.then(tsv => clipboard.writeText(tsv)).catch(() => {});
    }
  }

  /** The texts as the grid shows them, a row per line: the lines of a cell of a row of several lines are joined by ⏎. */
  private toTsv(rows: number[], cols: number[]): string {
    return rows.map(row => {
      const target = this.data[row];
      return cols.map(column => String(target['c' + column] ?? '').replace(/\n/g, '⏎')).join('\t');
    }).join('\n');
  }

  // endregion

  // region applying server state

  private applyConfig() {
    this.classList.toggle('no-horizontal-lines', !this.cfg.horizontalLines);
    this.classList.toggle('transparent-column-header', this.cfg.transparentColumnHeader);
    this.classList.toggle('transparent-row-header', this.cfg.transparentRowHeader);
    this.classList.toggle('no-row-hover', !this.cfg.hoverRows);
    if (this.grid) {
      this.grid.readonly = !this.cfg.editable;
      this.applyRowHeaders();
    }
    this.applyRowMetrics();
    this.updateOwnKeys();
  }

  /**
   * The pitch of the rows, and for rows of several lines the pitch of their lines (webDataGrid.css, multi-line-rows): the
   * first line sits where the text of a single-line row sits, the others follow at the pitch of a text line.
   */
  private applyRowMetrics() {
    this.classList.toggle('multi-line-rows', this.rowLines > 1);
    this.style.setProperty('--_consulo-grid-line-px', this.linePx + 'px');
    this.style.setProperty('--_consulo-grid-line-top', Math.max(0, Math.floor((this.rowBase - this.linePx) / 2)) + 'px');
    if (this.grid && this.grid.rowSize !== this.rowSize) {
      this.grid.rowSize = this.rowSize;
    }
  }

  /** The editor font of the colour scheme: the cells, the headers, the editor and the metrics follow it. */
  private applyFont() {
    this.setFontVariables();
    if (this.isConnected) {
      this.remeasure();
      this.loadFont();
    }
  }

  /**
   * The font as the css variables the grid reads (webDataGrid.css has the defaults, the ones an editor shows until its font
   * arrives). The browser falls back per glyph, so a family which never loads still shows the text.
   */
  private setFontVariables() {
    const font = this.fontSpec;
    if (font?.family) {
      this.style.setProperty('--_consulo-grid-font-family', JSON.stringify(font.family) + ', monospace');
    }
    else {
      this.style.removeProperty('--_consulo-grid-font-family');
    }
    if (font && font.size > 0) {
      this.style.setProperty('--_consulo-grid-font-size', font.size + 'px');
    }
    else {
      this.style.removeProperty('--_consulo-grid-font-size');
    }
  }

  /** A face served over http is measured against the fallback first - the metrics are taken again once it is in. */
  private loadFont() {
    const font = this.fontSpec;
    if (!font?.family || !document.fonts) {
      return;
    }
    const remeasure = () => {
      if (this.isConnected) {
        this.remeasure();
      }
    };
    void document.fonts.load(`${font.size > 0 ? font.size : 12}px ${JSON.stringify(font.family)}`).then(remeasure, remeasure);
  }

  private applyRowHeaders() {
    const grid = this.grid;
    if (!grid) {
      return;
    }
    if (!this.cfg.rowNumbers) {
      grid.rowHeaders = false;
      return;
    }
    const headers: RowHeaders = {
      prop: '_n',
      name: '',
      size: this.toPx(this.model.rowNumberWidthCh),
      cellTemplate: (_h, props) => String((props.model as Row)._n ?? ''),
      cellProperties: (props): CellProps | undefined => {
        const background = (props.model as Row)._m ?? null;
        return background
          ? { class: { 'consulo-cell-background': true }, style: { '--_consulo-cell-background': background } }
          : undefined;
      }
    };
    grid.rowHeaders = headers;
  }

  private applyColumns() {
    const grid = this.grid;
    if (!grid) {
      return;
    }
    // the widths are the server's (one the user dragged comes back from there), over a new layout of the columns too
    const newLayout = this.cols.gen !== this.dataColumnsGen;
    const columns: ColumnRegular[] = this.cols.items.map((info, index) => {
      const align = info.align === 'right' ? 'align-right' : info.align === 'center' ? 'align-center' : null;
      const headerClass: Record<string, boolean> = { 'consulo-header-sortable': info.sortable };
      if (align) {
        headerClass[align] = true;
      }
      const sort = info.sort ?? '';
      return {
        prop: 'c' + index,
        name: info.title,
        size: this.columnPx(index, info),
        sortable: false,
        editor: 'consulo',
        // the title, and the sort marker a press on which sorts (consulo-grid-header-sort)
        columnTemplate: (h) => {
          const title = h('span', { class: 'consulo-header-title' }, info.title);
          if (!info.sortable) {
            return title;
          }
          return [title, h('span', { class: { 'consulo-sort-marker': true, 'consulo-sorted': sort !== '' } }, sort || UNSORTED_MARKER)];
        },
        columnProperties: (): CellProps => ({ title: info.tooltip, class: headerClass }),
        cellProperties: (props): CellProps | undefined => this.cellProperties(props.model as Row, index, align)
      };
    });
    this.rendered = false;
    grid.columns = columns;

    if (newLayout) {
      // the texts the rows hold are laid out for the previous columns - a row model of the same response is applied
      // before the columns, and a structure change can come with no row model at all - so they go, and come back
      // in the new layout as the rows on screen are asked for again
      for (const row of this.data) {
        for (const key of Object.keys(row)) {
          if (key.charCodeAt(0) === 99 /* 'c' */) {
            delete row[key];
          }
        }
        row._f = undefined;
        row._b = undefined;
        row._c = undefined;
        row._stale = true;
      }
      this.dataColumnsGen = this.cols.gen;
      this.requested.clear();
      if (this.data.length > 0) {
        this.scheduleRefresh();
        this.scheduleWindow();
      }
    }
  }

  /**
   * The width of a column in px: the ch of the server and the padding of the cell - or the px the user dragged it to, while
   * the server keeps the width that drag gave it.
   */
  private columnPx(index: number, info: ColumnInfo): number {
    const resized = this.resized.get(index);
    if (resized && resized.title === info.title && resized.ch === info.widthCh) {
      return resized.px;
    }
    return this.toPx(info.widthCh + CELL_PADDING_CH);
  }

  /**
   * The styles of a data cell: the text styles of its flags, and its colours - the css colours the server sent, set on the cell
   * as its own variables, which webDataGrid.css paints (under the selection, which RevoGrid draws over the cells).
   */
  private cellProperties(row: Row, index: number, align: string | null): CellProps | undefined {
    const flags = row._f?.[index] ?? 0;
    const background = row._b?.[index] ?? null;
    const foreground = row._c?.[index] ?? null;
    const marked = row._mk === true && this.markedCols !== null && this.markedCols.has(index);
    if (flags === 0 && !background && !foreground && !marked) {
      return align ? { class: { [align]: true } } : undefined;
    }
    const cls: Record<string, boolean> = {
      'consulo-cell-null': (flags & FLAG_NULL) !== 0,
      'consulo-cell-grayed': (flags & FLAG_GRAYED) !== 0,
      'consulo-cell-error': (flags & FLAG_ERROR) !== 0,
      'consulo-cell-background': !!background,
      'consulo-cell-foreground': !!foreground,
      'consulo-cell-marked': marked
    };
    if (align) {
      cls[align] = true;
    }
    const style: Record<string, string> = {};
    if (background) {
      style['--_consulo-cell-background'] = background;
    }
    if (foreground) {
      style['--_consulo-cell-foreground'] = foreground;
    }
    return { class: cls, style };
  }

  private applyRowModel() {
    const grid = this.grid;
    if (!grid) {
      return;
    }
    const count = this.model.count;
    // same columns: keep the old texts on screen (marked stale) until the new ones arrive, so a re-sort does not flash
    const reuse = this.dataColumnsGen === this.cols.gen;
    const next: Row[] = new Array(count);
    for (let i = 0; i < count; i++) {
      const old = reuse ? this.data[i] : undefined;
      if (old) {
        old._stale = true;
        next[i] = old;
      }
      else {
        next[i] = { _n: '', _cls: '', _stale: true };
      }
    }
    this.data = next;
    this.dataColumnsGen = this.cols.gen;
    this.requested.clear();
    this.lastViewport = '';
    this.updateRowClasses(false);
    this.applyRowHeaders();
    this.rendered = false;
    grid.source = next; // a new array: RevoGrid keeps scroll position and range
    this.scheduleWindow();
  }

  private queueSelection() {
    this.queue(() => this.applySelection());
  }

  /** Runs a task after those queued before it, so an editor opens on the cell a selection of the same response focused. */
  private queue(task: () => Promise<void>) {
    this.work = this.work.then(task, task).catch(() => {});
  }

  private async applySelection() {
    const grid = this.grid;
    const sel = this.sel;
    if (!grid || !sel) {
      return;
    }
    if (!this.rendered) {
      this.selectionPending = true;
      return;
    }
    this.marked.clear();
    this.markedCols = null;
    this.lastReported = '';
    if (sel.rows.length === 0 || sel.cols.length === 0) {
      this.area = null;
      await this.clearRange();
    }
    else if (sel.rows.length === 1 && sel.cols.length === 1) {
      const want: Area = { x: sel.cols[0][0], y: sel.rows[0][0], x1: sel.cols[0][1], y1: sel.rows[0][1] };
      this.area = want;
      // RevoGrid focuses the first cell given; the lead stays focused when it is a corner (a RevoGrid range always has its
      // focus in a corner), so its editor opens there
      const lead: Cell = sel.lead && isCorner(want, { x: sel.lead[1], y: sel.lead[0] })
        ? { x: sel.lead[1], y: sel.lead[0] } : { x: want.x, y: want.y };
      const end: Cell = { x: lead.x === want.x ? want.x1 : want.x, y: lead.y === want.y ? want.y1 : want.y };
      const current = await grid.getSelectedRange();
      const focused = current ? await grid.getFocused() : null;
      if (!current || current.x !== want.x || current.y !== want.y || current.x1 !== want.x1 || current.y1 !== want.y1
        || !focused || focused.cell.x !== lead.x || focused.cell.y !== lead.y) {
        this.suppressScrollUntil = performance.now() + 300;
        await grid.setCellsFocus(lead, end);
      }
      this.focusCell = lead;
    }
    else {
      // not a rectangle (ctrl-toggled rows or columns, or a selection set through the API): tint its cells instead - the
      // rows whole, when every column is selected
      this.area = null;
      for (const row of fromRuns(sel.rows)) {
        this.marked.add(row);
      }
      const cols = fromRuns(sel.cols).filter(col => col >= 0 && col < this.cols.items.length);
      this.markedCols = cols.length >= this.cols.items.length ? null : new Set(cols);
      await this.clearRange();
    }
    this.updateRowClasses();
  }

  private async clearRange() {
    this.focusCell = null;
    this.programmatic++;
    try {
      await this.grid?.clearFocus();
    }
    finally {
      this.programmatic--;
    }
  }

  private async scrollNearest(row: number, column: number) {
    const grid = this.grid;
    if (!grid) {
      return;
    }
    const providers = await grid.getProviders();
    if (!providers) {
      return;
    }
    const target: { x?: number; y?: number } = {};
    if (row >= 0 && row < this.model.count) {
      const y = nearestCoordinate(providers, 'rgRow', row);
      if (y !== null) {
        target.y = y;
      }
    }
    if (column >= 0 && column < this.cols.items.length) {
      const x = nearestCoordinate(providers, 'rgCol', column);
      if (x !== null) {
        target.x = x;
      }
    }
    if (target.x !== undefined || target.y !== undefined) {
      await grid.scrollToCoordinate(target).catch(() => {});
    }
  }

  // endregion

  // region rows

  private fill(target: Row, payload: RowPayload, colors: string[]) {
    target._n = payload.n;
    target._f = payload.f;
    target._b = payload.b?.map(index => colorOf(colors, index));
    target._c = payload.c?.map(index => colorOf(colors, index));
    target._m = colorOf(colors, payload.m);
    target._stale = false;
    for (let column = 0; column < payload.t.length; column++) {
      target['c' + column] = payload.t[column];
    }
  }

  private updateRowClasses(refresh = true) {
    const striped = this.cfg.striped;
    // a selection of some of the columns tints its cells (cellProperties), one of every column the rows
    const wholeRows = this.markedCols === null;
    for (let row = 0; row < this.data.length; row++) {
      const target = this.data[row];
      const odd = striped && row % 2 === 1;
      const marked = this.marked.has(row);
      const selected = marked && wholeRows;
      target._mk = marked;
      target._cls = odd ? (selected ? 'consulo-odd consulo-selected' : 'consulo-odd') : (selected ? 'consulo-selected' : '');
    }
    if (refresh) {
      this.scheduleRefresh();
    }
  }

  private scheduleRefresh() {
    if (this.refreshFrame === 0) {
      this.refreshFrame = requestAnimationFrame(() => {
        this.refreshFrame = 0;
        void this.grid?.refresh('rgRow');
      });
    }
  }

  private scheduleWindow() {
    if (this.windowFrame === 0) {
      this.windowFrame = requestAnimationFrame(() => {
        this.windowFrame = 0;
        this.requestWindow();
      });
    }
  }

  /**
   * Reports the rows on screen, and asks the server for the stale chunks around them, merged into contiguous ranges.
   * A chunk asked for and not answered in time is asked for again.
   */
  private requestWindow() {
    const grid = this.grid;
    const count = this.model.count;
    if (!grid || count === 0) {
      return;
    }
    const chunk = Math.max(1, this.cfg.chunkSize);
    const viewport = grid.querySelector('revogr-viewport-scroll.rgCol') as HTMLElement | null;
    const height = viewport?.clientHeight || this.clientHeight;
    const top = Math.min(count - 1, this.scrollTopRow());
    const visible = Math.max(1, Math.ceil(height / this.rowSize));
    this.reportViewport(top, Math.min(count, top + visible));

    const first = Math.max(0, top - chunk / 2);
    const last = Math.min(count - 1, top + visible + chunk / 2);
    const now = performance.now();
    let waiting = false;
    let from = -1;
    let to = -1;
    const flush = () => {
      if (from >= 0) {
        this.emit('consulo-grid-rows-needed', { gen: this.model.gen, from, to: Math.min(count, to) });
      }
      from = -1;
    };
    for (let k = Math.floor(first / chunk); k <= Math.floor(last / chunk); k++) {
      const start = k * chunk;
      const end = Math.min(count, start + chunk);
      let stale = false;
      for (let row = start; row < end && !stale; row++) {
        stale = this.data[row]._stale;
      }
      if (!stale) {
        flush();
        continue;
      }
      waiting = true;
      const askedAt = this.requested.get(k);
      if (askedAt !== undefined && now - askedAt < REQUEST_RETRY_MS) {
        flush();
        continue;
      }
      this.requested.set(k, now);
      if (from < 0) {
        from = start;
      }
      to = end;
    }
    flush();
    if (waiting) {
      this.scheduleRetry();
    }
  }

  /** Asks for the stale chunks holding the given rows - the rows of a copy which could not wait for them. */
  private requestRows(rows: number[]) {
    const chunk = Math.max(1, this.cfg.chunkSize);
    const now = performance.now();
    const chunks = new Set(rows.filter(row => this.data[row]._stale).map(row => Math.floor(row / chunk)));
    for (const k of chunks) {
      this.requested.set(k, now);
      this.emit('consulo-grid-rows-needed', { gen: this.model.gen, from: k * chunk, to: Math.min(this.model.count, (k + 1) * chunk) });
    }
    if (chunks.size > 0) {
      this.scheduleRetry();
    }
  }

  private scheduleRetry() {
    if (this.retryTimer === 0) {
      this.retryTimer = window.setTimeout(() => {
        this.retryTimer = 0;
        this.scheduleWindow();
      }, REQUEST_RETRY_MS + 100);
    }
  }

  /** The rows on screen, so the server can send them at once with the next reload instead of waiting to be asked. */
  private reportViewport(from: number, to: number) {
    const key = `${this.model.gen}:${from}:${to}`;
    if (key !== this.lastViewport) {
      this.lastViewport = key;
      this.emit('consulo-grid-viewport', { gen: this.model.gen, from, to });
    }
  }

  private scrollTopRow(): number {
    return Math.floor(this.scrollTopPx / this.rowSize);
  }

  private forgetChunks(first: number, last: number) {
    const chunk = Math.max(1, this.cfg.chunkSize);
    for (let k = Math.floor(Math.max(0, first) / chunk); k <= Math.floor(Math.max(0, last) / chunk); k++) {
      this.requested.delete(k);
    }
  }

  // endregion

  // region metrics

  /**
   * The width of 1ch of the grid font (the editor font), the row pitch (webDataGrid.css: a vaadin-grid row, or the line of
   * the font when that is taller), and the pitch of the text lines of a row of several lines. Nothing is measured while the
   * element is hidden - the previous values stay, and the size observer measures again once it is shown.
   */
  private measure(): boolean {
    const probe = document.createElement('div');
    probe.style.cssText = 'position:absolute;visibility:hidden;pointer-events:none;'
      + 'font-family:var(--_consulo-grid-font-family, monospace);font-size:var(--_consulo-grid-font-size, 12px);width:100ch;'
      + 'height:var(--_consulo-grid-row-height, calc(var(--aura-line-height-m) + 2 * var(--vaadin-padding-block-container)))';
    const line = document.createElement('div');
    line.style.cssText = 'position:absolute;visibility:hidden;pointer-events:none;width:1px;'
      + 'font-size:var(--_consulo-grid-font-size, 12px);height:var(--_consulo-grid-line-height, 1.25em)';
    this.append(probe, line);
    const box = probe.getBoundingClientRect();
    const lineBox = line.getBoundingClientRect();
    probe.remove();
    line.remove();
    if (box.width <= 0 || box.height <= 0) {
      this.metricsPending = true;
      return false;
    }
    this.metricsPending = false;
    const chPx = box.width / 100;
    const rowBase = Math.round(box.height);
    const linePx = Math.max(1, Math.min(rowBase, Math.round(lineBox.height) || rowBase));
    const changed = chPx !== this.chPx || rowBase !== this.rowBase || linePx !== this.linePx;
    if (chPx !== this.chPx) {
      // the px of a dragged width were px of the previous font
      this.resized.clear();
    }
    this.chPx = chPx;
    this.rowBase = rowBase;
    this.linePx = linePx;
    this.applyRowMetrics();
    return changed;
  }

  /** Measures again and re-applies every width taken from the metrics. */
  private remeasure() {
    if (this.measure()) {
      this.applyColumns();
      this.applyRowHeaders();
      this.scheduleWindow();
    }
  }

  /** Widths come from Java in ch of the grid font, the editor font. */
  private toPx(widthCh: number): number {
    return Math.max(30, Math.round(widthCh * this.chPx));
  }

  // endregion
}

if (!customElements.get('consulo-data-grid')) {
  customElements.define('consulo-data-grid', ConsuloDataGrid);
}
