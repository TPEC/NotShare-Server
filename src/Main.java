/**
 * Created by Tony on 2017/6/21.
 */
public class Main {
    public static void main(String[] args){
        String s="";
        byte[] b=new byte[]{64,65,66};
        s+=(char)b[0];
        s+=(char)b[1];
        System.out.print(s);
    }
}
