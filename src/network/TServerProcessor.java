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
    boolean fileFlag=false;
    long fsize;
    int id;
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

    public long sendFile(String path){//put
        String str="GET0";
        long fs=0;
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                str += String.valueOf(file.length());//4开始，文件长度
                for (int i = str.length(); i < 255; i++)
                    str += '0';
                str += file.getName();//255开始，文件名字
                for (int i = str.length(); i < BUFFER_SIZE; i++)
                    str += '0';
                send(str.getBytes(), BUFFER_SIZE);
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
            return 0;
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
                            case "PUT"://服务器端的接受
                                String fs="", type="", ndID="";
                                int noteDocID = 0;
                                type+=(char)buf[3];
                                if (fos == null) {
                                    fileFlag = true;
                                    String fileName = "";
                                    for (int i = 255; i < buf.length; i++) {
                                        if (buf[i] != 0)
                                            fileName += (char) buf[i];
                                        else
                                            break;
                                    }
                                    if(type=="2"){
                                        for(int i=500; i<buf.length;i++){
                                            if(buf[i]!=0)
                                                ndID+=(char)buf[i];
                                            else
                                                break;
                                        }
                                        noteDocID=Integer.valueOf(ndID);
                                    }
                                    File file = new File(fileName);
                                    System.out.println(file.getAbsolutePath());
                                    fos = new FileOutputStream(fileName);
                                    for (int i = 4; i < buf.length; i++) {
                                        if (buf[i] != 0)
                                            fs += buf[i];
                                        else
                                            break;
                                    }
                                    fsize = Integer.valueOf(fs);
                                    if(type=="1")
                                        DatabaseHelper.getInstance().uploadDoc(fileName, id, fileName);
                                    else if(type=="2")
                                        DatabaseHelper.getInstance().uploadNote(noteDocID, id, fileName);
                                }
                                break;
                            case "GET"://客户端要求获取getID文档
                                int getID;
                                String gid="";
                                for(int i=4;i<BUFFER_SIZE;i++){
                                    if(buf[i]!=0)
                                        gid+=buf[i];
                                    else
                                        break;
                                }
                                getID=Integer.valueOf(gid);
                                String docPath=DatabaseHelper.getInstance().getDocPath(getID);
                                sendFile(docPath);
                            case "RGT"://注册
                                String userNameRegister="", passwordRegister="";
                                for (int i = 4; i < 36; i++) {//用户名，最多32个字节
                                    if (buf[i] != 0)
                                        userNameRegister += buf[i];
                                    else
                                        break;
                                }
                                for (int i = 36; i < 66; i++) {//email，最多32个字节
                                    if (buf[i] != 0)
                                        passwordRegister += buf[i];
                                    else
                                        break;
                                }
                                if (DatabaseHelper.getInstance().checkRegister(userNameRegister, passwordRegister) == -1)
                                    send(new String("RGF").getBytes(), 3);
                                else
                                    send(new String("RGS").getBytes(), 3);
                                break;
                            case "GUI"://获取用户id的信息
                                int userID;
                                String uID="";
                                for(int i=4;i<BUFFER_SIZE;i++){
                                    if(buf[i]!=0)
                                        uID+=buf[i];
                                    else
                                        break;
                                }
                                userID=Integer.valueOf(uID);
                                ResultSet rsGetUserInfo;
                                rsGetUserInfo=DatabaseHelper.getInstance().getFollow(userID, 0);
                                String userInfo="GUI0";
                                userInfo+=rsGetUserInfo.getString("name");
                                for(int i=userInfo.length();i<36;i++)
                                    userInfo+='0';
                                userInfo+=rsGetUserInfo.getInt("points");
                                for(int i=userInfo.length();i<BUFFER_SIZE;i++)
                                    userInfo+='0';
                                send(userInfo.getBytes(), BUFFER_SIZE);
                                break;
                            case "GDI"://获取docID的信息
                                int docID;
                                String dID="";
                                for(int i=4;i<BUFFER_SIZE;i++){
                                    if(buf[i]!=0)
                                        dID+=buf[i];
                                    else
                                        break;
                                }
                                docID=Integer.valueOf(dID);
                                ResultSet rsGetDocInfo;
                                rsGetDocInfo=DatabaseHelper.getInstance().getFollow(docID, 1);
                                String docInfo="GDI0";
                                docInfo+=rsGetDocInfo.getInt("id");
                                for(int i=docInfo.length();i<36;i++)
                                    docInfo+='0';
                                docInfo+=rsGetDocInfo.getString("title");
                                for(int i=docInfo.length();i<66;i++)
                                    docInfo+='0';
                                docInfo+=rsGetDocInfo.getInt("ownerid");
                                for(int i=docInfo.length();i<BUFFER_SIZE;i++)
                                    docInfo+='0';
                                send(docInfo.getBytes(), BUFFER_SIZE);
                                break;
                            case "GNI"://获取noteID的信息
                                int noteID;
                                String nID="";
                                for(int i=4;i<BUFFER_SIZE;i++){
                                    if(buf[i]!=0)
                                        nID+=buf[i];
                                    else
                                        break;
                                }
                                noteID=Integer.valueOf(nID);
                                ResultSet rsGetNoteInfo;
                                rsGetNoteInfo=DatabaseHelper.getInstance().getFollow(noteID, 2);
                                String noteInfo="GDI0";
                                noteInfo+=rsGetNoteInfo.getInt("id");
                                for(int i=noteInfo.length();i<36;i++)
                                    noteInfo+='0';
                                noteInfo+=rsGetNoteInfo.getInt("docid");
                                for(int i=noteInfo.length();i<66;i++)
                                    noteInfo+='0';
                                noteInfo+=rsGetNoteInfo.getInt("ownerid");
                                for(int i=noteInfo.length();i<BUFFER_SIZE;i++)
                                    noteInfo+='0';
                                send(noteInfo.getBytes(), BUFFER_SIZE);
                                break;
                            case "SGI"://登录
                                String userNameSignIn="", passwordSignIn="";
                                for (int i = 4; i < 36; i++) {//用户名，最多32个字节
                                    if (buf[i] != 0)
                                        userNameSignIn += buf[i];
                                    else
                                        break;
                                }
                                for (int i = 36; i < 66; i++) {//email，最多32个字节
                                    if (buf[i] != 0)
                                        passwordSignIn += buf[i];
                                    else
                                        break;
                                }
                                ResultSet rsSignIn;
                                rsSignIn = DatabaseHelper.getInstance().checkLogin(userNameSignIn, passwordSignIn);
                                if (rsSignIn == null)
                                    send(new String("SIF").getBytes(), 3);
                                else {
                                    id = rsSignIn.getInt("id");
                                    send(new String("SIS").getBytes(), 3);
                                }
                                break;
                            case "FAP"://喜欢人
                                int pid2;
                                String pid2str="";
                                for (int i = 4; i < buf.length; i++) {
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
                                for (int i = 4; i < buf.length; i++) {
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
                                for (int i = 4; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        nid2str += buf[i];
                                    else
                                        break;
                                }
                                nid2 = Integer.valueOf(nid2str);
                                DatabaseHelper.getInstance().follow(id,nid2,2);
                                break;
                            case "SHA":
                                int shareAllID;
                                short shareAllType;
                                String said="", sat="";
                                sat+=buf[3];
                                shareAllType=Short.valueOf(sat);
                                for (int i = 4; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        said += buf[i];
                                    else
                                        break;
                                }
                                shareAllID = Integer.valueOf(said);
                                DatabaseHelper.getInstance().createShare(id, shareAllType, shareAllID, 0);//0 for all
                                break;
                            case "SHF":
                                int shareFollowingID;
                                short shareFollowingType;
                                String sfid="", sft="";
                                sft+=buf[3];
                                shareFollowingType=Short.valueOf(sft);
                                for (int i = 4; i < buf.length; i++) {
                                    if (buf[i] != 0)
                                        sfid += buf[i];
                                    else
                                        break;
                                }
                                shareFollowingID = Integer.valueOf(sfid);
                                DatabaseHelper.getInstance().createShare(id, shareFollowingType, shareFollowingID, 1);//1 for following
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
