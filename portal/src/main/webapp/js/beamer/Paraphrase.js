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

var Paraphrase = function(text, tokens) {
	this._text = text;
	this._tokens = tokens ? tokens : [];
   if(text) {
		this.parseParaphrase();
		//this.createParaphraseRegex();
	}
};

Paraphrase.prototype.text = function(text) {
   if(text) this._text = text;
	else return this._text;
};

Paraphrase.prototype.tokens = function(tokens) {
   if(tokens) this._tokens = tokens;
	else return this._tokens;
};

Paraphrase.prototype.getToken = function(token_ind) {
   return this._tokens[token_ind];
};

Paraphrase.prototype.setToken = function(token_ind, token) {
   this._tokens[token_ind] = token;
};

Paraphrase.prototype.addToken = function(token) {
   this._tokens.push(token);
};

Paraphrase.prototype.regExps = [/^\+(.*)$/, /^(.*)\[(.*)\]$/, /^(.*)\.\.$/];
Paraphrase.prototype.parseParaphrase = function() {
	this._text = this._text.replace(/\s*([,\.])\s*/g, " $1 ");
	var tokenstrs = this._text.split(/\s+/);
	for(var ti=0; ti<tokenstrs.length; ti++) {
		var name = tokenstrs[ti];
      var type='const';
      var arg_type = undefined;
      var arg_cardinality = undefined;
      var m = this.regExps[0].exec(name);
      if (m) {
         name=m[1];
         type='arg';
         arg_cardinality = 'single';
         arg_type = 'String';
         var m2 = this.regExps[1].exec(name);
         if(m2) {
            name = m2[1];
            arg_type = m2[2];
				var m3 = this.regExps[2].exec(arg_type);
            if(m3) {
               arg_type = m3[1];
               arg_cardinality = 'multiple';
            }
         }
      }
      var token = new Token(name, type, arg_type, arg_cardinality);
      this.addToken(token);
   }
};

Paraphrase.prototype.createParaphraseRegex = function() {
	this.regex = [];
	this.args = [];

	var regexStr = "^";
	for(var j=0; j<this.tokens().length; j++) {
		if(j) regexStr += " ";

		var tok = this.getToken(j);
		if(tok.type() == "const") {
			regexStr += tok.name().replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
		}
		if(tok.type() == "arg") {
			regexStr += "(.+?)";
			this.args.push(tok.name());
		}
	}
	regexStr += "$";
	this.regex = new RegExp(regexStr);
};


Paraphrase.prototype.clone = function() {
	var p = new Paraphrase();
	p.text(this._text);
	for(var i=0; i<this.tokens().length; i++) {
		var t = this.getToken(i);
		p.addToken(new Token(t.name(), t.type(), t.arg_type(), t.arg_cardinality()));
	}
	return p;
};
