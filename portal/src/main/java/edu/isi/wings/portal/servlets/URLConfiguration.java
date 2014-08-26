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
