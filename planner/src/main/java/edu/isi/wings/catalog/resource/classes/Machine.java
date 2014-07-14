package edu.isi.wings.catalog.resource.classes;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.telecontrol.ssh.RemoteNodeTypeHandler;
import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;

/*import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;*/

public class Machine extends Resource {
  private static final long serialVersionUID = 5211295601774494163L;

  private String hostIP;
  private String hostName;
  private String userId;
  private String userKey;
  private float memoryGB;
  private float storageGB;
  private boolean is64Bit;
  private boolean isHealthy;
  
  private String storageFolder;
  private String executionFolder;
  
  private ArrayList<EnvironmentValue> environmentValues;
  private ArrayList<String> softwareIds;
  private String osid;
  
  public Machine(String id) {
    super(id);
    environmentValues = new ArrayList<EnvironmentValue>();
    softwareIds = new ArrayList<String>();
  }

  public String getHostIP() {
    return hostIP;
  }

  public void setHostIP(String hostIP) {
    this.hostIP = hostIP;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserKey() {
    return userKey;
  }

  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public float getMemoryGB() {
    return memoryGB;
  }

  public void setMemoryGB(float memoryGB) {
    this.memoryGB = memoryGB;
  }

  public float getStorageGB() {
    return storageGB;
  }

  public void setStorageGB(float storageGB) {
    this.storageGB = storageGB;
  }

  public boolean is64Bit() {
    return is64Bit;
  }

  public void setIs64Bit(boolean is64Bit) {
    this.is64Bit = is64Bit;
  }

  public boolean isHealthy() {
    return isHealthy;
  }

  public void setHealthy(boolean isHealthy) {
    this.isHealthy = isHealthy;
  }

  public String getStorageFolder() {
    return storageFolder;
  }

  public void setStorageFolder(String storageFolder) {
    this.storageFolder = storageFolder;
  }

  public String getExecutionFolder() {
    return executionFolder;
  }

  public void setExecutionFolder(String executionFolder) {
    this.executionFolder = executionFolder;
  }

  public ArrayList<EnvironmentValue> getEnvironmentValues() {
    return environmentValues;
  }

  public void setEnvironmentValues(ArrayList<EnvironmentValue> environmentValues) {
    this.environmentValues = environmentValues;
  }
  
  public void addEnvironmentValues(EnvironmentValue environmentValue) {
    this.environmentValues.add(environmentValue);
  }
  
  public String getEnvironmentValue(String variable) {
    for(EnvironmentValue evalue : this.environmentValues) {
      if(evalue.getVariable().equals(variable))
        return evalue.getValue();
    }
    return null;
  }

  public ArrayList<String> getSoftwareIds() {
    return softwareIds;
  }

  public void setSoftwares(ArrayList<String> softwareIds) {
    this.softwareIds = softwareIds;
  }

  public void addSoftware(String softwareId) {
    this.softwareIds.add(softwareId);
  }
  
  public String getOSid() {
    return osid;
  }

  public void setOSid(String osid) {
    this.osid = osid;
  }

  public MachineDetails getMachineDetails() {
    MachineDetails details = new MachineDetails();
    try {      
      String name = this.getName();
      Cloud cloud = CloudFactory.createCloud();
      cloud.node(name).setConfigElement(ViConf.TYPE_HANDLER + "remote", 
          new RemoteNodeTypeHandler());
      ViNode node = cloud.node(name);
      ViProps.at(node).setRemoteType();
      node.setProp(SshSpiConf.SPI_SSH_TARGET_HOST, hostName);
      node.setProp(SshSpiConf.SPI_SSH_TARGET_ACCOUNT, userId);
      node.setProp(SshSpiConf.SPI_SSH_PRIVATE_KEY_FILE, userKey);
      node.setProp(SshSpiConf.SPI_JAR_CACHE, storageFolder + "/nanocloud");
      String jhome = this.getEnvironmentValue("JAVA_HOME");
      String javaexec = "java";
      if(jhome != null && !jhome.equals(""))
        javaexec = jhome + "/bin/java";
      node.setProp(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC, javaexec);
      node.touch();
      details = node.exec(new MachineDetailsGrabber(this));
      cloud.shutdown();
    }
    catch (Exception e) {
      details.setCanConnect(false);
      details.addError(e.getMessage());
      //e.printStackTrace();
    }
    return details;
    /*JSch ssh = new JSch();
    try {
      if (this.getUserKey() != null)
        ssh.addIdentity(this.getUserKey());
      Session ssh_session;
      if(this.getUserId() == null)
        ssh_session = ssh.getSession(this.getHostName());
      else
        ssh_session = ssh.getSession(this.getUserId(), this.getHostName());
      java.util.Properties config = new java.util.Properties(); 
      config.put("StrictHostKeyChecking", "no");
      ssh_session.setConfig(config);
      
      ssh_session.connect();
      if(ssh_session.isConnected()) {
        ChannelExec channel = (ChannelExec) ssh_session.openChannel("exec");
        channel.setCommand("ls "+ executionFolder);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        InputStream in=channel.getInputStream();
        channel.connect();
        byte[] tmp=new byte[1024];
        while(true){
          while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            if(i<0)break;
            System.out.print(new String(tmp, 0, i));
          }
          if(channel.isClosed()){
            if(in.available()>0) continue; 
            System.out.println("exit-status: "+channel.getExitStatus());
            break;
          }
        }
        channel.disconnect();
        ssh_session.disconnect();
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;*/
  }
}

class MachineDetailsGrabber implements Callable<MachineDetails>, Serializable {
  private static final long serialVersionUID = 5960512182954001309L;
  private final Machine machine;

