package org.epcdiy.timemachineplus.model;
import lombok.Data;

@Data
public class BackupHistory
{
    public int id;
    public long backupfileid;
    public long filesize;
    public long motifytime;
    public String backuptargetpath;
    public String backuptargetfullpath;
    public int backuptargetrootid;
    public String md5;
}
