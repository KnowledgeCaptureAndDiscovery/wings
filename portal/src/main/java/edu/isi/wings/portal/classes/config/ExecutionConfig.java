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

  public HashMap<String, ExecutionEngine> engines;

  public ExecutionConfig(PropertyListConfiguration serverConfig) {
    this.engines = new HashMap<String, ExecutionEngine>();
    List<HierarchicalConfiguration> engineNodes = serverConfig.configurationsAt(
      "execution.engine"
    );
    for (HierarchicalConfiguration engineNode : engineNodes) {
      ExecutionEngine engine = this.getExeEngine(engineNode);
      this.engines.put(engine.getName(), engine);
    }
  }

  public ExecutionConfig() {
    this.engines = new HashMap<String, ExecutionEngine>();
    ExecutionEngine defaultLocal = new ExecutionEngine(
      "Local",
      LocalExecutionEngine.class.getCanonicalName(),
      ExecutionEngine.Type.BOTH
    );
    ExecutionEngine defaultDistrubited = new ExecutionEngine(
      "Distributed",
      DistributedExecutionEngine.class.getCanonicalName(),
      ExecutionEngine.Type.BOTH
    );

    this.engines.put(defaultLocal.getName(), defaultLocal);
    this.engines.put(defaultDistrubited.getName(), defaultDistrubited);
  }

  @SuppressWarnings("rawtypes")
  private ExecutionEngine getExeEngine(HierarchicalConfiguration node) {
    String name = node.getString("name");
    String impl = node.getString("implementation");
    ExecutionEngine.Type type = ExecutionEngine.Type.valueOf(
      node.getString("type")
    );
    ExecutionEngine engine = new ExecutionEngine(name, impl, type);
    for (Iterator it = node.getKeys("properties"); it.hasNext();) {
      String key = (String) it.next();
      String value = node.getString(key);
      engine.addProperty(key.replace("properties.", ""), value);
    }
    return engine;
  }

  public void addDefaultEngineConfig(PropertyListConfiguration config) {
    // loop engines and add them to config
    for (Entry<String, ExecutionEngine> entryEngine : this.engines.entrySet()) {
      ExecutionEngine engine = entryEngine.getValue();
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

  public HashMap<String, ExecutionEngine> getEngines() {
    return engines;
  }

  public Set<String> getEnginesList() {
    return this.engines.keySet();
  }
}
