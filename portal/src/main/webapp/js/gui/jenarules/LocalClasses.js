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

// ----------------------------------------
var RuleArgProp = function(arg, prop) {
	this._arg = arg;
	this._prop = prop;
};
RuleArgProp.prototype.arg = function(arg) {
   if(arg) this._arg = arg;
	else return this._arg;
};
RuleArgProp.prototype.prop = function(prop) {
   if(prop) this._prop = prop;
	else return this._prop;
};
RuleArgProp.prototype.toString = function() {
	return this._arg.toString()+"."+this._prop.toString();
};
// ----------------------------------------


// ----------------------------------------
var RuleEffect = function(v, val, type) {
	this._v = v;
	this._val = val;
	this._type = type;
};
RuleEffect.prototype.v = function(v) {
   if(v) this._v = v;
	else return this._v;
};
RuleEffect.prototype.val = function(val) {
   if(val) this._val = val;
	else return this._val;
};
RuleEffect.prototype.type = function(type) {
   if(type) this._type = type;
	else return this._type;
};
RuleEffect.prototype.toString = function() {
	return this._v.toString()+(this._type? "."+this._type :"")+" = "+this._val.toString();
};
// ----------------------------------------


// ----------------------------------------
var RuleArg = function(arg) {
	this._arg = arg ? arg : '__ANY';
};
RuleArg.prototype.arg = function(arg) {
	if(arg) this._arg = arg;
	else return this._arg;
};
RuleArg.prototype.toString = function() { return this._arg.toString(); };
// ----------------------------------------
var RuleInput = function(arg) { RuleArg.call(this, arg); };
RuleInput.prototype = Object.create(RuleArg.prototype);
RuleInput.prototype.constructor = RuleInput;
RuleInput.prototype.toString = function() { return 'Inputs.'+this._arg.toString(); };
// ----------------------------------------
var RuleOutput = function(arg) { RuleArg.call(this, arg); };
RuleOutput.prototype = Object.create(RuleArg.prototype);
RuleOutput.prototype.constructor = RuleOutput;
RuleOutput.prototype.toString = function() { return 'Outputs.'+this._arg.toString(); };
// ----------------------------------------

// ----------------------------------------
//var RuleArgID = function(arg) { RuleArg.call(this, arg); };
//RuleArgID.prototype = Object.create(RuleArg.prototype);
//RuleArgID.prototype.constructor = RuleArgID;
// ----------------------------------------
//var RuleArgValue = function(arg) { RuleArg.call(this, arg); };
//RuleArgValue.prototype = Object.create(RuleArg.prototype);
//RuleArgValue.prototype.constructor = RuleArgValue;
// ----------------------------------------
