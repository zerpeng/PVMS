/*
 Navicat Premium Dump SQL

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : pvms_media

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 08/05/2026 14:42:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for album
-- ----------------------------
DROP TABLE IF EXISTS `album`;
CREATE TABLE `album`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `album_type` tinyint NULL DEFAULT 1 COMMENT '类型：1-普通相册，2-智能相册',
  `query_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '智能相册规则(JSON格式)',
  `cover_media_id` bigint NULL DEFAULT NULL COMMENT '封面图ID',
  `create_user_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '相册表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for media_album_relation
-- ----------------------------
DROP TABLE IF EXISTS `media_album_relation`;
CREATE TABLE `media_album_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `media_id` bigint NOT NULL,
  `album_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_media`(`media_id` ASC) USING BTREE,
  INDEX `idx_album`(`album_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '媒体相册关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for media_file
-- ----------------------------
DROP TABLE IF EXISTS `media_file`;
CREATE TABLE `media_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件绝对路径',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件指纹（用于查重）',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小(Byte)',
  `media_type` tinyint NOT NULL COMMENT '媒体类型：1-图片，2-视频',
  `file_format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扩展名 (jpg, mp4, arw等)',
  `shot_time` datetime NULL DEFAULT NULL COMMENT '拍摄时间',
  `width` int NULL DEFAULT NULL COMMENT '宽',
  `height` int NULL DEFAULT NULL COMMENT '高',
  `duration` int NULL DEFAULT NULL COMMENT '时长(秒)',
  `camera_make` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相机品牌',
  `camera_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相机型号',
  `lens_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '镜头型号',
  `f_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '光圈值',
  `exposure_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '快门速度',
  `iso` int NULL DEFAULT NULL COMMENT '感光度',
  `focal_length` int NULL DEFAULT NULL COMMENT '等效焦距',
  `gps_latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `gps_longitude` decimal(11, 7) NULL DEFAULT NULL COMMENT '经度',
  `location_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语义化地点名',
  `thumbnail_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图路径',
  `directory_id` bigint NOT NULL COMMENT '所属监控目录ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_path`(`file_path`(768) ASC) USING BTREE,
  INDEX `idx_shot_time`(`shot_time` ASC) USING BTREE,
  INDEX `idx_file_hash`(`file_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '媒体文件元数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for media_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `media_tag_relation`;
CREATE TABLE `media_tag_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `media_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `confidence` decimal(5, 4) NULL DEFAULT 1.0000 COMMENT '置信度(AI打标用)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_media`(`media_id` ASC) USING BTREE,
  INDEX `idx_tag`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '媒体标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitor_directory
-- ----------------------------
DROP TABLE IF EXISTS `monitor_directory`;
CREATE TABLE `monitor_directory`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dir_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '本地绝对路径',
  `is_recursive` tinyint NULL DEFAULT 1 COMMENT '是否递归子目录',
  `scan_status` tinyint NULL DEFAULT 0 COMMENT '扫描状态：0-待机，1-扫描中',
  `last_scan_time` datetime NULL DEFAULT NULL COMMENT '最后一次完整扫描时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_path`(`dir_path`(768) ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '监控目录配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for system_task
-- ----------------------------
DROP TABLE IF EXISTS `system_task`;
CREATE TABLE `system_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：SCAN, TRANSCODE, GIF, SLIDESHOW',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-排队，1-执行中，2-成功，3-失败',
  `progress` int NULL DEFAULT 0 COMMENT '进度 0-100',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务参数(JSON)',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败信息',
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统异步任务记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_media_preference
-- ----------------------------
DROP TABLE IF EXISTS `user_media_preference`;
CREATE TABLE `user_media_preference`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `sync_scan_enabled` tinyint NULL DEFAULT 0 COMMENT '新增监控目录后是否同步启动扫描：0-关闭，1-开启',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户媒体偏好设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父标签ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `tag_type` tinyint NULL DEFAULT 1 COMMENT '类型：1-手动，2-AI生成',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '维度：人物、地点、食物等',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name_type`(`name` ASC, `tag_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签定义表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
