/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
