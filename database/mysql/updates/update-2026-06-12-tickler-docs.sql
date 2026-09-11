-- Issue #2476: Migrate Tickler to the Consult/EForm document attachment component.
--
-- Introduces the `ticklerdocs` table (mirrors `consultdocs`/`EFormDocs`) which replaces
-- the legacy `tickler_link` table as the Tickler attachment store. Existing `tickler_link`
-- rows are copied into `ticklerdocs` so historical attachments remain visible.
--
-- Legacy `tickler_link.table_name` (char(3)) -> new `ticklerdocs.doctype` (char(1)) mapping:
--   DOC                 -> D (document)
--   HRM                 -> H (hospital report)
--   HL7, MDS, CML, BCP  -> L (lab)  (and any other lab source code)
--
-- The `tickler_link` table itself is intentionally left in place (REST/Integrator code
-- still references it); it is simply no longer used by the Tickler UI.

CREATE TABLE IF NOT EXISTS `ticklerdocs` (
  `id` int(10) NOT NULL auto_increment PRIMARY KEY,
  `tickler_id` int(10) NOT NULL,
  `document_no` int(10) NOT NULL,
  `doctype` char(1) NOT NULL,
  `deleted` char(1) DEFAULT NULL,
  `attach_date` date,
  `provider_no` varchar(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS `idx_ticklerdocs_tickler_id` ON `ticklerdocs` (`tickler_id`);

-- Backfill existing attachments from tickler_link.
-- The match deliberately ignores `deleted`: an attachment that was backfilled and later
-- detached must not be resurrected by a second run.
INSERT INTO `ticklerdocs` (`tickler_id`, `document_no`, `doctype`, `deleted`, `attach_date`, `provider_no`)
SELECT
  src.`tickler_no`,
  src.`table_id`,
  src.`doctype`,
  NULL,
  CURDATE(),
  ''
FROM (
  SELECT DISTINCT
    tl.`tickler_no`,
    tl.`table_id`,
    CASE
      WHEN tl.`table_name` = 'DOC' THEN 'D'
      WHEN tl.`table_name` = 'HRM' THEN 'H'
      ELSE 'L'
    END AS `doctype`
  FROM `tickler_link` tl
) src
WHERE NOT EXISTS (
  SELECT 1
  FROM `ticklerdocs` td
  WHERE td.`tickler_id`  = src.`tickler_no`
    AND td.`document_no` = src.`table_id`
    AND td.`doctype`     = src.`doctype`
);