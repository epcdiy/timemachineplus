/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.0.223
 Source Server Type    : MySQL
 Source Server Version : 50717
 Source Host           : 192.168.0.223:3306
 Source Schema         : timemachine

 Target Server Type    : MySQL
 Target Server Version : 50717
 File Encoding         : 65001

 Date: 01/07/2023 10:58:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_backfilehistory
-- ----------------------------
DROP TABLE IF EXISTS `tb_backfilehistory`;
CREATE TABLE `tb_backfilehistory` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `backupfileid` bigint(20) DEFAULT NULL,
  `motifytime` bigint(20) DEFAULT NULL COMMENT '文件修改时间',
  `filesize` bigint(20) DEFAULT NULL COMMENT '文件大小',
  `copystarttime` datetime DEFAULT NULL COMMENT '备份拷贝开始时间',
  `copyendtime` datetime DEFAULT NULL COMMENT '备份拷贝结束时间',
  `backuptargetpath` varchar(4096) DEFAULT NULL COMMENT '备份目标路径',
  `backuptargetrootid` int(11) DEFAULT NULL COMMENT '备份目标id',
  `md5` char(64) DEFAULT NULL,
  `backupid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `dualid` (`backupfileid`,`backupid`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=601953 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for tb_backfiles
-- ----------------------------
DROP TABLE IF EXISTS `tb_backfiles`;
CREATE TABLE `tb_backfiles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `backuprootid` int(11) DEFAULT NULL COMMENT '来源文件夹表id',
  `filepath` varchar(1024) DEFAULT NULL COMMENT '来源完整路径',
  `versionhistorycnt` int(11) DEFAULT NULL COMMENT '备份次数',
  `lastbackuptime` datetime DEFAULT NULL COMMENT '最新备份时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `backuprootid` (`backuprootid`),
  FULLTEXT KEY `filepath` (`filepath`)
) ENGINE=InnoDB AUTO_INCREMENT=515337 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for tb_backup
-- ----------------------------
DROP TABLE IF EXISTS `tb_backup`;
CREATE TABLE `tb_backup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `begintime` datetime DEFAULT NULL COMMENT '备份开始时间',
  `endtime` datetime DEFAULT NULL COMMENT '备份结束时间',
  `filecopycount` int(11) DEFAULT NULL COMMENT '变动文件数',
  `datacopycount` bigint(20) DEFAULT NULL COMMENT '拷贝字节数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=531 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for tb_backuproot
-- ----------------------------
DROP TABLE IF EXISTS `tb_backuproot`;
CREATE TABLE `tb_backuproot` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rootpath` varchar(4096) DEFAULT NULL COMMENT '来源根路径',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for tb_backuptargetroot
-- ----------------------------
DROP TABLE IF EXISTS `tb_backuptargetroot`;
CREATE TABLE `tb_backuptargetroot` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagetrootpath` varchar(4096) DEFAULT NULL COMMENT '备份目标路径根目录',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
