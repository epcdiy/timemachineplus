import lombok.Data;

@Data
public class BackupHistory
{
    int id;
    long backupfileid;
    long filesize;
    long motifytime;
    String backuptargetpath;
    String backuptargetfullpath;
    int backuptargetrootid;
    String md5;
}
