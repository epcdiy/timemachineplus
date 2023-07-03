package org.epcdiy.timemachineplus.model;

import lombok.Data;

@Data
public class BackupHistory {
    private int id;
    private long backupFileId;
    private long fileSize;
    private long modifyTime;
    private String backupTargetPath;
    private String backupTargetFullPath;
    private int backupTargetRootId;
    private String md5;
}
