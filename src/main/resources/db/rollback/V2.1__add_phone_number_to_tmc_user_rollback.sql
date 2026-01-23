
COMMENT ON COLUMN tmc_user.phone_number IS NULL;


ALTER TABLE tmc_user
DROP COLUMN IF EXISTS phone_number;

