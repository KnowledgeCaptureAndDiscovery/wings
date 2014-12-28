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

var Rule = function(comp, type, preconditions, effects) {
	this._comp = comp;
	this._type = type;
	this._preconditions = preconditions ? preconditions : [];
	this._effects = effects ? effects : [];
};

Rule.prototype.name = function(name) {
   if(name) this._name = name;
	else return this._name;
};

Rule.prototype.comp = function(comp) {
   if(comp) this._comp = comp;
	else return this._comp;
};

Rule.prototype.type = function(type) {
   if(type) this._type = type;
	else return this._type;
};

Rule.prototype.add_precondition = function(precondition) {
	this._preconditions.push(precondition);
};

Rule.prototype.preconditions = function(preconditions) {
   if(preconditions) this._preconditions = preconditions;
	else return this._preconditions;
};

Rule.prototype.add_effect = function(effect) {
	this._effects.push(effect);
};

Rule.prototype.effects = function(effects) {
   if(effects) this._effects = effects;
	else return this._effects;
};

Rule.prototype.toString = function() {
	var str = "Rule for ["+this._comp+"]\n";
	if(this._type == '__PRECON_INV')
		str += "Component Invalid\n";

	if(this._preconditions.length) {
		str += "If:\n";
		for(var i=0; i<this._preconditions.length; i++) {
			var precond = this._preconditions[i];
			str += "\t"+precond.toString();
			if(i < this._preconditions.length-1) str += " &&";
			str += "\n";
		}
	}

	if(this._effects.length) {
		if(this._preconditions.length) str += "Then: ";
		if(this._type == '__METAPROP')
			str += "Propagate Following Metadata\n";
		else if(this._type == '__PARAMCONFIG')
			str += "Set Following Parameter Values\n";
		else if(this._type == '__COLLCONFIG')
			str += "Set Following Collection Attribute\n";

		for(var i=0; i<this._effects.length; i++) {
			var effect = this._effects[i];
			str += "\t"+effect.toString()+"\n";
		}
	}
	return str;
};

