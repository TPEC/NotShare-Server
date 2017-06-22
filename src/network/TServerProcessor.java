package network;

import java.io.*;
import java.net.Socket;

/**
 * Created by Irene on 2017/6/22.
 */
public class TServerProcessor implements Runnable {
    static final int BUFFER_SIZE=512;
    Socket s;
    Thread t;
    boolean flag;
    String str="";
    String fs="";
    long fsize;
    String userName="", emailAddress="", password="";
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
        while (flag){
            try {
                int k=is.read(buf);
                if(k>0){
                    if(buf[0]=='D' && buf[1]=='N' && buf[2]=='L'){
                        if(fos==null){
                            String fileName="";
                            for(int i=255;i<buf.length;i++){
                                if(buf[i]!=0)
                                    fileName+=(char)buf[i];
                                else
                                    break;
                            }
                            File file=new File(fileName);
                            System.out.println(file.getAbsolutePath());
                            fos=new FileOutputStream(fileName);
                            for(int i=3;i<buf.length;i++){
                                if(buf[i]!=0)
                                    fs+=buf[i];
                                else
                                    break;
                            }
                            fsize=Integer.valueOf(fs);
                        }else {
                            fos.write(buf,0,k);
                            fos.flush();
                        }
                    }else if(buf[0]=='R' && buf[1]=='G' && buf[2]=='T'){
                        for(int i=4;i<36;i++){//用户名，最多32个字节
                            if(buf[i]!=0)
                                userName+=buf[i];
                            else
                                break;
                        }
                        for(int i=36;i<66;i++){//email，最多32个字节
                            if(buf[i]!=0)
                                emailAddress+=buf[i];
                            else
                                break;
                        }
                        for(int i=66;i<98;i++){//密码，最多32个字节
                            if(buf[i]!=0)
                                password+=buf[i];
                            else
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
