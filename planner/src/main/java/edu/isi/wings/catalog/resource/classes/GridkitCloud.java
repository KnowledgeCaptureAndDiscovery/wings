package edu.isi.wings.catalog.resource.classes;

import java.util.HashMap;

import org.gridkit.internal.com.jcraft.jsch.ChannelSftp;
import org.gridkit.internal.com.jcraft.jsch.JSch;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.SimpleCloudFactory;
import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;

public class GridkitCloud {
  
  private static Cloud cloud = 
      SimpleCloudFactory.createSimpleSshCloud();
  
  public static ViNode getNode(Machine m) throws Exception {
    String host = m.getHostString();
    for(ViNode node : cloud.listNodes(host))
      return node;

    ViNode node = cloud.node(host);
    String jhome = m.getEnvironmentValue("JAVA_HOME");
    String javaexec = "java";
    if(jhome != null && !jhome.equals(""))
      javaexec = jhome + "/bin/java";
    
    node.setProp(ViConf.REMOTE_HOST, host);
    node.setProp(ViConf.REMOTE_ACCOUNT, m.getUserId());
    node.setProp(SshSpiConf.SPI_SSH_PRIVATE_KEY_FILE, m.getUserKey());
    node.setProp(SshSpiConf.SPI_JAR_CACHE, m.getStorageFolder() + "/nanocloud");
    node.setProp(ViConf.JVM_EXEC_CMD, javaexec);
    node.touch();
    return node;
  }
  
  public static void shutdown() {
    cloud.shutdown();
  }
  
  public static void resetNode(Machine m) {
    if(m.getHostString() != null ) {
      for(ViNode node : cloud.listNodes(m.getHostString()))
        node.shutdown();
    }
    ((ViManager)cloud).resetDeadNode();
  }
  
  private static Session getSSHSession(Machine m) 
      throws JSchException {
    JSch ssh = new JSch();
    if (m.getUserKey() != null)
      ssh.addIdentity(m.getUserKey());
    Session ssh_session = ssh.getSession(m.getUserId(), m.getHostName());
    java.util.Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    ssh_session.setConfig(config);
    ssh_session.connect();
    return ssh_session;
  }
  
  public static boolean uploadFiles(Machine m, 
      HashMap<String, String> localRemoteMap) {
    try {
      Session ssh_session = getSSHSession(m);
      if(ssh_session.isConnected()) {
        ChannelSftp sftpChannel = (ChannelSftp) ssh_session.openChannel("sftp");
        sftpChannel.connect();
        for(String local: localRemoteMap.keySet()) {
          String remote = localRemoteMap.get(local);
          sftpChannel.put(local, remote);
        }
        sftpChannel.disconnect();
        ssh_session.disconnect();
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public static boolean downloadFiles(Machine m,
      HashMap<String, String> localRemoteMap) {
    try {
      Session ssh_session = getSSHSession(m);
      if(ssh_session.isConnected()) {
        ChannelSftp sftpChannel = (ChannelSftp) ssh_session.openChannel("sftp");
        sftpChannel.connect();
        for(String local: localRemoteMap.keySet()) {
          String remote = localRemoteMap.get(local);
          try {
            sftpChannel.get(remote, local);
          }
          catch (Exception e) {
            // Ignore
          }
        }
        sftpChannel.disconnect();
        ssh_session.disconnect();
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
}
