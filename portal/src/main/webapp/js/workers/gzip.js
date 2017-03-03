self.importScripts("jsonc.min.js");

self.onmessage = function(e) {
	self.postMessage(gzip.zip(e.data));
};