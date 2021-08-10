/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.utils;

import be.spacebel.metricsclient.entities.ReportFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.LoggerFactory;

/**
 * FTP client utilities
 * @author mng
 */
public class FtpClient {

    private final String serverName;
    private final int servePort;
    private final String username;
    private final String password;
    private final String reportDir;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FtpClient.class);

    public FtpClient(String server, int port, String username, String password, String reportDir) {
        this.serverName = server;
        this.servePort = port;
        this.username = username;
        this.password = password;
        if (reportDir != null) {
            this.reportDir = Utils.trimSlashs(reportDir);
        } else {
            this.reportDir = StringUtils.EMPTY;            
        }

    }

    public void listMissingFiles(List<ReportFile> toBeGeneratedFiles) throws IOException {
        LOG.debug("List all missing files on FTP directory " + reportDir);

        FTPClient client = connect();

        createReportDirectories(client);

        FTPFile[] files = client.listFiles("/" + reportDir);
        for (FTPFile file : files) {
            //LOG.debug("File name " + file.getName());
            findAndRemoveFile(toBeGeneratedFiles, file.getName());
        }

        disconnect(client);
    }

    public void upload(List<ReportFile> localReportFiles, String localDir) throws IOException {
        LOG.debug("Uploading local files to FTP server ");
        FTPClient client = connect();

        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);

        localReportFiles.forEach((reportFile) -> {
            String localFilePath = localDir + "/" + reportFile.getName();
            File localFile = new File(localFilePath);
            String remoteFilePath = reportDir + "/" + reportFile.getName();

            if (localFile.exists()) {
                try (FileInputStream inputStream = new FileInputStream(localFile)) {
                    boolean done = client.storeFile(remoteFilePath, inputStream);
                    if (done) {
                        LOG.debug("Uploaded file " + remoteFilePath);
                    }
                } catch (IOException e) {
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    LOG.debug("Error while uploading report files to FTP server " + e);
                }

                try {
                    Files.delete(Paths.get(localFilePath));
                    LOG.debug("Deleted local file " + localFilePath);
                } catch (IOException ex) {
                    LOG.debug("Error while deleting file " + localFilePath + ": " + ex);
                }
            } else {
                LOG.debug("Local report file " + localFilePath + " doesn't exist");
            }
        });

        disconnect(client);
    }

    private void findAndRemoveFile(List<ReportFile> reportFiles, String fileName) {
        for (ReportFile file : reportFiles) {
            if (file.getName().equals(fileName)) {
                reportFiles.remove(file);
                break;
            }
        }
    }

    private FTPClient connect() throws IOException {
        FTPClient client = new FTPClient();

        client.connect(serverName, servePort);
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }
        client.login(username, password);
        return client;
    }

    private void disconnect(FTPClient client) throws IOException {
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    private void createReportDirectories(FTPClient ftpClient)
            throws IOException {
        if (reportDir != null && !reportDir.isEmpty() && !reportDir.equals("/")) {
            String[] pathElements = reportDir.trim().split("/");
            if (pathElements != null && pathElements.length > 0) {
                for (String singleDir : pathElements) {
                    boolean existed = ftpClient.changeWorkingDirectory(singleDir);
                    if (!existed) {
                        boolean created = ftpClient.makeDirectory(singleDir);
                        if (created) {
                            LOG.debug("Created directory " + singleDir);
                            ftpClient.changeWorkingDirectory(singleDir);
                        } else {
                            throw new IOException("Could not create directory " + singleDir);
                        }
                    }
                }
            }
        }
    }
}
