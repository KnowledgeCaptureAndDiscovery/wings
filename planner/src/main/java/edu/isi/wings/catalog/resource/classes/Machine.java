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

  public boolean checkConnection() {
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
      String r = node.exec(new CheckMachine(this));
      System.out.println(r);
      cloud.shutdown();
    }
    catch (Exception e) {
      if(e instanceof java.lang.RuntimeException)
        System.out.println ("Default java not OK. Set JAVA_HOME");
      e.printStackTrace();
      return false;
    }
    return true;
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

class CheckMachine implements Callable<String>, Serializable {
  private static final long serialVersionUID = 5960512182954001309L;
  private final Machine machine;

  public CheckMachine(Machine machine) {
    this.machine = machine;
  }

  @Override
  public String call() {
    System.out.println("Connected");
    File f = new File(machine.getStorageFolder());
    if (!f.exists())
      System.out.println("Cannot find Wings Storage Folder: "
          + machine.getStorageFolder());
    if (!f.canWrite())
      System.out.println("Cannot write to Wings Storage Folder: "
          + machine.getStorageFolder());

    f = new File(machine.getExecutionFolder());
    if (!f.exists())
      System.out.println("Cannot find Wings Execution Folder: "
          + machine.getExecutionFolder());
    if (!f.canWrite())
      System.out.println("Cannot write to Wings Execution Folder: "
          + machine.getExecutionFolder());
    
    /* Total number of processors or cores available to the JVM */
    System.out.println("Available processors (cores): " + 
        Runtime.getRuntime().availableProcessors());

    /* Total amount of free memory available to the JVM */
    System.out.println("Free memory (bytes): " + 
        Runtime.getRuntime().freeMemory());

    /* This will return Long.MAX_VALUE if there is no preset limit */
    long maxMemory = Runtime.getRuntime().maxMemory();
    /* Maximum amount of memory the JVM will attempt to use */
    System.out.println("Maximum memory (bytes): " + 
        (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

    /* Total memory currently available to the JVM */
    System.out.println("Total memory available to JVM (bytes): " + 
        Runtime.getRuntime().totalMemory());

    /* Get a list of all filesystem roots on this system */
    File[] roots = File.listRoots();

    /* For each filesystem root, print some info */
    for (File root : roots) {
      System.out.println("File system root: " + root.getAbsolutePath());
      System.out.println("Total space (bytes): " + root.getTotalSpace());
      System.out.println("Free space (bytes): " + root.getFreeSpace());
      System.out.println("Usable space (bytes): " + root.getUsableSpace());
    }

    System.out.println("Architecture: "+
        ManagementFactory.getOperatingSystemMXBean().getArch());
    System.out.println("Load average: "+
        ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
    return "ping";
  }
}