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

var Triple = function(s, p, o) {
	this._s = s;
	this._p = p;
	this._o = o;
};

Triple.prototype.s = function(s) {
   if(s) this._s = s;
	else return this._s;
};

Triple.prototype.p = function(p) {
   if(p) this._p = p;
	else return this._p;
};

Triple.prototype.o = function(o) {
   if(o) this._o = o;
	else return this._o;
};

Triple.prototype.toString = function() {
	return "( "+this._s+" "+this._p+" "+this._o+" )";
};
