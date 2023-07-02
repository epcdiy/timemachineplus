
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * Created by liuq2 on 2018/3/19.
 */
public class serviceRun {
    static Logger logger = Logger.getLogger(serviceRun.class);
    private MySQLHelper mysqlHelper;
    private HttpClintTool myclient;
    private List<Backuproot> BackupRootList=new ArrayList<>();
    private List<Backuptargetroot> BackupTargetRootList=new ArrayList<>();
    private long filecopycount=0;
    private long datacopycount=0;
    private int backupid=0;
    final static String targetBkDirName="BACKUPDATABASE";
    private Random random;
    public  void init()
    {
        logger.info("init db...");
        PropertyConfigurator.configure ( this.getClass().getClassLoader().getResourceAsStream("log4j.properties"));
        // Set the logger level to Level.INFO
        logger.setLevel(Level.INFO);
        mysqlHelper=new MySQLHelper("dbconfig");
        try {
            myclient=new HttpClintTool();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long seed=System.nanoTime();
        random=new Random(seed);
        logger.info("using rand seed :"+seed);
    }

    public void loadBackupRoot() throws SQLException
    {
        logger.info("loadBackupRoot");
        ResultSet ret=mysqlHelper.querySql("select * from tb_backuproot");
        while(ret!=null&&ret.next())
        {
            Backuproot backuproot=new Backuproot();
            backuproot.setId(ret.getInt("id"));
            backuproot.setRootpath(ret.getString("rootpath"));
            File checkfile=new File(backuproot.getRootpath());
            if(!checkfile.exists())
            {
                logger.error("No found backup source :"+backuproot.getRootpath());
            }
            else {
                BackupRootList.add(backuproot);
            }
        }
        ret=mysqlHelper.querySql("select * from tb_backuptargetroot");
        while(ret!=null&&ret.next())
        {
            Backuptargetroot backuptargetroot=new Backuptargetroot();
            backuptargetroot.setId(ret.getInt("id"));
            backuptargetroot.setTagetrootpath(ret.getString("tagetrootpath"));
            backuptargetroot.setTargetrootdir(targetBkDirName);
            File file=new File(backuptargetroot.getTagetrootpath());
            if(!file.exists()) {
                file.mkdir();
            }
            File checkfile=new File(backuptargetroot.getTagetrootpath());
            //if(false&&!checkfile.exists())//removed after debug
            if(!checkfile.exists())
            {
                logger.error("No found backup target :"+backuptargetroot.getTagetrootpath());
            }
            else {
                backuptargetroot.setSpaceRemain(file.getFreeSpace());
                BackupTargetRootList.add(backuptargetroot);
            }
        }
    }

    private static void loadAllFiles(File file,List<String> fileList)
    {
        File[] fs = file.listFiles();
        for(File f:fs)
        {
            if(f.toString().indexOf("/.")!=-1||f.toString().indexOf("\\.")!=-1)
            {
                continue;
            }
            if(f.isDirectory())	//若是目录，则递归打印该目录下的文件
                loadAllFiles(f,fileList);
            if(f.isFile())		//若是文件
            {
                fileList.add(f.toString());
            }
        }
    }

    private Backuptargetroot getAvalidTarget(long needspace)
    {
        List<Backuptargetroot> avaliedTarget=new ArrayList<>();
        for (Backuptargetroot backupTargetRoot : BackupTargetRootList)
        {
            File file=new File(backupTargetRoot.getTagetrootpath());
            backupTargetRoot.setSpaceRemain(file.getFreeSpace());
            if(backupTargetRoot.getSpaceRemain()>needspace)
            {
                avaliedTarget.add(backupTargetRoot);
                //return  backupTargetRoot;
            }
        }
        if(avaliedTarget.size()>0)
        {
            Backuptargetroot backuptargetroot=avaliedTarget.get(0);
            for(Backuptargetroot backuptargetroot1:avaliedTarget)
            {
                if(backuptargetroot1.getSpaceRemain()>backuptargetroot.getSpaceRemain())
                {
                    backuptargetroot=backuptargetroot1;
                }
            }
            return backuptargetroot;
        }
        return null;
    }
    private static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }
    boolean exeCopy(File fileHandle,String file,long backupfileid) throws IOException {
        Backuptargetroot backuptargetroot=getAvalidTarget(fileHandle.length());
        if(backuptargetroot==null)
        {
            logger.error("no space in all targetbackups! need:"+fileHandle.length());
            return false;
        }
        String md5str=DigestUtils.md5Hex(new FileInputStream(file));
        String targetName=backuptargetroot.getTagetrootpath()+File.separator+backuptargetroot.getTargetrootdir()+File.separator+md5str+"_"+System.currentTimeMillis();
        String targetNameSave=File.separator+backuptargetroot.getTargetrootdir()+File.separator+md5str+"_"+System.currentTimeMillis();
        LocalDateTime begincopysingle=LocalDateTime.now();
        try {
            copyFileUsingFileChannels(fileHandle, new File(targetName));
        }
        catch (Exception e)
        {
            logger.error("failed to copy file from "+file+" to "+targetName);
            return false;
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        mysqlHelper.exeSql("insert into tb_backfilehistory (backupfileid,backupid,motifytime,filesize,copystarttime,copyendtime,backuptargetpath,backuptargetrootid,md5) values (" +
                backupfileid+","+backupid+","+fileHandle.lastModified()+","+fileHandle.length()+",'"+dateTimeFormatter.format(begincopysingle)+"','"+dateTimeFormatter.format(LocalDateTime.now())+"','"+targetNameSave+"',"+backuptargetroot.getId()+",'"+md5str+"')");
        logger.info("copy file from "+file+" to "+targetName);
        return  true;
    }


    private void XCopy(Backuproot backuproot) throws SQLException, IOException {
        logger.info("Loading File list:"+backuproot.getRootpath());
        List<String> fileList=new ArrayList<>();
        Map<String,Long> mapFile=new HashMap<>();
        loadAllFiles(new File(backuproot.getRootpath()),fileList);
        logger.info("Loading CurrentFile In Db:"+ backuproot.getId());
        {
            String sqlquery = "select id,filepath from tb_backfiles where backuprootid=" + backuproot.getId();
            ResultSet ret = mysqlHelper.querySql(sqlquery);
            while(ret!=null&&ret.next())
            {
                mapFile.put(ret.getString("filepath"),ret.getLong("id"));
            }
        }
        logger.info("begin xcopy! total:"+ fileList.size());
        long counter=0;
        long timestamp=new Date().getTime()/1000;
        for(String file:fileList)
        {
            if(file.indexOf("/.")!=-1||file.indexOf("\\.")!=-1)
            {
                //不拷贝.开头的文件夹和文件
                continue;
            }
            counter++;
            if(new Date().getTime()/1000!=timestamp)
            {
                timestamp=new Date().getTime()/1000;
                System.out.println("copy progress:"+counter*100/fileList.size()+"%  "+counter+"/"+fileList.size());
            }
            File fileHandle=new File(file);
            long id=0;
           if(!mapFile.containsKey(file))
           {
               String sqlquery="select * from tb_backfiles where filepath='"+file.replace("\\","\\\\").replace("'","\\'")+"' and backuprootid="+backuproot.getId();
               //ResultSet ret= mysqlHelper.querySql(sqlquery);
           //if(ret==null||!ret.next()) {
               //第一次拷贝，创建档案
               mysqlHelper.exeSql("insert into tb_backfiles (backuprootid,filepath,versionhistorycnt,lastbackuptime) values (" +
                       backuproot.getId() + ",'" + file.replace("\\","\\\\").replace("'","\\'") + "',0,now())");
               ResultSet ret= mysqlHelper.querySql(sqlquery);
               if(ret==null||!ret.next())
               {
                   logger.error("内部错误！数据库异常，退出...");
                   break;
               }
               id=ret.getLong("id");
           }
           else
           {
               id=mapFile.get(file);
           }
           //判断文件是否有修改
            ResultSet ret=mysqlHelper.querySql("select * from tb_backfilehistory where backupfileid="+id+" order by id desc limit 1");
           if(ret!=null&&ret.next())
           {
               long lastmotify=ret.getLong("motifytime");
               long filesize=ret.getLong("filesize");
               String hash=ret.getString("md5");
               long fidid=ret.getLong("id");
               if(lastmotify==fileHandle.lastModified()&&filesize==fileHandle.length())
               {
                   //mysqlHelper.exeSql("insert into tb_backfilehistory (backupfileid,backupid,motifytime,filesize,backuptargetpath,backuptargetrootid,md5) values (" +
                   //        id+","+backupid+","+fileHandle.lastModified()+","+fileHandle.length()+",'"+ret.getString("backuptargetpath")+"',"+ret.getInt("backuptargetrootid")+",'"+ret.getString("md5")+"')");

                   continue;
               }
               System.out.println("motify time indb:"+lastmotify+" real:"+fileHandle.lastModified()+" filesize indb:"+filesize+" real:"+fileHandle.length());
              //如果修改时间不一样，文件大小一样，追加校验一次hash，如果hash一样，则更新修改时间，不执行备份
               if(lastmotify!=fileHandle.lastModified()&&filesize==fileHandle.length())
               {
                   String md5str=DigestUtils.md5Hex(new FileInputStream(fileHandle));
                   if(md5str.equals(hash))
                   {
                       logger.info("file not changed but motifytime diff, correcting...");
                       mysqlHelper.exeSql("update tb_backfilehistory set motifytime="+lastmotify+" where backupfileid="+id+" and id="+fidid);
                       continue;
                   }
                   System.out.println("hash indb:"+hash+" real:"+md5str);

               }
           }
           if(!exeCopy(fileHandle,file, id))
           {
               logger.error("拷贝错误！退出...");
               break;
           }
           filecopycount++;
           datacopycount+=fileHandle.length();
        }
    }

    private int beginbackup() throws SQLException {
        mysqlHelper.exeSql("insert into tb_backup (begintime) values(now())");
        ResultSet ret=mysqlHelper.querySql("select LAST_INSERT_ID() from tb_backup");
        if(ret!=null&&ret.next())
        {
           return ret.getInt("LAST_INSERT_ID()");
        }
        return -1;
    }

    private void finishbackup()
    {
        mysqlHelper.exeSql("update tb_backup set endtime=now(),filecopycount="+filecopycount+",datacopycount="+datacopycount+" where id="+backupid);
    }

    public void deleteByBackuprootid(long rootid) throws SQLException {
        long counter = 0;
        long timestamp = new Date().getTime() / 1000;
        System.out.println("loading files backuprootid=" + rootid);
        ResultSet ret = mysqlHelper.querySql("select id from tb_backfiles where backuprootid=" + rootid);
        List<Long> tbbackfilesList = new ArrayList<>();
        while (ret != null && ret.next())
        {
            tbbackfilesList.add(ret.getLong("id"));
        }
        for(long id:tbbackfilesList)
        {
            ResultSet subret= mysqlHelper.querySql("select id,backuptargetpath from tb_backfilehistory where backupfileid="+id);
            if(subret!=null&&subret.next())
            {
                File file=new File(subret.getString("backuptargetpath"));
                if(file.exists())
                {
                    file.delete();
                }
                else
                {
                    logger.error("not found target path:"+subret.getString("backuptargetpath"));
                }
                mysqlHelper.exeSql("delete from tb_backfilehistory where id="+subret.getInt("id"));
            }
            counter++;
            if(new Date().getTime()/1000!=timestamp)
            {
                timestamp=new Date().getTime()/1000;
                System.out.println("delete progress:"+counter*100/tbbackfilesList.size()+"%  "+counter+"/"+tbbackfilesList.size());
            }
        }
        mysqlHelper.exeSql("delete from tb_backfiles where backuprootid="+rootid);
    }

    public void XCopy()
    {
        try {
            backupid=beginbackup();
            if ( backupid== -1) {
                return;
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            return;
        }
        for(Backuproot backuproot:BackupRootList)
        {
            try {
                XCopy(backuproot);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }
        }
        finishbackup();
    }

    private String getTargetrootPath(int targetbkid)
    {
        for(Backuptargetroot backuptargetroot:BackupTargetRootList)
        {
            if(backuptargetroot.getId()==targetbkid)
            {
                return backuptargetroot.getTagetrootpath();
            }
        }
        return null;
    }

    private void removeWastedData(long backupfilehistoryid,String backupfilefullpath) {
        //get fileid
        try {
            ResultSet ret=mysqlHelper.querySql("select backupfileid from tb_backfilehistory where id="+backupfilehistoryid);
            if(ret!=null&&ret.next())
            {
                long backupfileid=ret.getLong("backupfileid");
                mysqlHelper.exeSql("delete from tb_backfilehistory where id="+backupfilehistoryid);
                File file=new File(backupfilefullpath);
                if(file.exists()) {
                    logger.info("delete broken file:"+backupfilefullpath);
                    file.delete();
                }
                ret=mysqlHelper.querySql("select count(*) from tb_backfilehistory where backupfileid="+backupfileid);
                if(ret!=null&&ret.next())
                {
                    int historycnt=ret.getInt("count(*)");
                    if(historycnt==0)
                    {
                        //delete parent
                        mysqlHelper.exeSql("delete from tb_backfiles where id="+backupfileid);
                    }
                }
            }
        } catch (Exception e)
        {
            logger.error(e.getMessage());
        }
    }

    private void checkdata(boolean withhash)
    {
        try {
            logger.info("loading all file and check");
            int innercounter=0;
            List<BackupHistory> historyList=new ArrayList<>();
            long timestamp=new Date().getTime()/1000;
            while(true)
            {
                int counter=0;
                ResultSet ret=mysqlHelper.querySql("select * from tb_backfilehistory limit "+innercounter+",1000");
                while(ret!=null&&ret.next()) {
                    BackupHistory backupHistory = new BackupHistory();
                    backupHistory.setId(ret.getInt("id"));
                    backupHistory.setMd5(ret.getString("md5"));
                    backupHistory.setFilesize(ret.getLong("filesize"));
                    backupHistory.setBackupfileid(ret.getLong("backupfileid"));
                    backupHistory.setBackuptargetrootid(ret.getInt("backuptargetrootid"));
                    backupHistory.setBackuptargetpath(ret.getString("backuptargetpath"));
                    //check file
                    String backuprootpath = getTargetrootPath(backupHistory.getBackuptargetrootid()) + backupHistory.getBackuptargetpath();
                    backupHistory.backuptargetfullpath=backuprootpath;
                    File fp = new File(backuprootpath);
                    if (!fp.exists() || fp.length() != backupHistory.getFilesize()) {
                        //kill
                        //logger.info("file not found or size mismatch:"+backuprootpath);
                        historyList.add(backupHistory);
                    }
                    else if (withhash)
                    {
                        String md5str=DigestUtils.md5Hex(new FileInputStream(backuprootpath));
                        if(!backupHistory.getMd5().equals(md5str))
                        {
                            //kill
                            logger.info("file hash not mismatch:"+backuprootpath);
                            historyList.add(backupHistory);
                        }
                    }
                    counter++;
                    if(new Date().getTime()/1000!=timestamp)
                    {
                        timestamp=new Date().getTime()/1000;
                        System.out.println("check num:"+(innercounter+counter)+" found:"+historyList.size());
                    }
                }
                if(counter==0)
                {
                    break;
                }
                innercounter+=counter;
            }
            innercounter=0;
            logger.info("begin removing wasted backup file recoreds");
            for(BackupHistory backupHistory:historyList)
            {
                innercounter++;
                removeWastedData(backupHistory.getId(),backupHistory.getBackuptargetfullpath());
                if(new Date().getTime()/1000!=timestamp)
                {
                    timestamp=new Date().getTime()/1000;
                    System.out.println("rm db count:"+innercounter+" / "+innercounter*100/historyList.size()+"%");
                }
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        serviceRun serviceRun=new serviceRun();
        try {
            serviceRun.init();
            serviceRun.loadBackupRoot();
            if(args.length==1)
            {
                if(args[0].equals("checkdata"))
                {
                    logger.info("begin to checkdata");
                    serviceRun.checkdata(false);
                }
                else if(args[0].equals("checkdatawithhash"))
                {
                    logger.info("begin to checkdata with hash");
                    serviceRun.checkdata(true);
                }
                else {
                    logger.info("cleaning data from backuprootid：" + args[0]);
                    serviceRun.deleteByBackuprootid(Long.parseLong(args[0]));
                    return;
                }
            }
            serviceRun.XCopy();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
