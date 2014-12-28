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

package edu.isi.wings.portal.servlets;

import javax.servlet.ServletContext;

import org.ocpsoft.rewrite.annotation.RewriteConfiguration;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Direction;
import org.ocpsoft.rewrite.servlet.config.Forward;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.Path;

@RewriteConfiguration
public class URLConfiguration extends HttpConfigurationProvider
{
   @Override
   public int priority()
   {
     return 10;
   }

   @Override
   public Configuration getConfiguration(final ServletContext context)
   {
     ConfigurationBuilder config = ConfigurationBuilder.begin();
     String[] scripts = new String[] {"data", "components", "workflows", "executions", 
         "plan", "upload"};
     for(String script: scripts) {
       config.addRule()
         .when(Direction.isInbound().and(Path.matches("/users/{owner}/"+script)))
         .perform(Forward.to("/"+script+"?userid={owner}"));
       config.addRule()
           .when(Direction.isInbound().and(Path.matches("/users/{owner}/{domain}/"+script)))
           .perform(Forward.to("/"+script+"?userid={owner}&domainid={domain}"));
       config.addRule()
           .when(Direction.isInbound().and(Path.matches("/users/{owner}/{domain}/"+script+"/{op}")))
           .perform(Forward.to("/"+script+"/{op}?userid={owner}&domainid={domain}"));
       config.addRule()
         .when(Direction.isInbound().and(Path.matches("/users/{owner}/{domain}/"+script+"/{ext}/{op}")))
         .perform(Forward.to("/"+script+"/{ext}/{op}?userid={owner}&domainid={domain}"));
     }
     config.addRule()
        .when(Direction.isInbound().and(Path.matches("/users/{owner}/domains")))
        .perform(Forward.to("/domains?userid={owner}"));
     config.addRule()
        .when(Direction.isInbound().and(Path.matches("/users/{owner}/domains/{op}")))
        .perform(Forward.to("/domains/{op}?userid={owner}"));
     return config;
   }
}
