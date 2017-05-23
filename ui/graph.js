
const loc = window.location;
const baseUrl = loc.protocol + '//' + loc.hostname + ':8089/api';
const tsneUrl = loc.protocol + '//' + loc.hostname + ':8000/bh.matrix.withPath'; // TODO: use rel path? 

function debug() {
  console.log(arguments);
}

function error(msg) {
  $('#msg').empty().append($('<span>').attr('class', 'error').text(msg));
}

function initGraph() {
  debug('initGraph:');
  createForm();
}

function mkLabeledInput(id, label, val) {
  return [
    $('<label>').attr({for: id}).text(label),
    $('<input>').attr({ id: id, type: 'text', value: val})
  ];
}

function mkOption(text, val) {
  return $('<option>').attr('value', val).text(text);
}

function getVal(sel) { return $(sel).val().trim(); }
function getValInt(sel) { return 1*getVal(sel); }

function createForm() {
  $('#form').append($('<form>')
    .append(mkLabeledInput('nodeId', 'id', '1'))
    .append(mkLabeledInput('maxHops', 'max hops', '3'))
    .append(mkLabeledInput('maxEdges', 'max edges', '100'))
    .append([
      $('<label>').attr({for: 'graphType'}).text('graph type'),
      $('<select>').attr({id: 'graphType'}).append([
        mkOption('top global', '0'),
        mkOption('local', '1'),
        mkOption('T-SNE', '2')
      ]),
      $('<button>').text('Show').click(function(e) {
        e.preventDefault();
        showGraph({ id: getValInt('#nodeId'), maxHops: getValInt('#maxHops'), maxEdges: getValInt('#maxEdges') }, getValInt('#graphType'));
      })
    ])
   );
}

