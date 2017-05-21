/**
 * Usage: node nersToNodeEdge.js < nerFiltered.json
 * Reads ners from stdin, writes node.json and edge.json.
 */
const readline = require('readline');
const fs = require('fs');

function load(s, callback) {
  const docs = [];
  
  const types = ['PERSON', 'ORGANIZATION', 'LOCATION'];
  var id = 0;
  const nodes = new Map(); // text -> node
  function nodeId(n) {
    const node = nodes.get(n.text);
    if (node) return node.id;
    const id2 = id++;
    nodes.set(n.text, { id: id2, label: n.text, type: types.indexOf(n.typ) });
    return id2;
  }
  
  readline.createInterface({
    input: s
  }).on('line', json => {
    const d = JSON.parse(json);
    docs.push({
      path: d.path, 
      ners: d.ner.map(n => 
        ({ id: nodeId(n), offStr: n.offStr })
      )
    });
  }).on('close', () => {
    s.close();
    const m = new Map(); // id -> node
    for (let n of nodes.values()) { m.set(n.id, n) };
    callback(docs, m);
  });
}

function writeNodesEdges(docs, nodes) {
  const edgeMap = new Map(); // (src, dst) -> edge
  function addOrInc(a, b, dist) {
    const [src, dst] = a < b ? [a, b] : [b, a];
    const k = src + '_' + dst;
    const v = edgeMap.get(k);
    // decay function e^(-x/200) is arbitrary - appears to suit our needs of favouring proximity but still counting more distant pairs
    const d = Math.exp(-dist/200.0); // additive weight at this stage, distance will be 1/this
    if (typeof v === 'undefined') edgeMap.set(k, { source: src, target: dst, distance: d, type: 1 })
    else v.distance += d;
  }
  
  // populate edgeMap
  docs.forEach(d => {
    const n = d.ners;
    n.sort((a, b) => a.offStr - b.offStr);
    for (var i = 0; i < n.length - 1; ++i) {
      var dist;
      for (var j = i + 1; j < n.length && (dist = n[j].offStr - n[i].offStr) < 1000; ++j) {
        // checking for non-equal id's avoids loops
        if (n[i].id != n[j].id) addOrInc(n[i].id, n[j].id, dist);
      };
    };
  });
  
  const ids = new Set();
  const wEdge = fs.createWriteStream('edge.json');
  wEdge.setDefaultEncoding('utf8');
  for (let e of edgeMap.values()) {
    e.distance = 1/e.distance; 
    wEdge.write(JSON.stringify(e) + '\n');
    ids.add(e.source).add(e.target);
  };
  wEdge.end();
  
  const wNode = fs.createWriteStream('node.json');
  wNode.setDefaultEncoding('utf8');
  for (let id of ids) {
    wNode.write(JSON.stringify(nodes.get(id)) + '\n'); // { id: id, label: rslt.search, type: type }
  };
  wNode.end();
}

process.stdin.setEncoding('utf8');
load(process.stdin, writeNodesEdges);