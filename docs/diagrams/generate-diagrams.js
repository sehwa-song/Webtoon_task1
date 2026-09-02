const fs = require('fs');
const path = require('path');

const diagrams = [
  {
    file: 'task1-strategy.svg', title: 'Task1 · Strategy',
    nodes: [
      ['context', 'StrategySplitter', 'class', 330, 35],
      ['strategy', '«interface»\nRowClassifier', 'interface', 330, 165],
      ['fixed', 'FixedColorRowClassifier', 'class', 35, 310],
      ['edge', 'EdgeColorRowClassifier', 'class', 310, 310],
      ['hybrid', 'HybridRowClassifier\n(Fixed + Edge 조합)', 'class', 585, 310]
    ],
    edges: [
      ['context', 'strategy', 'uses', '판정 위임'],
      ['fixed', 'strategy', 'implements', '구현'],
      ['edge', 'strategy', 'implements', '구현'],
      ['hybrid', 'strategy', 'implements', '구현']
    ]
  },
  {
    file: 'task1-template-method.svg', title: 'Task1 · Template Method',
    nodes: [
      ['base', '«abstract»\nAbstractWebtoonSplitter', 'abstract', 330, 60],
      ['flow', 'split()\nload → detect → write', 'note', 35, 220],
      ['impl', 'RgbMarginTemplateSplitter', 'class', 585, 220],
      ['result', 'TemplateSplitResult', 'class', 330, 355]
    ],
    edges: [
      ['impl', 'base', 'extends', '상속'],
      ['base', 'flow', 'uses', '순서 고정'],
      ['base', 'result', 'uses', '생성']
    ]
  },
  {
    file: 'task1-chain.svg', title: 'Task1 · Chain of Responsibility',
    nodes: [
      ['detector', 'ChainCutPointDetector', 'class', 330, 25],
      ['handler', '«abstract»\nBlankZoneHandler', 'abstract', 330, 145],
      ['minimum', 'MinimumHeightHandler', 'class', 20, 300],
      ['stability', 'ColoredStabilityHandler', 'class', 285, 300],
      ['safe', 'SafeCutSelectionHandler', 'class', 570, 300]
    ],
    edges: [
      ['detector', 'handler', 'uses', '후보 전달'],
      ['minimum', 'handler', 'extends', '상속'],
      ['stability', 'handler', 'extends', '상속'],
      ['safe', 'handler', 'extends', '상속'],
      ['minimum', 'stability', 'chain', 'next'],
      ['stability', 'safe', 'chain', 'next']
    ]
  },
  {
    file: 'task1-decorator.svg', title: 'Task1 · Decorator',
    nodes: [
      ['component', '«interface»\nCutPointDetector', 'interface', 330, 25],
      ['base', 'RgbMarginCutPointDetector', 'class', 40, 200],
      ['decorator', '«abstract»\nCutPointDetectorDecorator', 'abstract', 570, 200],
      ['validate', 'CutPointValidationDecorator', 'class', 420, 350],
      ['timing', 'TimingCutPointDetectorDecorator', 'class', 690, 350]
    ],
    edges: [
      ['base', 'component', 'implements', '구현'],
      ['decorator', 'component', 'implements', '구현'],
      ['decorator', 'component', 'wraps', '감쌈'],
      ['validate', 'decorator', 'extends', '상속'],
      ['timing', 'decorator', 'extends', '상속']
    ]
  },
  {
    file: 'task1-proxy.svg', title: 'Task1 · Proxy',
    nodes: [
      ['service', 'ProxySplittingService', 'class', 35, 40],
      ['subject', '«interface»\nImageRepository', 'interface', 330, 160],
      ['proxy', 'CachingImageRepositoryProxy', 'class', 35, 325],
      ['real', 'DiskImageRepository', 'class', 625, 325]
    ],
    edges: [
      ['service', 'subject', 'uses', '이미지 요청'],
      ['proxy', 'subject', 'implements', '구현'],
      ['real', 'subject', 'implements', '구현'],
      ['proxy', 'real', 'wraps', '캐시 후 위임']
    ]
  },
  {
    file: 'task1-abstract-factory.svg', title: 'Task1 · Abstract Factory',
    nodes: [
      ['client', 'WebtoonProcessingService', 'class', 25, 25],
      ['factory', '«interface»\nProcessingFamilyFactory', 'interface', 330, 120],
      ['standard', 'StandardProcessingFactory', 'class', 70, 275],
      ['cached', 'CachedProcessingFactory', 'class', 590, 275],
      ['products', 'ImageRepository + CutPointDetector + PieceWriter', 'note', 300, 420]
    ],
    edges: [
      ['client', 'factory', 'uses', '제품군 요청'],
      ['standard', 'factory', 'implements', '구현'],
      ['cached', 'factory', 'implements', '구현'],
      ['standard', 'products', 'uses', '표준 제품 생성'],
      ['cached', 'products', 'uses', '캐시 제품 생성']
    ]
  },
  {
    file: 'task1-composite-visitor.svg', title: 'Task1 · Composite + Visitor',
    nodes: [
      ['element', '«interface»\nWebtoonElement', 'interface', 90, 50],
      ['batch', 'WebtoonBatch\n(Composite)', 'class', 25, 205],
      ['episode', 'WebtoonEpisode\n(Composite)', 'class', 265, 205],
      ['page', 'WebtoonPage\n(Leaf)', 'class', 505, 205],
      ['visitor', '«interface»\nWebtoonVisitor', 'interface', 710, 50],
      ['split', 'SplitPageVisitor', 'class', 710, 300]
    ],
    edges: [
      ['batch', 'element', 'implements', '구현'],
      ['episode', 'element', 'implements', '구현'],
      ['page', 'element', 'implements', '구현'],
      ['batch', 'episode', 'contains', '포함'],
      ['episode', 'page', 'contains', '포함'],
      ['split', 'visitor', 'implements', '구현'],
      ['element', 'visitor', 'uses', 'accept']
    ]
  },
  {
    file: 'task2-strategy.svg', title: 'Task2 · Strategy',
    nodes: [
      ['app', 'StrategyExtractorApp', 'class', 25, 30],
      ['context', 'StrategyExtractor', 'class', 280, 140],
      ['strategy', '«interface»\nContentRegionStrategy', 'interface', 570, 140],
      ['impl', 'MarginContentRegionStrategy', 'class', 570, 315],
      ['region', 'ContentRegion', 'class', 280, 315]
    ],
    edges: [
      ['app', 'context', 'uses', '실행'],
      ['context', 'strategy', 'uses', '추출 위임'],
      ['impl', 'strategy', 'implements', '구현'],
      ['context', 'region', 'uses', '[top,bottom)']
    ]
  },
  {
    file: 'task2-template-method.svg', title: 'Task2 · Template Method',
    nodes: [
      ['base', '«abstract»\nAbstractWebtoonExtractor', 'abstract', 330, 40],
      ['flow', 'extract()\nload → detect → write', 'note', 40, 220],
      ['impl', 'RgbMarginTemplateExtractor', 'class', 600, 220],
      ['result', 'ExtractionResult + ContentRegion', 'class', 330, 370]
    ],
    edges: [
      ['impl', 'base', 'extends', '상속'],
      ['base', 'flow', 'uses', '순서 고정'],
      ['base', 'result', 'uses', '결과 생성']
    ]
  },
  {
    file: 'task2-chain.svg', title: 'Task2 · Chain of Responsibility',
    nodes: [
      ['detector', 'ChainContentRegionDetector', 'class', 330, 20],
      ['handler', '«abstract»\nRemovalCandidateHandler', 'abstract', 330, 130],
      ['anchor', 'ContentAnchorHandler', 'class', 5, 295],
      ['safe', 'SafeRemovalHandler', 'class', 220, 295],
      ['boundary', 'BoundaryForegroundHandler', 'class', 435, 295],
      ['bridge', 'ForegroundBridgeVetoHandler', 'class', 680, 295]
    ],
    edges: [
      ['detector', 'handler', 'uses', '여백 후보 전달'],
      ['anchor', 'handler', 'extends', '상속'],
      ['safe', 'handler', 'extends', '상속'],
      ['boundary', 'handler', 'extends', '상속'],
      ['bridge', 'handler', 'extends', '상속'],
      ['anchor', 'safe', 'chain', 'next'],
      ['safe', 'boundary', 'chain', 'next'],
      ['boundary', 'bridge', 'chain', 'next']
    ]
  },
  {
    file: 'task2-decorator.svg', title: 'Task2 · Decorator',
    nodes: [
      ['component', '«interface»\nContentRegionDetector', 'interface', 330, 20],
      ['base', 'RgbMarginContentRegionDetector', 'class', 35, 185],
      ['decorator', '«abstract»\nContentRegionDetectorDecorator', 'abstract', 585, 185],
      ['validate', 'ValidatingContentRegionDetector', 'class', 430, 345],
      ['metrics', 'MetricsContentRegionDetector', 'class', 720, 345]
    ],
    edges: [
      ['base', 'component', 'implements', '구현'],
      ['decorator', 'component', 'implements', '구현'],
      ['decorator', 'component', 'wraps', '감쌈'],
      ['validate', 'decorator', 'extends', '상속'],
      ['metrics', 'decorator', 'extends', '상속']
    ]
  },
  {
    file: 'task2-proxy.svg', title: 'Task2 · Proxy',
    nodes: [
      ['service', 'ProxyExtractionService', 'class', 30, 35],
      ['subject', '«interface»\nImageRepository', 'interface', 340, 150],
      ['proxy', 'CachingImageRepositoryProxy', 'class', 30, 320],
      ['real', 'DiskImageRepository', 'class', 650, 320]
    ],
    edges: [
      ['service', 'subject', 'uses', '이미지 요청'],
      ['proxy', 'subject', 'implements', '구현'],
      ['real', 'subject', 'implements', '구현'],
      ['proxy', 'real', 'wraps', 'LRU 캐시 후 위임']
    ]
  },
  {
    file: 'task2-abstract-factory.svg', title: 'Task2 · Abstract Factory',
    nodes: [
      ['client', 'WebtoonExtractionService', 'class', 25, 25],
      ['factory', '«interface»\nProcessingFamilyFactory', 'interface', 340, 115],
      ['standard', 'StandardProcessingFactory', 'class', 65, 275],
      ['cached', 'CachedProcessingFactory', 'class', 610, 275],
      ['products', 'ImageRepository + ContentRegionDetector + RegionWriter', 'note', 300, 420]
    ],
    edges: [
      ['client', 'factory', 'uses', '제품군 요청'],
      ['standard', 'factory', 'implements', '구현'],
      ['cached', 'factory', 'implements', '구현'],
      ['standard', 'products', 'uses', '표준 제품 생성'],
      ['cached', 'products', 'uses', '캐시 제품 생성']
    ]
  },
  {
    file: 'task2-composite-visitor.svg', title: 'Task2 · Composite + Visitor',
    nodes: [
      ['element', '«interface»\nWebtoonElement', 'interface', 90, 45],
      ['batch', 'WebtoonBatch\n(Composite)', 'class', 20, 205],
      ['episode', 'WebtoonEpisode\n(Composite)', 'class', 260, 205],
      ['page', 'WebtoonPage\n(Leaf)', 'class', 500, 205],
      ['visitor', '«interface»\nWebtoonVisitor', 'interface', 720, 45],
      ['extract', 'ExtractPageVisitor', 'class', 720, 300],
      ['algorithm', 'VisitorContentAlgorithm', 'class', 500, 390]
    ],
    edges: [
      ['batch', 'element', 'implements', '구현'],
      ['episode', 'element', 'implements', '구현'],
      ['page', 'element', 'implements', '구현'],
      ['batch', 'episode', 'contains', '포함'],
      ['episode', 'page', 'contains', '포함'],
      ['extract', 'visitor', 'implements', '구현'],
      ['element', 'visitor', 'uses', 'accept'],
      ['extract', 'algorithm', 'uses', '추출 위임']
    ]
  }
];

