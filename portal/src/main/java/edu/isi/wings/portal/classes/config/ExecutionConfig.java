package edu.isi.wings.portal.classes.config;

import edu.isi.wings.execution.engine.api.impl.distributed.DistributedExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class ExecutionConfig {

  public HashMap<String, ExecutionEngineConfig> engines;

  public ExecutionConfig(PropertyListConfiguration serverConfig) {
    this.engines = new HashMap<String, ExecutionEngineConfig>();
    if (serverConfig.containsKey("execution.engine")) {
      List<HierarchicalConfiguration> engineNodes =
        serverConfig.configurationsAt("execution.engine");
      for (HierarchicalConfiguration engineNode : engineNodes) {
        ExecutionEngineConfig engine = this.getExeEngine(engineNode);
        this.engines.put(engine.getName(), engine);
      }
    } else {
      ExecutionEngineConfig defaultLocal = new ExecutionEngineConfig(
        "Local",
        LocalExecutionEngine.class.getCanonicalName(),
        ExecutionEngineConfig.Type.BOTH
      );

      this.engines.put(defaultLocal.getName(), defaultLocal);
    }
  }

  @SuppressWarnings("rawtypes")
  private ExecutionEngineConfig getExeEngine(HierarchicalConfiguration node) {
    String name = node.getString("name");
    String impl = node.getString("implementation");
    ExecutionEngineConfig.Type type = ExecutionEngineConfig.Type.valueOf(
      node.getString("type")
    );
    ExecutionEngineConfig engine = new ExecutionEngineConfig(name, impl, type);
    for (Iterator it = node.getKeys("properties"); it.hasNext();) {
      String key = (String) it.next();
      String value = node.getString(key);
      engine.addProperty(key.replace("properties.", ""), value);
    }
    return engine;
  }

  public void addDefaultEngineConfig(PropertyListConfiguration config) {
    // loop engines and add them to config
    for (Entry<
      String,
      ExecutionEngineConfig
    > entryEngine : this.engines.entrySet()) {
      ExecutionEngineConfig engine = entryEngine.getValue();
      config.addProperty("execution.engine(-1).name", engine.getName());
      config.addProperty(
        "execution.engine.implementation",
        engine.getImplementation()
      );
      config.addProperty("execution.engine.type", engine.getType());
      for (Entry<Object, Object> entryProperty : engine
        .getProperties()
        .entrySet()) {
        config.addProperty(
          "execution.engine.properties." + entryProperty.getKey(),
          entryProperty.getValue()
        );
      }
    }
  }

  public HashMap<String, ExecutionEngineConfig> getEngines() {
    return engines;
  }

  public Set<String> getEnginesList() {
    return this.engines.keySet();
  }
}
