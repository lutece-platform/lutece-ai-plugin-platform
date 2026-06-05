-- liquibase formatted sql
-- changeset lutece-global-pom:init_core_platform.sql
-- preconditions onFail:MARK_RAN onError:WARN


--
-- Data for table core_admin_right
--
DELETE FROM core_admin_right WHERE id_right = 'PLATFORM_CONFIG_MANAGEMENT';
INSERT INTO core_admin_right (id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order)
VALUES ('PLATFORM_CONFIG_MANAGEMENT', 'platform.adminFeature.ManageResourceConfig.name', 1, 'jsp/admin/plugins/platform/ManageResourceConfig.jsp', 'platform.adminFeature.ManageResourceConfig.description', 0, 'platform', NULL, NULL, NULL, 5);

DELETE FROM core_user_right WHERE id_right = 'PLATFORM_CONFIG_MANAGEMENT';
INSERT INTO core_user_right (id_right, id_user) VALUES ('PLATFORM_CONFIG_MANAGEMENT', 1);


DELETE FROM core_admin_right WHERE id_right = 'PLATFORM_SUBSCRIPTION_MANAGEMENT';
INSERT INTO core_admin_right (id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order)
VALUES ('PLATFORM_SUBSCRIPTION_MANAGEMENT', 'platform.adminFeature.ManageSubscriptions.name', 1, 'jsp/admin/plugins/platform/ManageSubscriptions.jsp', 'platform.adminFeature.ManageSubscriptions.description', 0, 'platform', NULL, NULL, NULL, 6);

DELETE FROM core_user_right WHERE id_right = 'PLATFORM_SUBSCRIPTION_MANAGEMENT';
INSERT INTO core_user_right (id_right, id_user) VALUES ('PLATFORM_SUBSCRIPTION_MANAGEMENT', 1);