const WIDTH = 1200;
const HEIGHT = 610;
const BOX_W = 245;
const BOX_H = 70;
const X_SCALE = 1.22;
const TOP_OFFSET = 65;

function tx(x) {
  return Math.round(x * X_SCALE);
}

function ty(y) {
  return y + TOP_OFFSET;
}

function esc(value) {
  return value.replace(/[&<>"']/g, ch => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;'
  })[ch]);
}

function center(node) {
  return { x: tx(node[3]) + BOX_W / 2, y: ty(node[4]) + BOX_H / 2 };
}

function validateLayout(diagram) {
  const gap = 8;
  for (let i = 0; i < diagram.nodes.length; i++) {
    const first = diagram.nodes[i];
    for (let j = i + 1; j < diagram.nodes.length; j++) {
      const second = diagram.nodes[j];
      const separated =
        tx(first[3]) + BOX_W + gap <= tx(second[3]) ||
        tx(second[3]) + BOX_W + gap <= tx(first[3]) ||
        ty(first[4]) + BOX_H + gap <= ty(second[4]) ||
        ty(second[4]) + BOX_H + gap <= ty(first[4]);
      if (!separated) {
        throw new Error(`${diagram.file}: ${first[0]} and ${second[0]} overlap`);
      }
    }
  }
}

