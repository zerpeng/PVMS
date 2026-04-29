/*
 PVMS 数据库设计脚本 (v1.2)
 包含：pvms_auth (认证与权限) 和 pvms_media (媒体核心与任务)
*/

-- ----------------------------
-- 1. 创建认证与权限库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `pvms_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `pvms_auth`;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `role_key` varchar(20) NOT NULL COMMENT '角色标识：ADMIN, USER, VISITOR',
  `status` tinyint DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户信息表';

DROP TABLE IF EXISTS `sys_visitor_application`;
CREATE TABLE `sys_visitor_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_name` varchar(50) NOT NULL COMMENT '申请人姓名',
  `reason` varchar(255) DEFAULT NULL COMMENT '申请理由',
  `target_album_id` bigint DEFAULT NULL COMMENT '申请访问的相册ID',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-待审批，1-已通过，2-已拒绝',
  `auth_code` varchar(10) DEFAULT NULL COMMENT '审批通过后的访问码',
  `expire_time` datetime DEFAULT NULL COMMENT '权限过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `audit_time` datetime DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='访客访问申请表';


-- ----------------------------
-- 2. 创建媒体库与任务库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `pvms_media` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `pvms_media`;

DROP TABLE IF EXISTS `monitor_directory`;
CREATE TABLE `monitor_directory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dir_path` varchar(1024) NOT NULL COMMENT '本地绝对路径',
  `is_recursive` tinyint DEFAULT '1' COMMENT '是否递归子目录',
  `scan_status` tinyint DEFAULT '0' COMMENT '扫描状态：0-待机，1-扫描中',
  `last_scan_time` datetime DEFAULT NULL COMMENT '最后一次完整扫描时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path` (`dir_path`(768))
) ENGINE=InnoDB COMMENT='监控目录配置表';

DROP TABLE IF EXISTS `media_file`;
CREATE TABLE `media_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_path` varchar(1024) NOT NULL COMMENT '文件绝对路径',
  `file_hash` varchar(64) NOT NULL COMMENT '文件指纹（用于查重）',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(Byte)',
  `media_type` tinyint NOT NULL COMMENT '媒体类型：1-图片，2-视频',
  `file_format` varchar(20) DEFAULT NULL COMMENT '扩展名 (jpg, mp4, arw等)',
  `shot_time` datetime DEFAULT NULL COMMENT '拍摄时间',
  `width` int DEFAULT NULL COMMENT '宽',
  `height` int DEFAULT NULL COMMENT '高',
  `duration` int DEFAULT NULL COMMENT '时长(秒)',
  `camera_make` varchar(100) DEFAULT NULL COMMENT '相机品牌',
  `camera_model` varchar(100) DEFAULT NULL COMMENT '相机型号',
  `lens_model` varchar(255) DEFAULT NULL COMMENT '镜头型号',
  `f_number` varchar(20) DEFAULT NULL COMMENT '光圈值',
  `exposure_time` varchar(20) DEFAULT NULL COMMENT '快门速度',
  `iso` int DEFAULT NULL COMMENT '感光度',
  `focal_length` int DEFAULT NULL COMMENT '等效焦距',
  `gps_latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `gps_longitude` decimal(11,7) DEFAULT NULL COMMENT '经度',
  `location_name` varchar(255) DEFAULT NULL COMMENT '语义化地点名',
  `thumbnail_path` varchar(1024) DEFAULT NULL COMMENT '缩略图路径',
  `directory_id` bigint NOT NULL COMMENT '所属监控目录ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_path` (`file_path`(768)),
  KEY `idx_shot_time` (`shot_time`),
  KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB COMMENT='媒体文件元数据表';

DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT '0' COMMENT '父标签ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_type` tinyint DEFAULT '1' COMMENT '类型：1-手动，2-AI生成',
  `category` varchar(20) DEFAULT NULL COMMENT '维度：人物、地点、食物等',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_type` (`name`, `tag_type`)
) ENGINE=InnoDB COMMENT='标签定义表';

DROP TABLE IF EXISTS `media_tag_relation`;
CREATE TABLE `media_tag_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `media_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `confidence` decimal(5,4) DEFAULT '1.0000' COMMENT '置信度(AI打标用)',
  PRIMARY KEY (`id`),
  KEY `idx_media` (`media_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB COMMENT='媒体标签关联表';

DROP TABLE IF EXISTS `album`;
CREATE TABLE `album` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  `album_type` tinyint DEFAULT '1' COMMENT '类型：1-普通相册，2-智能相册',
  `query_rule` text COMMENT '智能相册规则(JSON格式)',
  `cover_media_id` bigint DEFAULT NULL COMMENT '封面图ID',
  `create_user_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='相册表';

DROP TABLE IF EXISTS `system_task`;
CREATE TABLE `system_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_type` varchar(20) NOT NULL COMMENT '任务类型：SCAN, TRANSCODE, GIF, SLIDESHOW',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-排队，1-执行中，2-成功，3-失败',
  `progress` int DEFAULT '0' COMMENT '进度 0-100',
  `params` text COMMENT '任务参数(JSON)',
  `error_msg` text COMMENT '失败信息',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='系统异步任务记录表';
