package edu.isi.wings.portal.classes.config;

import edu.isi.wings.execution.engine.api.impl.distributed.DistributedExecutionEngine;
import edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class PortalConfig {

  public static String EXPORT_SERVLET_PATH = "/export";
  public static String ONT_DIR_URL = "http://www.wings-workflows.org/ontology";
  public static String USERS_RELATIVE_DIR = "users";
  private String COMMUNITY_RELATIVE_DIR = "common";

  public String configFile;
  public String storageDirectory;
  public String tdbDirectory;
  public String logsDirectory;
  public String dotFile;
  public String serverUrl;

  private String exportCommunityUrl;
  private String communityPath;
  private String communityDir;

  public String workflowOntologyUrl;
  public String dataOntologyUrl;
  public String componentOntologyUrl;
  public String executionOntologyUrl;
  public String resourceOntologyUrl = ONT_DIR_URL + "/resource.owl";
  public boolean deleteRunOutputs;
  public String contextRootPath;
  public HashMap<String, ExeEngine> engines;
  public Publisher publisher;
  public PlannerConfig plannerConfig = new PlannerConfig();
  public String clients;
  public boolean hasMetaWorkflows;
  public boolean sandboxed;

  public void initializePortalConfig(HttpServletRequest request) {
    this.contextRootPath = request.getContextPath();

    PropertyListConfiguration serverConfig = getPortalConfiguration(request);
    this.storageDirectory = serverConfig.getString("storage.local");
    this.tdbDirectory = serverConfig.getString("storage.tdb");
    this.serverUrl = serverConfig.getString("server");
    this.dotFile = serverConfig.getString("graphviz");
    this.clients = serverConfig.getString("clients");
    this.dataOntologyUrl = serverConfig.getString("ontology.data");
    this.componentOntologyUrl =
      serverConfig.containsKey("ontology.component")
        ? serverConfig.getString("ontology.component")
        : componentOntologyUrl;
    this.workflowOntologyUrl = serverConfig.getString("ontology.workflow");
    this.executionOntologyUrl = serverConfig.getString("ontology.execution");
    this.resourceOntologyUrl =
      serverConfig.containsKey("ontology.resource")
        ? serverConfig.getString("ontology.resource")
        : resourceOntologyUrl;
    if (serverConfig.containsKey("metaworkflows")) this.hasMetaWorkflows =
      serverConfig.getBoolean("metaworkflows");

    if (
      serverConfig.containsKey("light-reasoner")
    ) plannerConfig.dataValidation = !serverConfig.getBoolean("light-reasoner");

    if (
      serverConfig.containsKey("planner.data-validation")
    ) plannerConfig.dataValidation =
      serverConfig.getBoolean("planner.data-validation");
    if (
      serverConfig.containsKey("planner.specialization")
    ) plannerConfig.specialization =
      serverConfig.getBoolean("planner.specialization");
    if (serverConfig.containsKey("planner.use-rules")) plannerConfig.useRules =
      serverConfig.getBoolean("planner.use-rules");

    if (
      serverConfig.containsKey("planner.max-queue-size")
    ) plannerConfig.maxQueueSize =
      serverConfig.getInt(
        "planner.max-queue-size",
        1000
      ); else plannerConfig.maxQueueSize = 1000;
    if (
      serverConfig.containsKey("planner.parallelism")
    ) plannerConfig.parallelism =
      serverConfig.getInt(
        "planner.parallelism",
        10
      ); else plannerConfig.parallelism = 10;

    if (serverConfig.containsKey("storage.logs")) {
      this.logsDirectory = serverConfig.getString("storage.logs");
    } else {
      this.logsDirectory = this.storageDirectory + File.separator + "logs";
    }
    if (serverConfig.containsKey("storage.delete-run-outputs")) {
      this.deleteRunOutputs =
        serverConfig.getBoolean("storage.delete-run-outputs");
    } else {
      this.deleteRunOutputs = false;
    }
    // Create logsDir (if it doesn't exist)
    File logdir = new File(this.logsDirectory);
    if (!logdir.exists() && !logdir.mkdirs()) System.err.println(
      "Cannot create logs directory : " + logdir.getAbsolutePath()
    );

    this.exportCommunityUrl =
      serverUrl +
      contextRootPath +
      EXPORT_SERVLET_PATH +
      "/" +
      COMMUNITY_RELATIVE_DIR;
    this.communityPath =
      contextRootPath + "/" + USERS_RELATIVE_DIR + "/" + COMMUNITY_RELATIVE_DIR;

    this.communityDir =
      storageDirectory + File.separator + COMMUNITY_RELATIVE_DIR;
    // Create communityDir (if it doesn't exist)
    File uf = new File(this.communityDir);
    if (!uf.exists() && !uf.mkdirs()) System.err.println(
      "Cannot create community directory : " + uf.getAbsolutePath()
    );

    // Load engine configurations
    this.engines = new HashMap<String, ExeEngine>();
    List<HierarchicalConfiguration> enginenodes = serverConfig.configurationsAt(
      "execution.engine"
    );
    for (HierarchicalConfiguration enode : enginenodes) {
      ExeEngine engine = this.getExeEngine(enode);
      this.engines.put(engine.getName(), engine);
    }
    // Add in the distributed engine if it doesn't already exist
    if (!this.engines.containsKey("Distributed")) {
      ExeEngine distengine = new ExeEngine(
        "Distributed",
        DistributedExecutionEngine.class.getCanonicalName(),
        ExeEngine.Type.BOTH
      );
      this.engines.put(distengine.getName(), distengine);
      this.addEngineConfig(serverConfig, distengine);
      try {
        serverConfig.save(this.configFile);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Load publishing configuration
    String publishUrl = serverConfig.getString("publisher.url");
    String publishExportName = serverConfig.getString("publisher.name");
    String tstorePublishUrl = serverConfig.getString(
      "publisher.triple-store.publish"
    );
    String tstoreQueryUrl = serverConfig.getString(
      "publisher.triple-store.query"
    );
    String domainsDir = serverConfig.getString(
      "publisher.triple-store.domains-directory"
    );
    String uploadUrl = serverConfig.getString("publisher.upload-server.url");
    String uploadUsername = serverConfig.getString(
      "publisher.upload-server.username"
    );
    String uploadPassword = serverConfig.getString(
      "publisher.upload-server.password"
    );
    String uploadDir = serverConfig.getString(
      "publisher.upload-server.directory"
    );
    String uploadHost = serverConfig.getString("publisher.upload-server.host");
    String uploadUserId = serverConfig.getString(
      "publisher.upload-server.userid"
    );
    String uploadKey = serverConfig.getString(
      "publisher.upload-server.private-key"
    );
    String sizeString = serverConfig.getString(
      "publisher.upload-server.max-upload-size"
    );

    this.publisher = new Publisher();
    this.publisher.setUrl(publishUrl);
    this.publisher.setExportName(publishExportName);
    this.publisher.setDomainsDir(domainsDir);
    this.publisher.setTstorePublishUrl(tstorePublishUrl);
    this.publisher.setTstoreQueryUrl(tstoreQueryUrl);

    ServerDetails upserver = new ServerDetails();
    upserver.setUrl(uploadUrl);
    upserver.setUsername(uploadUsername);
    upserver.setPassword(uploadPassword);
    upserver.setHostUserId(uploadUserId);
    upserver.setDirectory(uploadDir);
    upserver.setHost(uploadHost);
    upserver.setPrivateKey(uploadKey);
    if (sizeString != null) {
      long size = this.getSizeFromString(sizeString);
      upserver.setMaxUploadSize(size);
    }
    this.publisher.setUploadServer(upserver);
  }

  private long getSizeFromString(String sizeString) {
    long kb = 1024;
    long mb = kb * kb;
    long gb = kb * mb;
    long tb = kb * gb;

    Pattern pat = Pattern.compile("(\\d+)\\s*([KkMmGgTt])[Bb]?");
    Matcher mat = pat.matcher(sizeString);
    if (mat.find()) {
      long size = Long.parseLong(mat.group(1));
      if (mat.groupCount() > 1) {
        String units = mat.group(2).toLowerCase();
        if (units.equals("k")) return size * kb;
        if (units.equals("m")) return size * mb;
        if (units.equals("g")) return size * gb;
        if (units.equals("t")) return size * tb;
      }
      return size;
    }
    return 0;
  }

  public PropertyListConfiguration getPortalConfiguration(
    HttpServletRequest request
  ) {
    ServletContext app = request.getSession().getServletContext();
    this.configFile = app.getInitParameter("config.file");
    if (this.configFile == null) {
      String home = System.getProperty("user.home");
      if (home != null && !home.equals("")) this.configFile =
        home +
        File.separator +
        ".wings" +
        File.separator +
        "portal.properties"; else this.configFile =
        "/etc/wings/portal.properties";
    }
    // Create configFile if it doesn't exist (portal.properties)
    File cfile = new File(this.configFile);
    if (!cfile.exists()) {
      File configDir = cfile.getParentFile();
      if (!configDir.exists() && !configDir.mkdirs()) {
        System.err.println(
          "Cannot create config file directory : " + configDir
        );
        return null;
      }
      if (request != null) createDefaultPortalConfig(request);
    }

    // Load properties from configFile
    PropertyListConfiguration props = new PropertyListConfiguration();
    try {
      props.load(this.configFile);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return props;
  }

  private void createDefaultPortalConfig(HttpServletRequest request) {
    String server =
      request.getScheme() +
      "://" +
      request.getServerName() +
      ":" +
      request.getServerPort();
    String storageDir = null;
    String home = System.getProperty("user.home");
    if (home != null && !home.equals("")) storageDir =
      home +
      File.separator +
      ".wings" +
      File.separator +
      "storage"; else storageDir =
      System.getProperty("java.io.tmpdir") +
      File.separator +
      "wings" +
      File.separator +
      "storage";

    File storageDirFile = new File(storageDir);
    if (
      !storageDirFile.exists() && !storageDirFile.mkdirs()
    ) System.err.println("Cannot create storage directory: " + storageDir);

    PropertyListConfiguration config = new PropertyListConfiguration();
    config.addProperty("storage.local", storageDir);
    config.addProperty("storage.tdb", storageDir + File.separator + "TDB");
    config.addProperty("server", server);

    File loc1 = new File("/usr/bin/dot");
    File loc2 = new File("/usr/local/bin/dot");
    config.addProperty(
      "graphviz",
      loc2.exists() ? loc2.getAbsolutePath() : loc1.getAbsolutePath()
    );
    config.addProperty("ontology.data", ONT_DIR_URL + "/data.owl");
    config.addProperty("ontology.component", ONT_DIR_URL + "/component.owl");
    config.addProperty("ontology.workflow", ONT_DIR_URL + "/workflow.owl");
    config.addProperty("ontology.execution", ONT_DIR_URL + "/execution.owl");
    config.addProperty("ontology.resource", ONT_DIR_URL + "/resource.owl");

    this.addEngineConfig(
        config,
        new ExeEngine(
          "Local",
          LocalExecutionEngine.class.getCanonicalName(),
          ExeEngine.Type.BOTH
        )
      );
    this.addEngineConfig(
        config,
        new ExeEngine(
          "Distributed",
          DistributedExecutionEngine.class.getCanonicalName(),
          ExeEngine.Type.BOTH
        )
      );

    /*
     * this.addEngineConfig(config, new ExeEngine("OODT",
     * OODTExecutionEngine.class.getCanonicalName(), ExeEngine.Type.PLAN));
     *
     * this.addEngineConfig(config, new ExeEngine("Pegasus",
     * PegasusExecutionEngine.class.getCanonicalName(), ExeEngine.Type.PLAN));
     */

    try {
      config.save(this.configFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("rawtypes")
  private ExeEngine getExeEngine(HierarchicalConfiguration node) {
    String name = node.getString("name");
    String impl = node.getString("implementation");
    ExeEngine.Type type = ExeEngine.Type.valueOf(node.getString("type"));
    ExeEngine engine = new ExeEngine(name, impl, type);
    for (Iterator it = node.getKeys("properties"); it.hasNext();) {
      String key = (String) it.next();
      String value = node.getString(key);
      engine.addProperty(key.replace("properties.", ""), value);
    }
    return engine;
  }

  private void addEngineConfig(
    PropertyListConfiguration config,
    ExeEngine engine
  ) {
    config.addProperty("execution.engine(-1).name", engine.getName());
    config.addProperty(
      "execution.engine.implementation",
      engine.getImplementation()
    );
    config.addProperty("execution.engine.type", engine.getType());
    for (Entry<Object, Object> entry : engine
      .getProperties()
      .entrySet()) config.addProperty(
      "execution.engine.properties." + entry.getKey(),
      entry.getValue()
    );
  }

  public String getConfigFile() {
    return configFile;
  }

  public String getStorageDirectory() {
    return storageDirectory;
  }

  public String getTdbDirectory() {
    return tdbDirectory;
  }

  public String getLogsDirectory() {
    return logsDirectory;
  }

  public String getDotFile() {
    return dotFile;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getWorkflowOntologyUrl() {
    return workflowOntologyUrl;
  }

  public String getDataOntologyUrl() {
    return dataOntologyUrl;
  }

  public String getComponentOntologyUrl() {
    return componentOntologyUrl;
  }

  public String getExecutionOntologyUrl() {
    return executionOntologyUrl;
  }

  public String getResourceOntologyUrl() {
    return resourceOntologyUrl;
  }

  public boolean isDeleteRunOutputs() {
    return deleteRunOutputs;
  }

  public String getContextRootPath() {
    return contextRootPath;
  }

  public HashMap<String, ExeEngine> getEngines() {
    return engines;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  public PlannerConfig getPlannerConfig() {
    return plannerConfig;
  }

  public String getClients() {
    return clients;
  }

  public boolean isHasMetaWorkflows() {
    return hasMetaWorkflows;
  }

  public boolean hasMetaWorkflows() {
    return hasMetaWorkflows;
  }

  public String getTripleStoreDir() {
    return tdbDirectory;
  }

  public String getExportCommunityUrl() {
    return exportCommunityUrl;
  }

  public String getCommunityPath() {
    return communityPath;
  }

  public String getCommunityDir() {
    return communityDir;
  }

  public boolean isSandboxed() {
    return sandboxed;
  }

  public Set<String> getEnginesList() {
    return this.engines.keySet();
  }
}
