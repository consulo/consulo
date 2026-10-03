import * as echarts from 'echarts/core';
import { CustomChart, LineChart } from 'echarts/charts';
import { GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
  LineChart, CustomChart, GraphicComponent, GridComponent, LegendComponent, MarkAreaComponent, TooltipComponent, CanvasRenderer
]);

type Tick = [number, string];

type TimeSeriesModel = {
  type: 'timeSeries';
  xMin: number;
  xMax: number;
  xTicks: Tick[];
  yMax: number | null;
  yTicks: Tick[];
  selection: [number, number] | null;
  series: { name: string; kind: string; color: string; last: string; data: [number, number][] }[];
};

type StatesModel = {
  type: 'states';
  xMin: number;
  xMax: number;
  xTicks: Tick[];
  rows: string[];
  states: { label: string; color: string }[];
  intervals: [number, number, number, number][];
};

type FlameModel = {
  type: 'flame';
  orientation: string;
  xMin: number;
  xMax: number;
  maxDepth: number;
  nodes: [number, number, number, number, string, string, boolean, boolean, string][];
};

type Model = TimeSeriesModel | StatesModel | FlameModel;

type ScrollWindow = {
  offset: number;
  visible: number;
  total: number;
  integral: boolean;
  reversed: boolean;
  x: number;
  top: number;
  height: number;
  thumb: number;
};

type Rect = { x: number; y: number; width: number; height: number };

const TIME_GRID = { left: 64, right: 12, top: 30, bottom: 26 };
const TIME_MIN_HEIGHT = 120;
const STATES_GRID = { left: 150, right: 12, top: 4, bottom: 26 };
const STATE_ROW = 20;
const STATE_MIN_ROWS = 2;
const STATE_PREFERRED_ROWS = 8;
const FLAME_ROW = 22;
const FLAME_MIN_ROWS = 3;
const SCROLLBAR_WIDTH = 6;
const SCROLLBAR_GUTTER = 10;
const SCROLLBAR_MIN_THUMB = 16;
const WHEEL_LINE_PX = 33;

const HOST_STYLE_ID = 'consulo-echart-host-style';
const HOST_STYLE = `
consulo-echart {
  position: relative;
  box-sizing: border-box;
  flex: 1 1 var(--consulo-echart-basis, auto);
  min-width: 0;
  min-height: var(--consulo-echart-min-height, ${TIME_MIN_HEIGHT}px);
}
`;

function installHostStyle(element: HTMLElement) {
  const root = element.getRootNode();
  const shadow = root instanceof ShadowRoot ? root : null;
  if ((shadow ?? document).getElementById(HOST_STYLE_ID)) {
    return;
  }
  const style = document.createElement('style');
  style.id = HOST_STYLE_ID;
  style.textContent = HOST_STYLE;
  (shadow ?? document.head).appendChild(style);
}

function cssVar(element: HTMLElement, name: string, fallback: string): string {
  const value = getComputedStyle(element).getPropertyValue(name).trim();
  return value.length > 0 ? value : fallback;
}

function tickLabel(ticks: Tick[], value: number): string {
  let best = '';
  let distance = Number.POSITIVE_INFINITY;
  for (const [tick, label] of ticks) {
    const current = Math.abs(tick - value);
    if (current < distance) {
      distance = current;
      best = label;
    }
  }
  return best;
}

