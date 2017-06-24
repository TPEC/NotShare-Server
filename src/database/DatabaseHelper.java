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
     * 登陆，如果成功返回ResultSet，失败返回null
     * @param name
     * @param pass
     * @return ResultSet
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
     * 获取follow列表，type:0-user,1-doc,2-note
     * @param id
     * @param type
     * @return
     */
    public ResultSet getFollow(int id, int type){
        return jdbch.executeQuery(concat_s("*","uf"+String.valueOf(id),"type="+String.valueOf(type)));
    }

    /**
     * 注册，成功返回id，失败返回-1
     * @param name
     * @param pass
     * @return id
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
                jdbch.executeUpdate("create table uf"+String.valueOf(uid)+"(id int(4) not null,type int(2) not null primary key(id,type))");//关注
                jdbch.executeUpdate("create table uo"+String.valueOf(uid)+"(id int(4) not null,type int(2) not null primary key(id,type))");//拥有
                return uid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 搜索文档，成功返回ResultSet，失败返回null
     * @param str
     * @return
     */
    public ResultSet searchDoc(String str){
        String sql="title like\'"+str+"\'";
        return jdbch.executeQuery(concat_s("*","document",sql));
    }

    public String getDocPath(int id){
        ResultSet rs=jdbch.executeQuery(concat_s("*","document","id="+String.valueOf(id)));
        try {
            if(rs.next()){
                return rs.getString("filepath");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNotePath(int id){
        ResultSet rs=jdbch.executeQuery(concat_s("*","note","id="+String.valueOf(id)));
        try {
            if(rs.next()){
                return rs.getString("filepath");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean uploadDoc(String title,int ownerid,String filePath){
        String sql="\'"+title+"\',"+String.valueOf(ownerid)+",\'"+filePath+"\'";
        jdbch.executeUpdate(concat_i("document","title,ownerid,filepath",sql));
        return true;
    }

    public boolean uploadNote(int docid,int ownerid,String filePath){
        String sql=String.valueOf(docid)+","+String.valueOf(ownerid)+",\'"+filePath+"\'";
        jdbch.executeUpdate(concat_i("note","docid,ownerid,filepath",sql));
        return true;
    }

    public ResultSet getUserInfo(int id){
        ResultSet rs=jdbch.executeQuery(concat_s("*","user","id="+String.valueOf(id)));
        try {
            if(rs.next())
                return rs;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ResultSet getDocInfo(int id){
        ResultSet rs=jdbch.executeQuery(concat_s("*","document","id="+String.valueOf(id)));
        try {
            if(rs.next())
                return rs;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ResultSet getNoteInfo(int id){
        ResultSet rs=jdbch.executeQuery(concat_s("*","note","id="+String.valueOf(id)));
        try {
            if(rs.next())
                return rs;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param id
     * @param id2
     * @param type 0-user, 1-doc, 2-note
     * @return
     */
    public boolean follow(int id,int id2,int type){
        String table_="";
        switch (type){
            case 0:
                table_="user";
                break;
            case 1:
                table_="document";
                break;
            case 2:
                table_="note";
                break;
        }
        ResultSet rs=jdbch.executeQuery(concat_s("*",table_,"id="+String.valueOf(id2)));
        try {
            if(rs.next()){
                rs=jdbch.executeQuery(concat_s("*","user","id="+String.valueOf(id)));
                if(rs.next()){
                    String sql="id="+String.valueOf(id2)+" and type="+String.valueOf(type);
                    rs=jdbch.executeQuery(concat_s("*","uf"+String.valueOf(id),sql));
                    if(rs.next()){
                        return false;
                    }else{
                        sql=String.valueOf(id2)+ ","+String.valueOf(type);
                        jdbch.executeUpdate(concat_i("uf"+String.valueOf(id),"id,type",sql));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String concat_s(String column_,String table_,String where_){
        return concat_s(column_,table_) +" where "+where_;
    }

    private String concat_s(String column_,String table_){
        return "select " + column_+" from " +table_;
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
