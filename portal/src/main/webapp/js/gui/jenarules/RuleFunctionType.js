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

var RuleFunctionType = function(name, numargs, argtypes, returntype) {
	this._name = name;
	this._numargs = numargs;
	this._returntype = returntype;
	this._argtypes = argtypes;
};

RuleFunctionType.prototype.name = function(name) {
   if(name) this._name = name;
	else return this._name;
};

RuleFunctionType.prototype.numargs = function(numargs) {
   if(numargs) this._numargs = numargs;
	else return this._numargs;
};

RuleFunctionType.prototype.add_argtype = function(argtype) {
	this._argtypes.push(argtype);
};

RuleFunctionType.prototype.argtypes = function(argtypes) {
   if(argtypes)  this._argtypes = argtypes;
	else return this._argtypes;
};

RuleFunctionType.prototype.returntype = function(returntype) {
   if(returntype) this._returntype = returntype;
	else return this._returntype;
};