function doAjax(url, data, success, error, method, contentType, dataType) {
  if (!method) method = data ? 'POST' : 'GET';
  if (!contentType) contentType = 'application/json; charset=utf-8';
  if (!dataType) dataType = 'json';
  try {
    debug('doAjax: url =', url, 'data =', data);
    $.ajax({
      type: method,
      url: url,
      data: data,
      contentType: contentType,
      dataType: dataType,
      success: function(data, textStatus, jqXHR) {
        try {
          debug('doAjax success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          success(data);
        } catch (e) {
          debug('doAjax success exception: e = ', e);
          error(e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('doAjax ajax error: jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        error(errorThrown);
      }
    });
  } catch (e) {
    debug('doAjax exception: e =', e);
    error(e.message);
  }
}

function showGraphForId(id) {
  $('#nodeId').val(id);
  $('#graphType').val(1);
  showGraph({ id: id, maxHops: getVal('#maxHops'), maxEdges: getVal('#maxEdges') }, getVal('#graphType'));
}

function showGraph(data, type) {
  // svg.selectAll('*').remove();
  $('svg').empty();
  $('#msg').empty();
  $('#msg').append($('<img>').attr('src', 'loading.gif'));
  if (type == 0) doAjax(baseUrl + '/topConnectedGraph?num=' + data.maxEdges, null, success(data.id), error);
  else if (type == 1) doAjax(baseUrl + '/graph', JSON.stringify(data), success(data.id), error);
  else showTSNE();
}

function distClamped(d) { // I think d3 has a clamping function somewhere
  // my smallest distance is 0.005, bottom decile < 1.2, bottom half < 6.8, log scale may be useful?
  const x = d.distance;
  // console.log('distClamped:', x);
  return !x ? 1 : x < 1 ? 1 : x > 20 ? 20 : x;
}

function success(id) { return function(graph) {
  $('#msg').empty();
  $('#msg').append(graph.nodes.length + ' nodes with ' + graph.edges.length + ' edges. ');
  if (graph.nodes.length > 1000) {
    $('#msg').append("That's too many nodes.");
    return;
  }
  if (graph.edges.length > 1000) {
    $('#msg').append("That's too many edges.");
    return;
  }

  const svg = d3.select("svg"),
        width = +svg.attr("width"),
        height = +svg.attr("height"),
        color = d3.schemeCategory10;
  // debug('graph: width =', width, 'height =', height);

  const simulation = d3.forceSimulation()
      .force("link", d3.forceLink().distance(distClamped).strength(0.2))
      .force("charge", d3.forceManyBody().strength(-200))
      .force("center", d3.forceCenter(width / 2, height / 2));

  const nodes = graph.nodes,
      nodeById = d3.map(nodes, function(d) { return d.id; }),
      links = graph.edges,
      bilinks = [];
  /*    
  if (nodeById.get(id)) {
    debug('got central node');
    nodeById.get(id).type = 8; // central node in a different colour
  } else {
    debug('not got central node');
  };
  */
  
  links.forEach(function(link) {
    var s = link.source = nodeById.get(link.source),
        t = link.target = nodeById.get(link.target),
        i = {}; // intermediate node
    nodes.push(i);
    links.push({source: s, target: i}, {source: i, target: t});
    bilinks.push([s, i, t]);
  });

  var link = svg.append("g")
    .attr("class", "links")
    .selectAll(".link")
    .data(bilinks)
    .enter().append("path")
      .attr("class", "link");

  /*
   * rect that takes zoom events rendered after links and before nodes,
   * so nodes are on top (can still be dragged), then rect is under nodes (pan and zoom if not on a node),
   * and links are at the bottom (won't get any mouse events). Change if links need title mouse-overs.
   */
  svg.append("rect")
  .attr("width", width)
  .attr("height", height)
  .style("fill", "none")
  .style("pointer-events", "all")
  .call(d3.zoom()
      .scaleExtent([0.1, 10])
      .on("zoom", zoomed));
  
  function labelText(d) { return d.label + ' - ' + d.id; }

  var node = svg.append("g")
    .attr("class", "nodes")
    .selectAll(".node")
    .data(nodes.filter(function (d) { return d.id; }))
    .enter().append("g")
      .attr("class", "node")
      .call(d3.drag()
          .on("start", dragstarted)
          .on("drag", dragged)
          .on("end", dragended)
      )
      .on('click', function (e) { showGraphForId(e.id); });
  node.append("circle")
    .attr("r", 5)
    .attr("fill", d => color[d.type]);
      // .append("title").text(labelText);
  node.append("text").attr("dx", 12).attr("dy", ".35em").attr("stroke", "#000").attr('stroke-width', '0.1px').attr('font-size', '8').text(labelText);

  function zoomed() {
    svg.attr("transform", d3.event.transform);
  }
  
  simulation
      .nodes(nodes)
      .on("tick", ticked);

  simulation.force("link")
      .links(links);

  function ticked() {
    link.attr("d", positionLink);
    node.attr("transform", positionNode);
  }
  
  function positionLink(d) {
    return "M" + d[0].x + "," + d[0].y
         + "S" + d[1].x + "," + d[1].y
         + " " + d[2].x + "," + d[2].y;
  }

  function positionNode(d) {
    return "translate(" + d.x + "," + d.y + ")";
  }

  function dragstarted(d) {
    if (!d3.event.active) simulation.alphaTarget(0.3).restart();
    d.fx = d.x, d.fy = d.y;
  }

  function dragged(d) {
    d.fx = d3.event.x, d.fy = d3.event.y;
  }

  function dragended(d) {
    if (!d3.event.active) simulation.alphaTarget(0);
    d.fx = null, d.fy = null;
  }
} }

function showTSNE() {
  const svg = d3.select("svg"),
        svgWidth = +svg.attr("width"),
        svgHeight = +svg.attr("height"),
        margin = {top: 0, right: 0, bottom: 0, left: 0}, // margin = {top: 20, right: 20, bottom: 30, left: 40},
        width = svgWidth - margin.left - margin.right,
        height = svgHeight - margin.top - margin.bottom;
  
  debug('showTSNE: width =', width, 'height =', height);
  const x = d3.scaleLinear().range([0, width]),
        y = d3.scaleLinear().range([height, 0]),
        color = d3.schemeCategory10;
        // xAxis = d3.axisBottom(x),
        // yAxis = d3.axisLeft(y);
  
  svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");
  
  d3.tsv(tsneUrl, function(error, data) {
    $('#msg').empty();
    if (error) throw error;
    data.forEach(d => {
      d.x = +d.x;
      d.y = +d.y;
    });
    x.domain(d3.extent(data, d => d.x)).nice();
    y.domain(d3.extent(data, d => d.y)).nice();
    
    /*
    svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis)
      .append("text")
      .attr("class", "label")
      .attr("x", width)
      .attr("y", -6)
      .style("text-anchor", "end")
      .text("x");
    
    svg.append("g")
      .attr("class", "y axis")
      .call(yAxis)
      .append("text")
      .attr("class", "label")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("y")
    */
    svg.selectAll(".dot")
      .data(data)
      .enter().append("circle")
      .attr("class", "dot")
      .attr("r", 3.5)
      .attr("cx", d => x(d.x))
      .attr("cy", d => y(d.y))
      .style("fill", d => color[1]);
    
    /*
    var legend = svg.selectAll(".legend")
    .data(color.domain())
    .enter().append("g")
    .attr("class", "legend")
    .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });
    
    legend.append("rect")
    .attr("x", width - 18)
    .attr("width", 18)
    .attr("height", 18)
    .style("fill", color);
    
    legend.append("text")
    .attr("x", width - 24)
    .attr("y", 9)
    .attr("dy", ".35em")
    .style("text-anchor", "end")
    .text(function(d) { return d; });
    */
  });
  
}

