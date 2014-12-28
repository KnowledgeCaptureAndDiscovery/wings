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

var TrieNode = function(parser, trie_type, trie_search_method, c) {
	this.c = c;
	this.wordCount = 0;
	this.prefixCount = 0;
	this.sentenceCount = 0;
	this.args = [];
	this.children = [];
	this.parser = parser;
	this.trie_type = trie_type;
	this.trie_search = trie_search_method;
};

TrieNode.prototype.type = {io:0, c:1, d:2, prop:3, val:4, v:5, proc:6, misc:99};
TrieNode.prototype.typeRegs = {io:/^(ip|op)/, c:/^c/, d:/^d/, prop:/^p/, val:/^val/, v:/^v/, proc:/^proc/};

TrieNode.prototype.getArgumentType = function(arg, type) {
	if(type==0 && this.typeRegs.proc.test(arg)) return this.type.proc;
	else if(type==1 && this.typeRegs.io.test(arg)) return this.type.io;
	else if(type==1 && this.typeRegs.c.test(arg)) return this.type.c;
	else if(type==1 && this.typeRegs.v.test(arg)) return this.type.v;
	else if(type==3 && this.typeRegs.d.test(arg)) return this.type.d;
	else if(type==3 && this.typeRegs.val.test(arg)) return this.type.val;
	else if(type==3 && this.typeRegs.prop.test(arg)) return this.type.prop;
	else return this.type.misc;
};

TrieNode.prototype.add = function(sentence) {
	if(sentence) {
		this.prefixCount++;
		var c = sentence.charAt(0);
		var substr = sentence.substring(1);

		// Special case of arguments
		if(c == '+') {
			var argstr;
			this.wordCount++;
			var ind = sentence.indexOf(' ');
			if(ind == -1) {
				this.sentenceCount++;
				argstr = sentence.substring(1);
				substr = '';
			}
			else {
				argstr = sentence.substring(1,ind);
				substr = sentence.substring(ind+1);
			}
			arg = this.getArgumentType(argstr, this.trie_type);
			if(!this.args[arg])
				this.args[arg] = new TrieNode(this.parser, this.trie_type, this.trie_search, c);
			if(substr)
				this.args[arg].add(substr);
			return;
		}

		if(c == ' ') this.wordCount++;

		if(!this.children[c]) 
			this.children[c] = new TrieNode(this.parser, this.trie_type, this.trie_search, c);

		this.children[c].add(substr);
	}
	else {
		this.wordCount++;
		this.sentenceCount++;
	}
};


// Get all nodes that can match the prefix
// -- Recursively call the appropriate trie to search
//    for the prefix
// -- Higher ranking to constant matches
TrieNode.prototype.getNodes = function(pfx, rank) {
	var nodes = [];
	var curNodes = [{node:this, pfx:pfx, rank:rank?rank:0}];

	while(curNodes.length) {
		var ns = curNodes.pop();
		var n = ns.node;
		var s = ns.pfx;
		var rank = ns.rank;
		var first = true;

		var skip = false;
//console.log(nodes.length);
//console.log("==============="+s+"==========");
//console.log(nodes);

		while(true) {
			if(!n) break;

			// If this is the 'first' iteration, then proceed even though there isn't anything left
			first = false;

			if(n.args.length && !skip) {
				for(var argtype in n.args) {
					var cnode = n.args[argtype];
					if(typeof(cnode) == "function") continue;

					// Search in the Argument Trie
					var trie = n.trie_search.call(n.parser, argtype);
					if(!trie) continue;

// io:0, c:1, d:2, prop:3, val:4, v:5, proc:6, misc:99
//console.log("Args available for Trie "+trie.trie_type+" in Trie "+this.trie_type);
					if(trie.trie_type == this.trie_type) {
						skip = true; 
						return [];
					}
					var anodes = trie.getNodes(s, rank+1);
					for(var j=0; j<anodes.length; j++) {
						var an = anodes[j].node;
						var as = anodes[j].pfx;
						var arank = anodes[j].rank;
//console.log("-> Got data in Trie "+this.trie_type+" from Trie "+trie.trie_type+" Sent: '"+s+"' Returned:'"+as+"'");

						if(as) 
							curNodes.push({node:cnode, pfx:as, rank:rank+arank});
						else if(s[s.length-1] == ' ') 
							curNodes.push({node:cnode, pfx:as, rank:rank+arank});

						if(an) curNodes.push({node:an, pfx:as, rank:rank+arank});
					}
				}
				skip = true;
			}
			else {
				skip = false;
				if(!s && !first) break;

				if(n.wordCount) {
					rank+=2;
					nodes.push({node:n, pfx:s, rank:rank});
				}

				var c = s[0];
				if(c) {
					s = s.substring(1);
					n = n.children[c];
				}
//console.log("Trie "+this.trie_type+": "+c);
			}
		}
//console.log(n);
//console.log("========= returning '"+s+"' ========");

		//if(s.length && s[0] == ' ') 
		//	s = s.substring(1);
		nodes.push({node:n, pfx:s, rank:rank});
	}
	return nodes;
};

TrieNode.prototype.uniqueArr = function(a) {
	temp = new Array();
	for(var i=0;i<a.length;i++){
		if(!this.arrContains(temp, a[i])) {
			temp.length+=1;
			temp[temp.length-1]=a[i];
		}
	}
	return temp;
};
 
TrieNode.prototype.arrContains = function(a, e) {
	for(var j=0;j<a.length;j++)
		if(a[j]==e) return true;
	return false;
};

TrieNode.prototype.getWordsForPrefix = function(pfx) {
	//pfx = pfx.replace(/(\S)(\s*[,\.])/, "$1 $2");
	var ns = this.getNodes(pfx);
	ns = ns.sort(function(a, b){return b.rank - a.rank;});

	var words = [];
	for(var i=0; i<ns.length; i++) {
		if(!ns[i].pfx && ns[i].node) {
			var nwords = ns[i].node.getWordsForNode();
			for(var j=0; j<nwords.length; j++) {
				words.push(pfx+nwords[j]);
			}
		}
	}
	words = this.uniqueArr(words);

	return words;
};

TrieNode.prototype.getWordsForNode = function() {
	var words = [];
	if(this.wordCount) words.push('');

	for(var c in this.children) {
		var n = this.children[c];
		if(typeof(n) == "function") continue;
		if(c == ' ') continue;
		var cwords = n.getWordsForNode();
		if(cwords.length) {
			for(var j=0; j<cwords.length; j++)
				words.push(c + cwords[j]);
		}
	}
	return words;
};

