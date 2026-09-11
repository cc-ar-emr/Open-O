-- =====================================================================
-- OpenO EMR - OntarioMD EHR Connectivity - schema, config, permissions
-- =====================================================================

-- One audit row per gateway, context and viewlet call

CREATE TABLE IF NOT EXISTS `OMDGatewayTransactionLog` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `started` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ended` timestamp NULL DEFAULT NULL,
  `initiatingProviderNo` varchar(25) DEFAULT NULL,
  `transactionType` varchar(50) DEFAULT NULL,
  `externalSystem` varchar(50) DEFAULT NULL,
  `demographicNo` int(10) DEFAULT NULL,
  `resultCode` int(10) DEFAULT NULL,
  -- The EHR service's own outcome code, for example IN_0045 or CONSENT_EXISTS. resultCode holds
  -- the HTTP status, which says a call was refused but not why; this holds the reason as a value
  -- that can be filtered on. Values come from the published ca.on.ehr.r4 CodeSystem-ErrorCode.
  `ehrResultCode` varchar(255) DEFAULT NULL,
  -- Resource ids from the FHIR message an interaction carried. 64 characters is the FHIR id
  -- primitive's own limit, so no conformant id can be longer. One transaction covers a search
  -- that returns many dispenses, so those are held as a comma separated list, which is how they
  -- are read: nothing queries or counts on them.
  `messageHeaderId` varchar(64) DEFAULT NULL,
  `medicationDispenseIds` text,
  `success` tinyint(1) DEFAULT NULL,
  `error` longtext,
  `headers` longtext,
  `uao` varchar(50) DEFAULT NULL,
  `oscarSessionId` varchar(50) DEFAULT NULL,
  `contextSessionId` varchar(50) DEFAULT NULL,
  `secondsLeft` int(10) DEFAULT NULL,
  `dataSent` longtext,
  `uniqueSessionId` varchar(50) DEFAULT NULL,
  `dataRecieved` longtext,
  `xRequestId` varchar(50) DEFAULT NULL,
  `xLobTxId` varchar(50) DEFAULT NULL,
  `xCorrelationId` varchar(50) DEFAULT NULL,
  `xGtwyClientId` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `OMDGatewayTransactionLog_uniqueSessionId_idx` (`uniqueSessionId`(40)),
  KEY `OMDGatewayTransactionLog_provider_idx` (`initiatingProviderNo`),
  KEY `OMDGatewayTransactionLog_ehrResultCode_idx` (`ehrResultCode`(64))
);

-- Persisted ONE ID session (tokens/toolbar/UAO/hubTopic per provider)
CREATE TABLE IF NOT EXISTS `OneIdSession` (
  `providerNo` varchar(25) NOT NULL,          -- provider number (session owner)
  `refreshToken` varchar(2048) DEFAULT NULL,
  `idToken` varchar(8192) DEFAULT NULL,
  `accessToken` blob,
  `subject` varchar(128) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `uaoName` varchar(255) DEFAULT NULL,
  `uaoUpi` varchar(64) DEFAULT NULL,
  `serviceEntitlements` text,
  `authorizationId` varchar(1024) DEFAULT NULL,
  `hubTopic` varchar(255) DEFAULT NULL,
  `timestamp` bigint(20) NOT NULL DEFAULT 0,
  `lastKeptActive` datetime DEFAULT NULL,
  `sso` tinyint(1) DEFAULT 0,
  `toolbar` blob DEFAULT NULL,
  PRIMARY KEY (`providerNo`)
);

-- Configurable list of viewlets
CREATE TABLE IF NOT EXISTS `OneIdViewlet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `keyValue` varchar(50) NOT NULL,
  `updatedBy` varchar(10) DEFAULT NULL,
  `updateTime` datetime DEFAULT NULL,
  `showInEchart` tinyint(1) DEFAULT 1,
  `deleted` tinyint(1) DEFAULT 0,
  `displayMode` varchar(20) NOT NULL DEFAULT 'non-modal',   -- 'modal' or 'non-modal' interface per viewlet
  PRIMARY KEY (`id`)
);


-- "Under Authority Of" values (one row per provider/UAO assignment)
CREATE TABLE IF NOT EXISTS `UAO` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `providerNo` varchar(25) DEFAULT NULL,
  `friendlyName` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `postal` varchar(25) DEFAULT NULL,
  `defaultUAO` tinyint(1) DEFAULT NULL,
  `active` tinyint(1) DEFAULT NULL,
  `addedBy` varchar(25) DEFAULT NULL,
  `dateCreated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dateUpdated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `UAO_provider_name_idx` (`providerNo`,`name`)
);


-- The gateway settings do not fit the column's 255 characters: a PEM encoded 2048-bit public key
-- is around 450, and an endpoint URL carrying query parameters can be longer than 255 too. The
-- settings screen writes one row at a time, so a value the column refuses throws part way through
-- the list and leaves the rest of the screen unsaved.
ALTER TABLE `SystemPreferences` MODIFY COLUMN `value` VARCHAR(1000) NULL;

-- Gateway configuration (entered on the admin settings screen; secrets left blank)
INSERT INTO `SystemPreferences` (`name`,`value`,`updateDate`) VALUES
  ('oag_client_id','',NOW()),
  ('oag_client_secret','',NOW()),
  ('oag_public_key','',NOW()),                 -- OAG public key from enrollment
  ('keystore_path','',NOW()),
  ('keystore_alias','',NOW()),
  ('keystore_password','',NOW()),
  ('endpoint_authorize','',NOW()),
  ('endpoint_access_token','',NOW()),
  ('endpoint_callback','',NOW()),
  ('endpoint_audience','',NOW()),
  ('endpoint_jwks','',NOW()),                  -- JWKS URI for id-token signature validation
  ('oneid_issuer','',NOW()),                   -- expected id-token issuer
  ('endpoint_end_session','',NOW()),           -- OIDC end-session endpoint
  ('endpoint_revocation','',NOW()),            -- token revocation endpoint
  ('pcoi_key','',NOW()),
  ('timeout','65',NOW()),                      -- gateway connection/read timeout (seconds)
  ('viewlet_timeout','65',NOW()),              -- viewlet response wait time (seconds)
  ('oneid_enabled','false',NOW()),             -- shows the ONE ID login button
  ('dhdr_enabled','false',NOW());              -- shows the DHDR medication viewer


-- Security objects
--   _admin.ehrConnectivity -> admin gateway / viewlet / UAO screens
--   _ehr.connectivity      -> provider-facing use (login / launch / UAO / logout)
INSERT INTO `secObjectName` (`objectName`) VALUES ('_admin.ehrConnectivity');
INSERT INTO `secObjectName` (`objectName`) VALUES ('_ehr.connectivity');

-- 'x' grants full access
INSERT INTO `secObjPrivilege` VALUES ('admin','_admin.ehrConnectivity','x',0,999998);
INSERT INTO `secObjPrivilege` VALUES ('admin','_ehr.connectivity','x',0,999998);
INSERT INTO `secObjPrivilege` VALUES ('doctor','_ehr.connectivity','x',0,999998);
INSERT INTO `secObjPrivilege` VALUES ('nurse','_ehr.connectivity','x',0,999998);
INSERT INTO `secObjPrivilege` VALUES ('locum','_ehr.connectivity','x',0,999998);
INSERT INTO `secObjPrivilege` VALUES ('psychiatrist','_ehr.connectivity','x',0,999998);
