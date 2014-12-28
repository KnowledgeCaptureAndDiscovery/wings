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

var Token = function ( name, type, arg_type, arg_cardinality ) {
	this._name            = name;
	this._type            = type  ? type : 'const';
	this._arg_type        = arg_type ? arg_type : undefined;
	this._arg_cardinality = arg_cardinality ? arg_type : undefined;
};


Token.prototype.name = function(name) {
   if(name) this._name = name;
	else return this._name;
};

Token.prototype.type = function(type) {
   if(type) this._type = type;
	else return this._type;
};

Token.prototype.arg_type = function(arg_type) {
   if(arg_type) this._arg_type = arg_type;
	else return this._arg_type;
};

Token.prototype.arg_cardinality = function(arg_cardinality) {
   if(arg_cardinality) this._arg_cardinality = arg_cardinality;
	else return this._arg_cardinality;
};
