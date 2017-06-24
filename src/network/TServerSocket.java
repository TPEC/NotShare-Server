package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tony on 2017/6/21.
 */
public class TServerSocket implements Runnable {
    List<TServerProcessor> tsp=null;
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
        tsp=new ArrayList<>();
        th=new Thread(this);
        flag=true;
        //tsp=new TServerProcessor[16];
        th.start();
    }

    public void stop(){
        try {
            ss.close();
            for(int i=0;i<tsp.size();i++)
                tsp.get(i).stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (flag){
            try {
                Socket s=ss.accept();
                tsp.add(new TServerProcessor(s));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
