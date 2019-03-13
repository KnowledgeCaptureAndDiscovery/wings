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

package edu.isi.wings.opmm;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.request.body.multipart.InputStreamPart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.asynchttpclient.Dsl.basicAuthRealm;

public class StorageHandler {

    public static void zipAndStream(File dir, ZipOutputStream zos, String prefix)
            throws Exception {
        byte bytes[] = new byte[2048];
        for (File file : dir.listFiles()) {
            if(file.isDirectory())
                StorageHandler.zipAndStream(file, zos, prefix + file.getName() + "/" );
            else {
                FileInputStream fis = new FileInputStream(file.getAbsolutePath());
                BufferedInputStream bis = new BufferedInputStream(fis);
                zos.putNextEntry(new ZipEntry(prefix + file.getName()));
                int bytesRead;
                while ((bytesRead = bis.read(bytes)) != -1) {
                    zos.write(bytes, 0, bytesRead);
                }
                zos.closeEntry();
                bis.close();
                fis.close();
            }
        }
    }

    public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static String zipStreamUpload(File dir, String server, String username, String password)
            throws Exception {

        File _tmpZip = File.createTempFile(dir.getName(), "");
        FileOutputStream fos = new FileOutputStream(_tmpZip);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        zipFile(dir, _tmpZip.getName(), zipOut);

        String remoteURL = uploadFile(_tmpZip, server, username, password);
        zipOut.close();
        fos.close();

        FileUtils.deleteQuietly(_tmpZip);

        return remoteURL;
    }

    public static String uploadFile(File datafile, String upUrl, String username, String password) throws Exception {
        if (username == null || password == null){
            return "missing username or password " + upUrl + " " + username + " " + password;
        }
        if(datafile.exists()) {
            AsyncHttpClient client = Dsl.asyncHttpClient();
            InputStream inputStream;
            inputStream = new BufferedInputStream(new FileInputStream(datafile));
            try {
                org.asynchttpclient.Response response = client.preparePost(upUrl)
                        .setRealm(basicAuthRealm(username, password).setUsePreemptiveAuth(true))
                        .addBodyPart(new
                                InputStreamPart(
                                datafile.getName(), inputStream, datafile.getName(), -1,
                                "application/octet-stream", UTF_8)
                        ).execute().get();
                String returnURL = response.getResponseBody().replace("\n", "");
                return returnURL;
            } catch (Exception e) {
                return null;
            }
            finally {
                inputStream.close();
            }

            }
        return null;
    }
}

class ZipStreamer extends Thread {
    public PipedInputStream pis;
    public OutputStream os;

    public ZipStreamer(PipedInputStream pis, OutputStream os) {
        super();
        this.pis = pis;
        this.os = os;
    }

    public void run() {
        try {
            IOUtils.copyLarge(pis, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

