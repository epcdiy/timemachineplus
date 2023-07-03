package org.epcdiy.timemachineplus.model;

import lombok.Data;

@Data
public class BackupTargetRoot {
    private int id;
    private String targetRootPath;
    private String targetRootDir;
    private long spaceRemain;
}
