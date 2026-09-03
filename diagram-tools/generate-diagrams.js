const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', 'design-pattern-diagrams');
const colors = { interface: '#FFF1A8', abstract: '#BDEFC4', concrete: '#BFE3FF', data: '#DFC7FF' };

function box(node) {
  const { x, y, w = 230, h = 72, type = 'concrete', title, detail = '' } = node;
  return `<g><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="${colors[type]}" stroke="#34495e" stroke-width="2"/>` +
    `<text x="${x + w / 2}" y="${y + 29}" text-anchor="middle" font-size="16" font-weight="700">${title}</text>` +
    (detail ? `<text x="${x + w / 2}" y="${y + 53}" text-anchor="middle" font-size="13">${detail}</text>` : '') + `</g>`;
}

function edge(nodes, from, to, label, dashed = false) {
  const a = nodes[from], b = nodes[to];
  const ax = a.x + (a.w || 230) / 2, ay = a.y + (a.h || 72) / 2;
  const bx = b.x + (b.w || 230) / 2, by = b.y + (b.h || 72) / 2;
  let x1 = ax, y1 = ay, x2 = bx, y2 = by;
  if (Math.abs(bx - ax) >= Math.abs(by - ay)) {
    x1 += bx > ax ? (a.w || 230) / 2 : -(a.w || 230) / 2;
    x2 += bx > ax ? -(b.w || 230) / 2 : (b.w || 230) / 2;
  } else {
    y1 += by > ay ? (a.h || 72) / 2 : -(a.h || 72) / 2;
    y2 += by > ay ? -(b.h || 72) / 2 : (b.h || 72) / 2;
  }
  const mx = (x1 + x2) / 2, my = (y1 + y2) / 2 - 8;
  return `<g><line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#52616b" stroke-width="2" ${dashed ? 'stroke-dasharray="8 6"' : ''} marker-end="url(#arrow)"/>` +
    `<text x="${mx}" y="${my}" text-anchor="middle" font-size="13" fill="#263238">${label}</text></g>`;
}

function draw(task, pattern, subtitle, nodeList, edgeList) {
  const nodes = Object.fromEntries(nodeList.map(n => [n.id, n]));
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="500" viewBox="0 0 1100 500">
<defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto"><path d="M0,0 L0,6 L9,3 z" fill="#52616b"/></marker></defs>
<rect width="1100" height="500" fill="#F8FAFC"/>
<text x="42" y="48" font-size="26" font-weight="700" fill="#17202a">${task} · ${pattern}</text>
<text x="42" y="76" font-size="15" fill="#52616b">${subtitle}</text>
${edgeList.map(e => edge(nodes, ...e)).join('\n')}
${nodeList.map(box).join('\n')}
<text x="42" y="474" font-size="13" fill="#52616b">노랑: 인터페이스 · 초록: 추상 클래스 · 파랑: 구체 클래스 · 보라: 전달 데이터</text>
</svg>`;
  const dir = path.join(root, task.toLowerCase());
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${pattern.toLowerCase().replaceAll(' ', '-')}.svg`), svg, 'utf8');
}

