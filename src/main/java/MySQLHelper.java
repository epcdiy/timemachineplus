import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ResourceBundle;

/**
 *
 * 项目名称：JavaSQL1    
 * 类名称：DBHelper    
 * 类描述：MySQL数据库操作类    
 * 创建人：Administrator 
 * 创建时间：2014-11-25 下午5:11:11    
 * 修改备注：    
 * @version
 */
public class MySQLHelper
{
    public String url = null;// "jdbc:mysql://127.0.0.1/vge_whu"; //数据库连接
    public String name = "com.mysql.jdbc.Driver";   //程序驱动
    public String user = null;//"root";  //用户名
    public String password = null;//"abc@123"; //密码
    private String configFilename;
    public int maxpackage=1024;
    public Connection conn = null;
    private  Statement statement=null;
    static Logger logger = Logger.getLogger(MySQLHelper.class);

    private void initCfg() throws Exception {
        ResourceBundle bundle=null;
        try {
            bundle = ResourceBundle.getBundle(configFilename);
            url=bundle.getString("mysqlurl");
            user=bundle.getString("mysqluser");
            password=bundle.getString("mysqlpassword");
            maxpackage=Integer.parseInt(bundle.getString("max_allowed_packet"));
        } catch (Exception var6) {
            var6.printStackTrace(System.out);
            logger.error("Can not Found Config File dbconfig.properties");
        }
    }

    public boolean init()
    {
        if(url==null)
        {
            try {
                initCfg();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logger.error(e.toString());
                return false;
            }
        }
        try
        {
            Class.forName(name);// 指定连接类型
            conn = DriverManager.getConnection(url, user, password);// 获取连接
            logger.info("mysql connected!");
            return true;
        } catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(e.toString());
        }
        return false;
    }

    /**
     *
     * 创建一个新的实例 DBHelper.
     */
    public MySQLHelper(String dbconfig)
    {
        configFilename=dbconfig;
        init();
       // importSql("D:\\project\\03_fj_tobacco_qr_code\\05_RD_TS_PI_VER_VAL\\02_概要设计\\02-接口设计\\数据上传和云端接收接口\\测试用文件\\db_180316141201\\tb_cigar.sql");

    }



    public boolean exeSql(String sql)
    {
        try {
            if(!conn.isValid(1000))
            {
                if(!init())
                {
                    return false;
                }
            }
            conn.setAutoCommit(true);
            PreparedStatement pst = conn.prepareStatement(sql);// 准备执行语句
            pst.execute();
            pst.close();
            return true;
        } catch (SQLException e)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(e.toString());
            if(sql.length()>256)
            {
                logger.error(sql.substring(0, 256)+"...");
            }
            else
            {
                logger.error(sql);
            }
        }
        return false;
    }

    public boolean importSql(String filePath)
    {
        logger.info("sql importing "+filePath);
        StringBuilder sqlline=new StringBuilder("");
        File file = new File(filePath);
        if(file.exists()&&file.isFile())
        {

            int curpackage=0;
            int cnt=0;
            try
            {
                if(!conn.isValid(1000))
                {
                    if(!init())
                    {
                        return false;
                    }
                }
                conn.setAutoCommit(false);
                Statement stmt = conn.createStatement();
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), "UTF-8");// 考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = "";
                sqlline=new StringBuilder("");
                while ((lineTxt = bufferedReader.readLine()) != null)
                {
                    sqlline.append(lineTxt);
                    if(sqlline.indexOf(";")!=-1)
                    {
                        stmt.addBatch(sqlline.toString());
                        curpackage+=sqlline.length();
                        if(curpackage>maxpackage)
                        {
                            curpackage=0;
                            logger.debug("executeBatch:"+stmt.executeBatch().length);
                            conn.commit();
                        }
                        sqlline=new StringBuilder("");
                        cnt++;
                    }

                }
                bufferedReader.close();
                read.close();
                if(curpackage>0)
                {
                    curpackage=0;
                    logger.debug("final executeBatch:"+stmt.executeBatch().length);
                    conn.commit();
                }
                logger.info("imported "+cnt+" lines");
                conn.setAutoCommit(true);
                stmt.close();
                return  true;
            }
            catch (Exception e)
               {
                   logger.error("file "+filePath+" read error!");
                   StringWriter sw = new StringWriter();
                   PrintWriter pw = new PrintWriter(sw);
                   e.printStackTrace(pw);
                   logger.error(e.toString());
                   if(sqlline.length()>256)
                   {
                       logger.error(sqlline.substring(0, 256)+"...");
                   }
                   else
                   {
                       logger.error(sqlline);
                   }
                }
        }
        else
        {
            logger.error("file "+filePath+" unaccessable!");
        }

        return false;

    }

    public ResultSet querySql(String sql)
    {
        try {
            if(!conn.isValid(1000))
            {
                if(!init())
                {
                    return null;
                }
            }
            conn.setAutoCommit(true);
            if(statement!=null)
            {
                statement.close();
            }
            statement=conn.createStatement();
            ResultSet  rs = statement.executeQuery(sql);
            return rs;
        } catch (SQLException e) {
            logger.error("querySql error!sql="+sql);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(e.toString());
            logger.error(sql);
        }
        return null;
    }

    /**
     *
     * 方法名称: close ； 
     * 方法描述:  关闭数据库连接 ； 
     * 参数 ：  
     * 返回类型: void ； 
     * 创建人：James； 
     * 创建时间：2014-11-25 下午7:00:12； 
     * @throws
     */
    public void close()
    {
        try
        {
            this.conn.close();

        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

}  