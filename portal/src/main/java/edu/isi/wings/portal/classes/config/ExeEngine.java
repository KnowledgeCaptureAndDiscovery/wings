package edu.isi.wings.portal.classes.config;

import java.util.Properties;

/**
 * Created by varun on 13/07/2015.
 */
public class ExeEngine {
    public static enum Type {PLAN, STEP, BOTH}

    ;
    ExeEngine.Type type;
    String name;
    String implementation;
    Properties props;

    public ExeEngine(String name, String implementation, ExeEngine.Type type) {
        this.type = type;
        this.name = name;
        this.implementation = implementation;
        props = new Properties();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExeEngine.Type getType() {
        return type;
    }

    public void setType(ExeEngine.Type type) {
        this.type = type;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public Properties getProperties() {
        return props;
    }

    public void setProperties(Properties props) {
        this.props = props;
    }

    public void addProperty(String key, String value) {
        this.props.setProperty(key, value);
    }
}
