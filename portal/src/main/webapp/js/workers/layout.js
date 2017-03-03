self.importScripts("viz-lite.js");

self.onmessage = function(e) {
	self.postMessage(Viz(e.data, { engine: 'dot', format: "plain" }));
};