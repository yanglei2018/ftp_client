package com.weather.ftp.utils;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * 实现从服务器下载程序到本地磁盘的工具类
 */
public class FTPTransfer {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    protected final Logger log = Logger.getLogger(getClass().getName());
    private String         host;
    private int            port;
    private String         username;
    private String         password;

    private boolean        binaryTransfer = true;
    private boolean        passiveMode    = true;
    private String         encoding       = "UTF-8";
    private int            clientTimeout  = 3000;
    private boolean flag=true;
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isBinaryTransfer() {
        return binaryTransfer;
    }

    public void setBinaryTransfer(boolean binaryTransfer) {
        this.binaryTransfer = binaryTransfer;
    }

    public boolean isPassiveMode() {
        return passiveMode;
    }

    public void setPassiveMode(boolean passiveMode) {
        this.passiveMode = passiveMode;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    /**
     * 返回一个FTPClient实例
     *
     * @throws ConnectException
     */
    private FTPClient getFTPClient() throws ConnectException {
        FTPClient ftpClient = new FTPClient(); //构造一个FtpClient实例
        ftpClient.setControlEncoding(encoding); //设置字符集

        connect(ftpClient); //连接到ftp服务器
        logger.info("ftp连接成功");
        //设置为passive模式
        if (passiveMode) {
            ftpClient.enterLocalPassiveMode();
        }
        setFileType(ftpClient); //设置文件传输类型

        try {
            ftpClient.setSoTimeout(clientTimeout);
        } catch (SocketException e) {
            throw new ConnectException("Set timeout error.");
        }

        return ftpClient;
    }

    /**
     * 设置文件传输类型
     *
     * @throws ConnectException
     * @throws IOException
     */
    private void setFileType(FTPClient ftpClient) throws ConnectException {
        try {
            if (binaryTransfer) {
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            } else {
                ftpClient.setFileType(FTPClient.ASCII_FILE_TYPE);
            }
        } catch (IOException e) {
            throw new ConnectException("Could not to set file type.");
        }
    }

    /**
     * 连接到ftp服务器
     *
     * @param ftpClient
     * @return 连接成功返回true，否则返回false
     * @throws ConnectException
     */
    public boolean connect(FTPClient ftpClient) throws ConnectException {
        try {
            ftpClient.connect(host, port);

            // 连接后检测返回码来校验连接是否成功
            int reply = ftpClient.getReplyCode();

            if (FTPReply.isPositiveCompletion(reply)) {
                //登陆到ftp服务器
                if (ftpClient.login(username, password)) {
                    setFileType(ftpClient);
                    return true;
                }
            } else {
                ftpClient.disconnect();
                throw new ConnectException("FTP server refused connection.");
            }
        } catch (IOException e) {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect(); //断开连接
                } catch (IOException e1) {
                    throw new ConnectException("Could not disconnect from server.");
                }

            }
            throw new ConnectException("Could not connect to server.");
        }
        return false;
    }

    /**
     * 断开ftp连接
     *
     * @throws ConnectException
     */
    private void disconnect(FTPClient ftpClient) throws ConnectException {
        try {
            ftpClient.logout();
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            throw new ConnectException("Could not disconnect from server.");
        }
    }

    //---------------------------------------------------------------------
    // public method
    //---------------------------------------------------------------------
    /**
     * 上传一个本地文件到远程指定文件
     *
     * @param serverFile 服务器端文件名(包括完整路径)
     * @param localFile 本地文件名(包括完整路径)
     * @return 成功时，返回true，失败返回false
     * @throws ConnectException
     */
    public boolean put(String serverFile, String localFile) throws ConnectException {
        return put(serverFile, localFile, false);
    }

    /**
     * 上传一个本地文件到远程指定文件
     *
     * @param serverFile 服务器端文件名(包括完整路径)
     * @param localFile 本地文件名(包括完整路径)
     * @param delFile 成功后是否删除文件
     * @return 成功时，返回true，失败返回false
     * @throws ConnectException
     */
    public boolean put(String serverFile, String localFile, boolean delFile) throws ConnectException {
        FTPClient ftpClient = null;
        InputStream input = null;
        try {
            ftpClient = getFTPClient();
            // 处理传输
            input = new FileInputStream(localFile);
            ftpClient.storeFile(serverFile, input);
            // log.debug("put " + localFile);
            input.close();
            if (delFile) {
                (new File(localFile)).delete();
            }
            // log.debug("delete " + localFile);
            return true;
        } catch (FileNotFoundException e) {
            throw new ConnectException("local file not found.");
        } catch (IOException e) {
            throw new ConnectException("Could not put file to server.");
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {
                throw new ConnectException("Couldn't close FileInputStream.");
            }
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
    }

    /**
     * 下载一个远程文件到本地的指定文件
     *
     * @param serverFile 服务器端文件名(包括完整路径)
     * @param localFile 本地文件名(包括完整路径)
     * @return 成功时，返回true，失败返回false
     * @throws ConnectException
     */
    public boolean get(String serverFile, String localFile) throws ConnectException {
        return get(serverFile, localFile, false);
    }

    /**
     * 下载一个远程文件到本地的指定文件
     *
     * @param serverFile 服务器端文件名(包括完整路径)
     * @param localFile 本地文件名(包括完整路径)
     * @return 成功时，返回true，失败返回false
     * @throws ConnectException
     */
    public boolean get(String serverFile, String localFile, boolean delFile) throws ConnectException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(localFile);
            return get(serverFile, output, delFile);
        } catch (FileNotFoundException e) {
            throw new ConnectException("local file not found.");
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                throw new ConnectException("Couldn't close FileOutputStream.");
            }
        }
    }

    /**
     * 下载一个远程文件到指定的流
     * 处理完后记得关闭流
     *
     * @param serverFile
     * @param output
     * @return
     * @throws ConnectException
     */
    public boolean get(String serverFile, OutputStream output) throws ConnectException {
        return get(serverFile, output, false);
    }

    /**
     * 下载一个远程文件到指定的流
     * 处理完后记得关闭流
     *
     * @param serverFile
     * @param output
     * @param delFile
     * @return
     * @throws ConnectException
     */
    public boolean get(String serverFile, OutputStream output, boolean delFile) throws ConnectException {
        FTPClient ftpClient = null;
        try {
            ftpClient = getFTPClient();
            // 处理传输
            ftpClient.retrieveFile(serverFile, output);
            if (delFile) { // 删除远程文件
                ftpClient.deleteFile(serverFile);
            }
            return true;
        } catch (IOException e) {
            throw new ConnectException("Couldn't get file from server.");
        } finally {
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
    }

    /**
     * 从ftp服务器上删除一个文件
     *
     * @param delFile
     * @return
     * @throws ConnectException
     */
    public boolean delete(String delFile) throws ConnectException {
        FTPClient ftpClient = null;
        try {
            ftpClient = getFTPClient();
            ftpClient.deleteFile(delFile);
            return true;
        } catch (IOException e) {
            throw new ConnectException("Couldn't delete file from server.");
        } finally {
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
    }

    /**
     * 批量删除
     *
     * @param delFiles
     * @return
     * @throws ConnectException
     */
    public boolean delete(String[] delFiles) throws ConnectException {
        FTPClient ftpClient = null;
        try {
            ftpClient = getFTPClient();
            for (String s : delFiles) {
                ftpClient.deleteFile(s);
            }
            return true;
        } catch (IOException e) {
            throw new ConnectException("Couldn't delete file from server.");
        } finally {
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
    }

    /**
     * 列出远程默认目录下所有的文件
     *
     * @return 远程默认目录下所有文件名的列表，目录不存在或者目录下没有文件时返回0长度的数组
     * @throws ConnectException
     */
    public String[] listNames() throws ConnectException {
        return listNames(null);
    }

    /**
     * 列出远程目录下所有的文件
     *
     * @param remotePath 远程目录名
     * @return 远程目录下所有文件名的列表，目录不存在或者目录下没有文件时返回0长度的数组
     * @throws ConnectException
     */
    public String[] listNames(String remotePath) throws ConnectException {
        FTPClient ftpClient = null;
        try {
            ftpClient = getFTPClient();
            String[] listNames = ftpClient.listNames(remotePath);
            return listNames;
        } catch (IOException e) {
            throw new ConnectException("列出远程目录下所有的文件时出现异常");
        } finally {
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
    }
    public boolean isExist(String remoteFilePath)throws ConnectException{

        FTPClient ftpClient = null;
        try{
            ftpClient = getFTPClient();
            File file=new File(remoteFilePath);

            String remotePath=remoteFilePath.substring(0,(remoteFilePath.indexOf(file.getName())-1));
            String[] listNames = ftpClient.listNames(remotePath);
            System.out.println(remoteFilePath);
            for(int i=0;i<listNames.length;i++){

                if(remoteFilePath.equals(listNames[i])){
                    flag=true;
                    System.out.println("文件:"+file.getName()+"已经存在了");
                    break;

                }else {
                    flag=false;
                }
            }

        } catch (IOException e) {
            throw new ConnectException("查询文件是否存在文件时出现异常");
        } finally {
            if (ftpClient != null) {
                disconnect(ftpClient); //断开连接
            }
        }
        return flag;
    }

    /**
     * 从FTP服务器上下载文件
     * @throws IOException
     */
    public static void down() throws IOException {
        FTPTransfer ftp = new FTPTransfer();
        ftp.setHost("111.67.199.85");
        ftp.setPort(21);
        ftp.setUsername("anonymous");//匿名访问
        ftp.setPassword(null);
        ftp.setBinaryTransfer(true);
        ftp.setPassiveMode(true);
        ftp.setEncoding("utf-8");
        String localFile="/home/u/weather_data/download";

        FTPClient client = ftp.getFTPClient();//创建一个FTP实例

        ftp.connect(client);//链接

        String[] names = client.listNames("/download");//遍历FTP服务器文件夹下的所有文件

        for (int i = 0; i < names.length; i++) {
            System.out.println(names[i]);
        }

        for (String name: names){
            if(name.contains("zip")){//只下载文件
                System.out.println(name);
                String desname = localFile+name;//拼接成本地目录
                System.out.println(desname);

                ftp.get(name,desname);//下载文件
            }
        }

    }

}
