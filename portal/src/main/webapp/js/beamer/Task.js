var Task = function(text, paraphrases) {
	this._text  = text;
	this._isCompound  = false;
	if(text.match(/\|/)) {
		this._texts = text.split(/\s*\|\s*/);
		this._isCompound = true;
	}
	this._paraphrases = paraphrases ? paraphrases : [];
};

Task.prototype.text = function(text) {
   if(text) this._text = text;
	else return this._text;
};

Task.prototype.isCompound = function() {
	return this._isCompound;
};

Task.prototype.texts = function() {
	return this._texts;
};

Task.prototype.paraphraseExists = function(text) {
	for (var pi in this._paraphrases) {
		var para = this._paraphrases[pi];
		if(para._text.toLowerCase() == text.toLowerCase()) return true;
	}
	return false;
};

Task.prototype.addParaphrase = function(paraphrase) {
	this._paraphrases.push(paraphrase);
};

Task.prototype.paraphrases = function(paraphrases) {
   if(paraphrases) this._paraphrases = paraphrases;
	else return this._paraphrases;
};