function tickValues(ticks: Tick[]): number[] {
  return ticks.map((tick) => tick[0]);
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function statesHeight(rows: number): number {
  return STATES_GRID.top + STATES_GRID.bottom + rows * STATE_ROW;
}

function coordRect(params: any): Rect {
  return { x: params.coordSys.x, y: params.coordSys.y, width: params.coordSys.width, height: params.coordSys.height };
}

class ConsuloEChart extends HTMLElement {
  private chart: echarts.ECharts | null = null;
  private container: HTMLDivElement | null = null;
  private observer: ResizeObserver | null = null;
  private model: Model | null = null;
  private dragStart: number | null = null;
  private scrollDrag: { y: number; offset: number } | null = null;
  private scrollWindow: ScrollWindow | null = null;
  private rowOffset = 0;
  private minHeight = '';
  private basis = '';

  private readonly windowMouseMove = (event: MouseEvent) => this.onMouseMove(event);
  private readonly windowMouseUp = () => {
    this.dragStart = null;
    this.scrollDrag = null;
  };

  constructor() {
    super();
    this.addEventListener('wheel', (event) => this.onWheel(event), { passive: false });
    this.addEventListener('mousedown', (event) => this.onMouseDown(event));
  }

  connectedCallback() {
    installHostStyle(this);
    if (!this.style.display) {
      this.style.display = 'block';
    }

    let container = this.container;
    if (!container) {
      container = document.createElement('div');
      container.style.position = 'absolute';
      container.style.inset = '0';
      this.container = container;
    }
    if (container.parentNode !== this) {
      this.appendChild(container);
    }

    const chart = echarts.init(container, null, { renderer: 'canvas' });
    this.chart = chart;
    this.observer = new ResizeObserver(() => {
      this.chart?.resize();
      this.render();
    });
    this.observer.observe(this);

    chart.on('click', (params: any) => this.emitNode('consulo-chart-select', params));
    chart.on('dblclick', (params: any) => this.emitNode('consulo-chart-dblclick', params));
    chart.getZr().on('click', (event: any) => {
      if (!event.target && this.model?.type === 'flame' && !this.inScrollbar(event.offsetX)) {
        this.dispatchEvent(new CustomEvent('consulo-chart-select', { detail: { id: -1 } }));
      }
    });

    window.addEventListener('mousemove', this.windowMouseMove);
    window.addEventListener('mouseup', this.windowMouseUp);

    this.render();
  }

  disconnectedCallback() {
    window.removeEventListener('mousemove', this.windowMouseMove);
    window.removeEventListener('mouseup', this.windowMouseUp);
    this.observer?.disconnect();
    this.observer = null;
    this.chart?.dispose();
    this.chart = null;
  }

  update(json: string) {
    this.model = JSON.parse(json) as Model;
    this.render();
  }

  private emitNode(name: string, params: any) {
    if (this.model?.type === 'flame' && params.data) {
      this.dispatchEvent(new CustomEvent(name, { detail: { id: params.data[0] } }));
    }
  }

  private localX(event: MouseEvent): number {
    return event.clientX - this.getBoundingClientRect().left;
  }

  private plotRatio(x: number): number | null {
    if (this.model?.type !== 'timeSeries') {
      return null;
    }
    const width = this.clientWidth - TIME_GRID.left - TIME_GRID.right;
    if (width <= 0) {
      return null;
    }
    return clamp((x - TIME_GRID.left) / width, 0, 1);
  }

  private isScrollable(): boolean {
    const scroll = this.scrollWindow;
    return scroll !== null && scroll.total > scroll.visible;
  }

  private inScrollbar(x: number): boolean {
    return this.isScrollable() && this.scrollWindow !== null && x >= this.scrollWindow.x - 2;
  }

  private onWheel(event: WheelEvent) {
    const model = this.model;
    if (!model) {
      return;
    }
    if (model.type === 'timeSeries') {
      const ratio = this.plotRatio(this.localX(event));
      if (ratio === null) {
        return;
      }
      event.preventDefault();
      this.dispatchEvent(new CustomEvent('consulo-chart-wheel', {
        detail: { count: event.deltaY / 100, anchor: ratio, zoom: event.ctrlKey || event.metaKey }
      }));
      return;
    }

    const scroll = this.scrollWindow;
    if (!scroll || !this.isScrollable() || event.ctrlKey || event.metaKey || event.deltaY === 0) {
      return;
    }
    let lines = event.deltaY / WHEEL_LINE_PX;
    if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) {
      lines = event.deltaY;
    }
    else if (event.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
      lines = event.deltaY * Math.max(1, Math.floor(scroll.visible));
    }
    const rows = Math.sign(lines) * Math.max(1, Math.round(Math.abs(lines)));
    if (this.scrollRows(this.rowOffset + (scroll.reversed ? -rows : rows))) {
      event.preventDefault();
    }
  }

  private scrollRows(offset: number): boolean {
    const scroll = this.scrollWindow;
    if (!scroll) {
      return false;
    }
    let target = clamp(offset, 0, Math.max(0, scroll.total - scroll.visible));
    if (scroll.integral) {
      target = Math.round(target);
    }
    if (target === this.rowOffset) {
      return false;
    }
    this.rowOffset = target;
    this.render();
    return true;
  }

  private onMouseDown(event: MouseEvent) {
    if (event.button !== 0) {
      return;
    }
    const x = this.localX(event);
    if (this.model?.type !== 'timeSeries') {
      if (this.inScrollbar(x)) {
        event.preventDefault();
        this.scrollDrag = { y: event.clientY, offset: this.rowOffset };
      }
      return;
    }
    const ratio = this.plotRatio(x);
    if (ratio === null) {
      return;
    }
    this.dragStart = ratio;
    this.dispatchEvent(new CustomEvent('consulo-chart-selection', { detail: { from: -1, to: -1 } }));
  }

  private onMouseMove(event: MouseEvent) {
    if ((event.buttons & 1) === 0) {
      return;
    }
    const drag = this.scrollDrag;
    const scroll = this.scrollWindow;
    if (drag && scroll) {
      const travel = Math.max(1, scroll.height - scroll.thumb);
      const delta = (event.clientY - drag.y) / travel * Math.max(0, scroll.total - scroll.visible);
      this.scrollRows(drag.offset + (scroll.reversed ? -delta : delta));
      return;
    }
    if (this.dragStart === null) {
      return;
    }
    const ratio = this.plotRatio(this.localX(event));
    if (ratio === null) {
      return;
    }
    this.dispatchEvent(new CustomEvent('consulo-chart-selection', {
      detail: { from: Math.min(this.dragStart, ratio), to: Math.max(this.dragStart, ratio) }
    }));
  }

  private updateHostSize(model: Model) {
    let minimum = TIME_MIN_HEIGHT;
    let basis = 'auto';
    switch (model.type) {
      case 'states':
        minimum = statesHeight(STATE_MIN_ROWS);
        basis = `${statesHeight(clamp(model.rows.length, STATE_MIN_ROWS, STATE_PREFERRED_ROWS))}px`;
        break;
      case 'flame':
        minimum = FLAME_MIN_ROWS * FLAME_ROW;
        break;
      case 'timeSeries':
        break;
    }
    const minHeight = `${minimum}px`;
    if (minHeight !== this.minHeight) {
      this.minHeight = minHeight;
      this.style.setProperty('--consulo-echart-min-height', minHeight);
    }
    if (basis !== this.basis) {
      this.basis = basis;
      this.style.setProperty('--consulo-echart-basis', basis);
    }
  }

  private updateScrollWindow(total: number, visible: number, integral: boolean, reversed: boolean, x: number, top: number, height: number) {
    let offset = clamp(this.rowOffset, 0, Math.max(0, total - visible));
    if (integral) {
      offset = Math.round(offset);
    }
    this.rowOffset = offset;
    const thumb = total > visible ? Math.max(SCROLLBAR_MIN_THUMB, height * visible / total) : height;
    const scroll = { offset, visible, total, integral, reversed, x, top, height, thumb };
    this.scrollWindow = scroll;
    return scroll;
  }

  private scrollbar(scroll: ScrollWindow, track: string, thumb: string) {
    if (scroll.total <= scroll.visible) {
      return [];
    }
    const range = scroll.total - scroll.visible;
    const ratio = scroll.reversed ? 1 - scroll.offset / range : scroll.offset / range;
    const y = scroll.top + (scroll.height - scroll.thumb) * clamp(ratio, 0, 1);
    return [
      {
        type: 'rect',
        silent: true,
        shape: { x: scroll.x, y: scroll.top, width: SCROLLBAR_WIDTH, height: scroll.height, r: SCROLLBAR_WIDTH / 2 },
        style: { fill: track }
      },
      {
        type: 'rect',
        silent: true,
        shape: { x: scroll.x, y, width: SCROLLBAR_WIDTH, height: scroll.thumb, r: SCROLLBAR_WIDTH / 2 },
        style: { fill: thumb }
      }
    ];
  }

  private render() {
    const chart = this.chart;
    const model = this.model;
    if (!chart || !model) {
      return;
    }
    this.updateHostSize(model);
    if (this.clientWidth === 0 || this.clientHeight === 0) {
      return;
    }
    const text = cssVar(this, '--lumo-secondary-text-color', '#666');
    const grid = cssVar(this, '--lumo-contrast-10pct', 'rgba(0,0,0,0.1)');
    const selection = cssVar(this, '--lumo-primary-color-10pct', 'rgba(0,120,255,0.15)');
    const track = cssVar(this, '--lumo-contrast-5pct', 'rgba(0,0,0,0.05)');
    const thumb = cssVar(this, '--lumo-contrast-30pct', 'rgba(0,0,0,0.3)');

    switch (model.type) {
      case 'timeSeries':
        this.scrollWindow = null;
        chart.setOption(this.timeSeriesOption(model, text, grid, selection), { notMerge: true, lazyUpdate: true });
        break;
      case 'states':
        chart.setOption(this.statesOption(model, text, track, thumb), { notMerge: true, lazyUpdate: true });
        break;
      case 'flame':
        chart.setOption(this.flameOption(model, track, thumb), { notMerge: true, lazyUpdate: true });
        break;
    }
  }

  private timeAxis(min: number, max: number, ticks: Tick[], text: string) {
    return {
      type: 'value',
      min,
      max,
      axisLabel: { color: text, customValues: tickValues(ticks), formatter: (value: number) => tickLabel(ticks, value) },
      axisTick: { customValues: tickValues(ticks) },
      axisLine: { show: false },
      splitLine: { show: false }
    };
  }

  private timeSeriesOption(model: TimeSeriesModel, text: string, grid: string, selection: string) {
    const legend = new Map(model.series.map((series) => [series.name, series.last]));
    return {
      animation: false,
      grid: TIME_GRID,
      legend: {
        right: 8,
        top: 4,
        textStyle: { color: text },
        formatter: (name: string) => `${name}: ${legend.get(name) ?? '-'}`,
        data: model.series.map((series) => ({ name: series.name, icon: 'rect' }))
      },
      xAxis: this.timeAxis(model.xMin, model.xMax, model.xTicks, text),
      yAxis: {
        type: 'value',
        min: 0,
        max: model.yMax ?? undefined,
        axisLabel: { color: text, customValues: tickValues(model.yTicks), formatter: (value: number) => tickLabel(model.yTicks, value) },
        axisTick: { customValues: tickValues(model.yTicks) },
        splitLine: { lineStyle: { color: grid } }
      },
      series: model.series.map((series, index) => ({
        name: series.name,
        type: 'line',
        showSymbol: false,
        data: series.data,
        lineStyle: { width: series.kind === 'AREA' ? 1 : 2, color: series.color },
        itemStyle: { color: series.color },
        areaStyle: series.kind === 'AREA' ? { color: series.color, opacity: 0.6 } : undefined,
        markArea: index === 0 && model.selection
          ? { silent: true, itemStyle: { color: selection }, data: [[{ xAxis: model.selection[0] }, { xAxis: model.selection[1] }]] }
          : undefined
      }))
    };
  }

  private statesOption(model: StatesModel, text: string, track: string, thumb: string) {
    const height = Math.max(STATE_ROW, this.clientHeight - STATES_GRID.top - STATES_GRID.bottom);
    const visible = Math.max(1, Math.floor(height / STATE_ROW));
    const x = this.clientWidth - STATES_GRID.right + (STATES_GRID.right - SCROLLBAR_WIDTH) / 2;
    const scroll = this.updateScrollWindow(model.rows.length, visible, true, false, x, STATES_GRID.top, height);
    const first = scroll.offset;
    const rows = model.rows.slice(first, first + visible);
    while (rows.length < visible) {
      rows.push('');
    }
    const intervals = model.intervals
      .filter((interval) => interval[0] >= first && interval[0] < first + visible)
      .map((interval) => [interval[0] - first, interval[1], interval[2], interval[3]]);

    return {
      animation: false,
      grid: STATES_GRID,
      tooltip: {
        formatter: (params: any) => model.states[params.data[3]]?.label ?? ''
      },
      xAxis: this.timeAxis(model.xMin, model.xMax, model.xTicks, text),
      yAxis: {
        type: 'category',
        inverse: true,
        data: rows,
        axisLabel: { color: text, interval: 0, width: STATES_GRID.left - 12, overflow: 'truncate' },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      graphic: this.scrollbar(scroll, track, thumb),
      series: [{
        type: 'custom',
        encode: { x: [1, 2], y: 0 },
        data: intervals,
        renderItem: (params: any, api: any) => {
          const row = api.value(0);
          const start = api.coord([api.value(1), row]);
          const end = api.coord([api.value(2), row]);
          const barHeight = api.size([0, 1])[1] * 0.7;
          const shape = echarts.graphic.clipRectByRect(
            { x: start[0], y: start[1] - barHeight / 2, width: Math.max(0, end[0] - start[0]), height: barHeight },
            coordRect(params)
          );
          return shape && {
            type: 'rect',
            shape,
            style: { fill: model.states[api.value(3)]?.color ?? '#999' }
          };
        }
      }]
    };
  }

  private flameOption(model: FlameModel, track: string, thumb: string) {
    const flame = model.orientation === 'FLAME';
    const height = Math.max(FLAME_ROW, this.clientHeight);
    const visible = height / FLAME_ROW;
    const total = model.maxDepth + 1;
    const right = total > visible ? SCROLLBAR_GUTTER : 0;
    const x = this.clientWidth - SCROLLBAR_GUTTER + (SCROLLBAR_GUTTER - SCROLLBAR_WIDTH) / 2;
    const scroll = this.updateScrollWindow(total, visible, false, flame, x, 0, height);
    return {
      animation: false,
      grid: { left: 0, right, top: 0, bottom: 0 },
      tooltip: { formatter: (params: any) => params.data[8] },
      xAxis: { type: 'value', min: model.xMin, max: model.xMax, show: false },
      yAxis: { type: 'value', min: scroll.offset, max: scroll.offset + visible, inverse: !flame, show: false },
      graphic: this.scrollbar(scroll, track, thumb),
      series: [{
        type: 'custom',
        data: model.nodes,
        renderItem: (params: any, api: any) => {
          const depth = api.value(1);
          const start = api.coord([api.value(2), depth]);
          const end = api.coord([api.value(3), depth + 1]);
          const width = Math.max(0, end[0] - start[0] - 1);
          if (width < 1) {
            return null;
          }
          const shape = echarts.graphic.clipRectByRect(
            { x: start[0], y: Math.min(start[1], end[1]), width, height: Math.abs(end[1] - start[1]) - 1 },
            coordRect(params)
          );
          if (!shape || shape.height < 1) {
            return null;
          }
          const node = model.nodes[params.dataIndex];
          return {
            type: 'rect',
            shape,
            style: {
              fill: node[5],
              opacity: node[6] ? 0.35 : 1,
              stroke: node[7] ? '#000' : undefined,
              lineWidth: node[7] ? 1 : 0
            },
            textContent: {
              style: { text: node[4], fill: '#000', overflow: 'truncate', width: Math.max(0, width - 6), fontSize: 12 }
            },
            textConfig: { position: 'insideLeft', distance: 3 }
          };
        }
      }]
    };
  }
}

if (!customElements.get('consulo-echart')) {
  customElements.define('consulo-echart', ConsuloEChart);
}
