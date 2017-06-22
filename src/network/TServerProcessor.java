package network;

import database.DatabaseHelper;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Irene on 2017/6/22.
 */
public class TServerProcessor implements Runnable {
    static final int BUFFER_SIZE=512;
    Socket s;
    Thread t;
    boolean flag;
    String str="";
    boolean fileFlag=false;
    String fs="";
    long fsize;
    String userName="", emailAddress="", password="";
    int id;
    ResultSet rs;
    OutputStream os=null;
    InputStream is=null;
    FileOutputStream fos=null;

    public TServerProcessor(Socket ss) throws IOException {
        s=ss;
        os=s.getOutputStream();//传给客户端
        is=s.getInputStream();//客户端传来的东西
        t=new Thread(this);
        flag=true;
        t.start();
    }

    public int send(byte[] buf,int size) {
        if (os != null) {
            try {
                os.write(buf, 0, size);
                os.flush();
                return size;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public long sendFile(String path){
        long fs=0;
        try {
            File file=new File(path);
            if(file.exists() && file.isFile()) {
                byte[] fileN=new byte[BUFFER_SIZE];
                String fn=file.getName();
                fileN[0]='D';//协议头
                fileN[1]='N';
                fileN[2]='L';
                str=String.valueOf(file.length());
                for(int i=0;i<fileN.length;i++){//文件长度
                    if(i<str.length())
                        fileN[3+i]= (byte) str.charAt(i);
                    else
                        fileN[i]=0;
                }
                for(int i=255;i<fn.length();i++) {//文件名字，从255开始
                    fileN[i]=(byte)fn.charAt(i-255);
                }
                send(fileN,BUFFER_SIZE);
                FileInputStream fis=new FileInputStream(file);
                byte[] buf = new byte[BUFFER_SIZE];
                int rs;
                while ((rs = fis.read(buf)) >= 0) {
                    long fs_ = send(buf, rs);
                    if (fs_ <= 0)
                        break;
                    fs += rs;
                }
                System.out.println(fs);
                fis.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            return fs;
        }
    }

    public void stop(){
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buf=new byte[BUFFER_SIZE];
        long writeLength=0;
        while (flag){
            try {
                int k=is.read(buf);
                if(k>0){
                    String a="";
                    a+=(char)buf[0];
                    a+=(char)buf[1];
                    a+=(char)buf[2];
                    if(fileFlag==false) {
                        switch (a) {
                            case "DNL"://下载
                                if (fos == null) {
                                    fileFlag = true;
                                    String fileName = "";
                                    for (int i = 255; i < buf.length; i++) {
                                        if (buf[i] != 0)
                                            fileName += (char) buf[i];
                                        else
                                            break;
                                    }
                                    File file = new File(fileName);
                                    System.out.println(file.getAbsolutePath());
                                    fos = new FileOutputStream(fileName);
                                    for (int i = 3; i < buf.length; i++) {
                                        if (buf[i] != 0)
                                            fs += buf[i];
                                        else
                                            break;
                                    }
                                    fsize = Integer.valueOf(fs);
                                }
                                break;
                            case "RGT"://注册
                                userName = "";
                                for (int i = 4; i < 36; i++) {//用户名，最多32个字节
                                    if (buf[i] != 0)
                                        userName += buf[i];
                                    else
                                        break;
                                }
                                password = "";
                                for (int i = 36; i < 66; i++) {//email，最多32个字节
                                    if (buf[i] != 0)
                                        password += buf[i];
                                    else
                                        break;
                                }
                                if (DatabaseHelper.getInstance().checkRegister(userName, password) == -1)
                                    send(new String("RGF").getBytes(), 3);
                                else
                                    send(new String("RGS").getBytes(), 3);
                                break;
                            case "SGI"://登录
                                userName = "";
                                for (int i = 4; i < 36; i++) {//用户名，最多32个字节
                                    if (buf[i] != 0)
                                        userName += buf[i];
                                    else
                                        break;
                                }
                                password = "";
                                for (int i = 36; i < 66; i++) {//email，最多32个字节
                                    if (buf[i] != 0)
                                        password += buf[i];
                                    else
                                        break;
                                }
                                rs = DatabaseHelper.getInstance().checkLogin(userName, password);
                                if (rs == null)
                                    send(new String("SIF").getBytes(), 3);
                                else {
                                    id = rs.getInt("id");
                                    send(new String("SIS").getBytes(), 3);
                                }
                                break;
                            case "FAP"://喜欢人
                                int pid2;
                                String pid2str="";
                                for (int i = 3; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        pid2str += buf[i];
                                    else
                                        break;
                                }
                                pid2 = Integer.valueOf(pid2str);
                                DatabaseHelper.getInstance().follow(id,pid2,0);
                                break;
                            case "FAD"://喜欢doc
                                int did2;
                                String did2str="";
                                for (int i = 3; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        did2str += buf[i];
                                    else
                                        break;
                                }
                                did2 = Integer.valueOf(did2str);
                                DatabaseHelper.getInstance().follow(id,did2,1);
                                break;
                            case "FAN"://喜欢note
                                int nid2;
                                String nid2str="";
                                for (int i = 3; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        nid2str += buf[i];
                                    else
                                        break;
                                }
                                nid2 = Integer.valueOf(nid2str);
                                DatabaseHelper.getInstance().follow(id,nid2,2);
                                break;
                            case "SHA":

                                break;
                            case "SHP":

                                break;
                        }
                    }else {
                        if(writeLength<fsize){
                            fos.write(buf,0,k);
                            writeLength+=k;
                            fos.flush();
                        }else {
                            fos.close();
                            fos=null;
                            fileFlag=false;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
