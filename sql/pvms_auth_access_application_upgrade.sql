ALTER TABLE `sys_visitor_application`
  ADD COLUMN IF NOT EXISTS `user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID' AFTER `id`,
  ADD COLUMN IF NOT EXISTS `username` varchar(50) NULL DEFAULT NULL COMMENT '申请账号' AFTER `user_id`,
  ADD COLUMN IF NOT EXISTS `role_key` varchar(40) NULL DEFAULT NULL COMMENT '审核后绑定角色' AFTER `expire_time`,
  ADD COLUMN IF NOT EXISTS `audit_username` varchar(50) NULL DEFAULT NULL COMMENT '审核人' AFTER `role_key`,
  ADD COLUMN IF NOT EXISTS `audit_remark` varchar(255) NULL DEFAULT NULL COMMENT '审核备注' AFTER `audit_username`;

UPDATE `sys_role`
SET `menu_keys` = 'library,projects,album-tags,favorites,users,roles,applications'
WHERE `role_key` = 'ADMIN';
