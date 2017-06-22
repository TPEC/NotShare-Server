package database;

import com.sun.org.apache.regexp.internal.RE;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Tony on 2017/6/22.
 */
public class DatabaseHelper {
    JDBCH jdbch=null;

    /**
     *
     * @param name
     * @param pass
     * @return
     */
    public ResultSet checkLogin(String name, String pass){
        String sql="name=\'"+name+"\' and pass=\'"+ pass +"\'";
        ResultSet rs=jdbch.executeQuery(concat_s("*","user",sql));
        try {
            if(rs.next()){
                return rs;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param name
     * @param pass
     * @return id(-1: none)
     */
    public int checkRegister(String name,String pass){
        String sql="name=\'"+name+"\'";
        ResultSet rs=jdbch.executeQuery(concat_s("*","user",sql));
        try {
            if(rs.next()){
                return -1;
            }else{
                sql="\'"+name+"\',\'"+pass+"\',0";
                jdbch.executeUpdate(concat_i("user","name,pass,points",sql));
                int uid=checkLogin(name,pass).getInt("id");
                jdbch.executeUpdate("create table uk"+String.valueOf(uid)+"(id int(4) not null,type int(2) not null primary key(id,type))");//关注
                jdbch.executeUpdate("create table uo"+String.valueOf(uid)+"(id int(4) not null,type int(2) not null primary key(id,type))mmm");//拥有
                return uid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String concat_s(String column_,String table_,String where_){
        return "select " + column_+" from " +table_ +" where "+where_;
    }

    private String concat_i(String table_,String column_,String value_){
        return "insert into "+table_+"("+column_+") values("+value_+")";
    }

    private static DatabaseHelper ourInstance = new DatabaseHelper();

    public static DatabaseHelper getInstance() {
        return ourInstance;
    }

    private DatabaseHelper() {
        jdbch=new JDBCH();
    }
}