  public MachineDetailsGrabber(Machine machine) {
    this.machine = machine;
  }

  @SuppressWarnings("restriction")
  @Override
  public MachineDetails call() {
    MachineDetails details = new MachineDetails();
    details.setCanConnect(true);;
    File f = new File(machine.getStorageFolder());
    if (!f.exists())
      details.addError("Cannot find Wings Storage Folder: "
          + machine.getStorageFolder());
    if (!f.canWrite())
      details.addError("Cannot write to Wings Storage Folder: "
          + machine.getStorageFolder());

    f = new File(machine.getExecutionFolder());
    if (!f.exists())
      details.addError("Cannot find Wings Execution Folder: "
          + machine.getExecutionFolder());
    if (!f.canWrite())
      details.addError("Cannot write to Wings Execution Folder: "
          + machine.getExecutionFolder());
    
    details.setNumCores(Runtime.getRuntime().availableProcessors());
    details.setMaxMemory(((com.sun.management.OperatingSystemMXBean) 
        ManagementFactory.getOperatingSystemMXBean())
          .getTotalPhysicalMemorySize());
    details.setFreeMemory(((com.sun.management.OperatingSystemMXBean) 
        ManagementFactory.getOperatingSystemMXBean())
          .getFreePhysicalMemorySize());
    File[] roots = File.listRoots();
    for (File root : roots) {
      details.setStorageRoot(root.getAbsolutePath());
      details.setTotalStorage(root.getTotalSpace());
      details.setFreeStorage(root.getFreeSpace());
      break;
    }

    details.setArchitecture(
        ManagementFactory.getOperatingSystemMXBean().getName() + " - "+
        ManagementFactory.getOperatingSystemMXBean().getArch());
    details.setSystemLoad(
        ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
    return details;
  }
}

class MachineDetails implements Serializable {
  private static final long serialVersionUID = -2690736677192673940L;
  private boolean connect;
  private float memoryMax;
  private float memoryFree;
  private int numCores;
  private String storageRoot;
  private float storageRootMax;
  private float storageRootFree;
  private String systemArch;
  private double systemLoad;
  private ArrayList<String> errors;

  public MachineDetails() {
    errors = new ArrayList<String>();
  }
  
  public boolean isCanConnect() {
    return connect;
  }

  public void setCanConnect(boolean canConnect) {
    this.connect = canConnect;
  }

  public float maxMemory() {
    return memoryMax;
  }

  public void setMaxMemory(float memoryMax) {
    this.memoryMax = memoryMax;
  }

  public float getFreeMemory() {
    return memoryFree;
  }

  public void setFreeMemory(float memoryFree) {
    this.memoryFree = memoryFree;
  }

  public int getNumCores() {
    return numCores;
  }

  public void setNumCores(int numCores) {
    this.numCores = numCores;
  }

  public String getStorageRoot() {
    return storageRoot;
  }

  public void setStorageRoot(String storageRoot) {
    this.storageRoot = storageRoot;
  }

  public float getTotalStorage() {
    return storageRootMax;
  }

  public void setTotalStorage(float totalStorage) {
    this.storageRootMax = totalStorage;
  }

  public float getFreeStorage() {
    return storageRootFree;
  }

  public void setFreeStorage(float freeStorage) {
    this.storageRootFree = freeStorage;
  }

  public String getArchitecture() {
    return systemArch;
  }

  public void setArchitecture(String architecture) {
    this.systemArch = architecture;
  }

  public double getSystemLoad() {
    return systemLoad;
  }

  public void setSystemLoad(double systemLoad) {
    this.systemLoad = systemLoad;
  }

  public ArrayList<String> getErrors() {
    return errors;
  }

  public void addError(String error) {
    this.errors.add(error);
  }
}