function diagrams(task, kind) {
  const iface = kind === 'Cut' ? 'CutAlgorithm' : 'ContentAlgorithm';
  const simple = kind === 'Cut' ? 'SimpleCutAlgorithm' : 'SimpleContentAlgorithm';
  const result = kind === 'Cut' ? '절단점' : '콘텐츠 구간';
  const context = kind === 'Cut' ? 'CutContext' : 'ContentContext';
  const strategy = kind === 'Cut' ? 'BlankCutStrategy' : 'BlankRemovalStrategy';
  const template = kind === 'Cut' ? 'CutTemplate' : 'ContentTemplate';
  const concreteTemplate = kind === 'Cut' ? 'BaselineCutTemplate' : 'BaselineContentTemplate';
  const chain = kind === 'Cut' ? 'ChainCutAlgorithm' : 'ChainContentAlgorithm';
  const last = kind === 'Cut' ? 'MakeCutPoints' : 'MakeContentRegions';
  const base = kind === 'Cut' ? 'ImageBoundary' : 'WholeImage';
  const decorator = kind === 'Cut' ? 'BlankCutDecorator' : 'BlankRemovalDecorator';
  const proxy = kind === 'Cut' ? 'CutProxy' : 'ContentProxy';
  const factory = kind === 'Cut' ? 'CutAlgorithmFactory' : 'ContentAlgorithmFactory';
  const element = 'WebtoonImage';
  const visitor = kind === 'Cut' ? 'CutVisitor' : 'ContentVisitor';

  draw(task, 'Strategy', `전략 객체에 ${result} 계산을 위임`, [
    {id:'c',x:70,y:190,title:context},{id:'i',x:420,y:190,type:'interface',title:`«interface» ${kind}Strategy`},{id:'s',x:770,y:190,title:strategy}
  ], [['c','i','위임'],['s','i','구현',true]]);
  draw(task, 'Template Method', '처리 순서는 추상 클래스가 고정', [
    {id:'i',x:70,y:185,type:'interface',title:`«interface» ${iface}`},{id:'a',x:420,y:185,type:'abstract',title:`«abstract» ${template}`,detail:'find → 결과 생성'},{id:'c',x:770,y:185,title:concreteTemplate}
  ], [['a','i','구현',true],['c','a','상속',true]]);
  draw(task, 'Chain of Responsibility', '현재 알고리즘 두 단계를 순서대로 연결', [
    {id:'a',x:40,y:190,title:chain},{id:'h',x:315,y:190,type:'abstract',title:'«abstract» Handler'},{id:'f',x:610,y:120,title:'FindBlankRows'},{id:'m',x:610,y:265,title:last},{id:'d',x:875,y:190,type:'data',title:'Context',detail:`image · blanks · ${result}`}
  ], [['a','h','첫 Handler'],['h','f','1단계'],['f','m','next'],['m','d','결과 기록']]);
  draw(task, 'Decorator', `기본 결과를 감싸 여백 처리 기능을 더함`, [
    {id:'i',x:420,y:110,type:'interface',title:`«interface» ${iface}`},{id:'b',x:125,y:280,title:base},{id:'d',x:420,y:280,title:decorator},{id:'r',x:770,y:280,title:'BlankRows'}
  ], [['b','i','구현',true],['d','i','구현',true],['d','b','감쌈'],['d','r','여백 탐지']]);
  draw(task, 'Proxy', '대리 객체가 같은 인터페이스로 실제 알고리즘에 위임', [
    {id:'i',x:420,y:100,type:'interface',title:`«interface» ${iface}`},{id:'p',x:180,y:275,title:proxy},{id:'r',x:680,y:275,title:simple}
  ], [['p','i','구현',true],['r','i','구현',true],['p','r','그대로 위임']]);
  draw(task, 'Factory', '생성 책임만 Factory에 둠', [
    {id:'f',x:80,y:190,title:factory},{id:'i',x:420,y:190,type:'interface',title:`«interface» ${iface}`},{id:'r',x:770,y:190,title:simple}
  ], [['f','i','반환'],['f','r','생성'],['r','i','구현',true]]);
  draw(task, 'Visitor', '이미지 객체가 Visitor를 받아 알고리즘을 실행', [
    {id:'e',x:70,y:190,title:element,detail:'accept(visitor)'},{id:'i',x:420,y:100,type:'interface',title:'«interface» ImageVisitor'},{id:'v',x:770,y:100,title:visitor},{id:'r',x:420,y:300,title:simple}
  ], [['e','i','accept'],['v','i','구현',true],['v','r','사용']]);
}

diagrams('Task1', 'Cut');
diagrams('Task2', 'Content');
