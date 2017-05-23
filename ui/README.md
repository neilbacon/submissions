# dataFusion - ui2

## Introduction
This project consists of static files providing a demonstration web user interface for a graph of client register entities found nearby in unstructured documents.

[ECMAScript](https://en.wikipedia.org/wiki/ECMAScript) 6 is used so the web page only runs in some modern browsers (Chrome, Firefox, Edge, not yet Safari).

## Configuration

The `url` near the top of `graph.js` must point to the graph web service.

## Running and Usage

CORS access to servers isn't working from a `file:` URL. To use python's simple web server to serve the UI over HTTP, run `python3 -m http.server`. Access the UI at: http://localhost:8000/.
