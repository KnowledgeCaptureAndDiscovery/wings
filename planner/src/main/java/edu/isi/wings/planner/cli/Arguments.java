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

package edu.isi.wings.planner.cli;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.util.HashMap;

public class Arguments {

  public static void displayUsage(String program) {
    System.out.println(
      "usage: " + program + " [options] [seed|template options]"
    );
    System.out.println();
    System.out.println("options:");
    System.out.println(" -h, --help Show this help message (default)");
    System.out.println(
      " -c, --conf=<file> Specify the Wings properties file path"
    );
    System.out.println(
      " -l, --logdir=<directory> Specify the directory to store logs"
    );
    System.out.println(
      " -L, --libname=<name> Specify the Concrete Components Library to use"
    );
    System.out.println(
      " -o, --outputdir=<directory> Specify the directory to store daxes"
    );
    System.out.println(
      " -O, --ontdir=<directory> Specify the ontologies directory"
    );
    System.out.println(" -r, --requestid=<id> Specify the Request ID to use");
    System.out.println(
      " -D, --getData=<file> Select appropriate data and store in <file>"
    );
    System.out.println(
      " -P, --getParameters=<file> Get appropriate parameters and store in <file>"
    );
    System.out.println(
      " -E, --elaborate=<file> Elaborate given template and store in <file>"
    );
    System.out.println(
      " -V, --validate=<file> Validate given template and store rdf in <file>"
    );
    System.out.println(
      " -T, --trim=<n> Trim the search space to return 'n' or less daxes"
    );
    System.out.println();
    System.out.println("seed|template options:");
    System.out.println(" -s, --seed=<name> Specify the Seed name");
    System.out.println(" -t, --template=<name> Specify the Template name");
    System.out.println();
  }

  public static HashMap<String, String> getOptions(
    String program,
    String[] args
  ) {
    // Parse Arguments for SeedFile and TemplateID(optional)
    if (args.length == 0) {
      displayUsage(program);
      return null;
    }

    String sopts = "hc:l:o:O:d:i:r:L:D:P:E:V:T:s:t:";

    LongOpt[] lopts = {
      new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
      new LongOpt("conf", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
      new LongOpt("logdir", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
      new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
      new LongOpt("ontdir", LongOpt.REQUIRED_ARGUMENT, null, 'O'),
      new LongOpt("requestid", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
      new LongOpt("libname", LongOpt.REQUIRED_ARGUMENT, null, 'L'),
      new LongOpt("getData", LongOpt.REQUIRED_ARGUMENT, null, 'D'),
      new LongOpt("getParameters", LongOpt.REQUIRED_ARGUMENT, null, 'P'),
      new LongOpt("elaborate", LongOpt.REQUIRED_ARGUMENT, null, 'E'),
      new LongOpt("validate", LongOpt.REQUIRED_ARGUMENT, null, 'V'),
      new LongOpt("trim", LongOpt.REQUIRED_ARGUMENT, null, 'T'),
      new LongOpt("seed", LongOpt.REQUIRED_ARGUMENT, null, 's'),
      new LongOpt("template", LongOpt.REQUIRED_ARGUMENT, null, 't'),
    };

    int code;
    HashMap<String, String> options = new HashMap<String, String>();

    Getopt g = new Getopt(program, args, sopts, lopts);
    while ((code = g.getopt()) != -1) {
      switch (code) {
        case '?':
          displayUsage(program);
          return null;
        case 'h':
          displayUsage(program);
          return null;
        case 'T':
          String trimnum = g.getOptarg();
          boolean ok = true;
          try {
            if (Integer.parseInt(trimnum) == 0) ok = false;
          } catch (NumberFormatException e) {
            ok = false;
          }
          if (!ok) {
            System.err.println("-T or --trim takes a non-zero number argument");
            displayUsage(program);
            return null;
          }
          options.put("trim", trimnum);
          break;
        case 'r':
          options.put("requestid", g.getOptarg());
          break;
        case 'L':
          options.put("libname", g.getOptarg());
          break;
        case 'c':
          options.put("conf", g.getOptarg());
          break;
        case 's':
          options.put("seed", g.getOptarg());
          break;
        case 't':
          options.put("template", g.getOptarg());
          break;
        case 'l':
          options.put("logdir", g.getOptarg());
          break;
        case 'o':
          options.put("outputdir", g.getOptarg());
          break;
        case 'O':
          options.put("ontdir", g.getOptarg());
          break;
        case 'D':
          options.put("getData", g.getOptarg());
          break;
        case 'P':
          options.put("getParameters", g.getOptarg());
          break;
        case 'V':
          options.put("validate", g.getOptarg());
          break;
        case 'E':
          options.put("elaborate", g.getOptarg());
          break;
        default:
          displayUsage(program);
          return null;
      }
    }
    return options;
  }
}
