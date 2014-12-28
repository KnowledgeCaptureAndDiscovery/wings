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

R = {}; // Jena Rules

R.AT = {}; // Arg type
R.AT.ANY = 0;
R.AT.STRING = 1;
R.AT.BOOL = 2;
R.AT.INT = 4;
R.AT.FLOAT = 8;
R.AT.DATETIME = 16;
R.AT.URI = 32;
R.AT.NUMBER = R.AT.INT | R.AT.FLOAT; // 12

R.FT = {}; // Function type
R.FT.EQUAL = new RuleFunctionType("equal", 2, R.AT.ANY, R.AT.BOOL);
R.FT.NOTEQUAL = new RuleFunctionType("notEqual", 2, R.AT.ANY, R.AT.BOOL);

R.FT.LESSTHAN = new RuleFunctionType("lessThan", 2, R.AT.NUMBER | R.AT.DATETIME, R.AT.BOOL);
R.FT.GREATERTHAN = new RuleFunctionType("greaterThan", 2, R.AT.NUMBER | R.AT.DATETIME, R.AT.BOOL);
R.FT.LE = new RuleFunctionType("le", 2, R.AT.NUMBER | R.AT.DATETIME, R.AT.BOOL);
R.FT.GE = new RuleFunctionType("ge", 2, R.AT.NUMBER | R.AT.DATETIME, R.AT.BOOL);

R.FT.NOVALUE_2 = new RuleFunctionType("noValue", 2, R.AT.URI, R.AT.BOOL);
R.FT.NOVALUE_3 = new RuleFunctionType("noValue", 3, [R.AT.URI,R.AT.URI,R.AT.ANY], R.AT.BOOL);

R.FT.SUM = new RuleFunctionType("sum", 2, R.AT.NUMBER, R.AT.NUMBER);
R.FT.DIFFERENCE = new RuleFunctionType("difference", 2, R.AT.NUMBER, R.AT.NUMBER);
R.FT.MIN = new RuleFunctionType("min", 2, R.AT.NUMBER, R.AT.NUMBER);
R.FT.MAX = new RuleFunctionType("max", 2, R.AT.NUMBER, R.AT.NUMBER);
R.FT.PRODUCT = new RuleFunctionType("product", 2, R.AT.NUMBER, R.AT.NUMBER);
R.FT.QUOTIENT = new RuleFunctionType("quotient", 2, R.AT.NUMBER, R.AT.NUMBER);

R.FT.STRCONCAT = new RuleFunctionType("strConcat", -1, R.AT.STRING, R.AT.STRING);
R.FT.URICONCAT = new RuleFunctionType("uriConcat", -1, R.AT.URI, R.AT.URI);

R.FT.NOW = new RuleFunctionType("now", 0, null, R.AT.DATETIME);

