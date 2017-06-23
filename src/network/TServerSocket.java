package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Tony on 2017/6/21.
 */
public class TServerSocket implements Runnable {
    ServerSocket ss=null;
    //TServerProcessor[] tsp;
    boolean flag=false;
    Thread th=null;

    public TServerSocket(int port){
        try {
            ss=new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        th=new Thread(this);
        flag=true;
        //tsp=new TServerProcessor[16];
        th.start();
    }

    public void stop(){
        try {
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (flag){
            try {
                Socket s=ss.accept();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