function edgePoints(from, to) {
  const a = center(from);
  const b = center(to);
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const sx = BOX_W / 2;
  const sy = BOX_H / 2;
  const scale = 1 / Math.max(Math.abs(dx) / sx || 0, Math.abs(dy) / sy || 0);
  return {
    x1: a.x + dx * scale,
    y1: a.y + dy * scale,
    x2: b.x - dx * scale,
    y2: b.y - dy * scale
  };
}

function svg(diagram) {
  validateLayout(diagram);
  const byId = new Map(diagram.nodes.map(n => [n[0], n]));
  const edges = diagram.edges.map(([fromId, toId, type, label]) => {
    const p = edgePoints(byId.get(fromId), byId.get(toId));
    const dashed = type === 'implements' ? ' stroke-dasharray="8 6"' : '';
    const marker = type === 'extends' || type === 'implements'
      ? 'triangle' : type === 'contains' || type === 'wraps' ? 'diamond' : 'arrow';
    const mx = (p.x1 + p.x2) / 2;
    const my = (p.y1 + p.y2) / 2 - 7;
    if (type === 'wraps') {
      const vertical = Math.abs(p.x2 - p.x1) < Math.abs(p.y2 - p.y1);
      const cx = vertical ? mx + 155 : mx;
      const cy = vertical ? my : my - 75;
      const labelX = (p.x1 + 2 * cx + p.x2) / 4;
      const labelY = (p.y1 + 2 * cy + p.y2) / 4 - 8;
      return `<g class="edge"><path d="M ${p.x1} ${p.y1} Q ${cx} ${cy} ${p.x2} ${p.y2}" marker-end="url(#${marker})"/><text x="${labelX}" y="${labelY}">${esc(label)}</text></g>`;
    }
    return `<g class="edge"><line x1="${p.x1}" y1="${p.y1}" x2="${p.x2}" y2="${p.y2}"${dashed} marker-end="url(#${marker})"/><text x="${mx}" y="${my}">${esc(label)}</text></g>`;
  }).join('\n');

  const nodes = diagram.nodes.map(([, label, type, sourceX, sourceY]) => {
    const x = tx(sourceX);
    const y = ty(sourceY);
    const lines = label.split('\n');
    const text = lines.map((line, i) =>
      `<tspan x="${x + BOX_W / 2}" dy="${i === 0 ? (lines.length === 1 ? 41 : 29) : 23}">${esc(line)}</tspan>`
    ).join('');
    return `<g class="node ${type}"><rect x="${x}" y="${y}" width="${BOX_W}" height="${BOX_H}" rx="12"/><text x="${x + BOX_W / 2}" y="${y}">${text}</text></g>`;
  }).join('\n');

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}" role="img" aria-labelledby="title desc">
  <title id="title">${esc(diagram.title)}</title>
  <desc id="desc">핵심 클래스와 인터페이스 관계도</desc>
  <defs>
    <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L0,6 L9,3 z" fill="#475569"/></marker>
    <marker id="triangle" markerWidth="12" markerHeight="12" refX="11" refY="5" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L0,10 L11,5 z" fill="#fff" stroke="#475569"/></marker>
    <marker id="diamond" markerWidth="15" markerHeight="12" refX="14" refY="5" orient="auto" markerUnits="strokeWidth"><path d="M0,5 L6,0 L12,5 L6,10 z" fill="#fff" stroke="#475569"/></marker>
    <style>
      svg { background: #f8fafc; }
      text { font-family: "Pretendard", "Noto Sans KR", "Malgun Gothic", sans-serif; fill: #172033; }
      .title { font-size: 24px; font-weight: 700; }
      .subtitle { font-size: 13px; fill: #64748b; }
      .node rect { stroke-width: 2; }
      .node text { text-anchor: middle; font-size: 15px; font-weight: 650; }
      .class rect { fill: #e0f2fe; stroke: #0284c7; }
      .interface rect { fill: #f3e8ff; stroke: #9333ea; stroke-dasharray: 7 4; }
      .abstract rect { fill: #fef3c7; stroke: #d97706; }
      .note rect { fill: #ecfdf5; stroke: #059669; }
      .edge line, .edge path { fill: none; stroke: #475569; stroke-width: 2; }
      .edge text { text-anchor: middle; font-size: 12px; fill: #475569; paint-order: stroke; stroke: #f8fafc; stroke-width: 5px; }
      .legend { font-size: 12px; fill: #64748b; }
    </style>
  </defs>
  <text class="title" x="24" y="32">${esc(diagram.title)}</text>
  <text class="subtitle" x="24" y="53">핵심 객체 관계만 표시 · 메서드/필드는 생략</text>
  ${edges}
  ${nodes}
  <text class="legend" x="24" y="590">점선 삼각형: implements · 실선 삼각형: extends · 화살표: uses/next · 마름모: contains/wraps</text>
</svg>`;
}

for (const diagram of diagrams) {
  fs.writeFileSync(path.join(__dirname, diagram.file), svg(diagram), 'utf8');
}

console.log(`${diagrams.length} SVG diagrams generated.`);
