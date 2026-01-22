# Rollback Scripts

This directory contains manual rollback scripts for Flyway migrations.

## Important Notes

- **These scripts are NOT tracked by Flyway** - they are stored outside the `migration` directory
- **Execute manually** when you need to rollback a migration
- **Use with caution** - rollbacks can cause data loss

## How to Use

### Execute Rollback Script

```bash
# Using psql command line
psql -h localhost -U <username> -d <database> -f src/main/resources/db/rollback/rollback_v2_remove_phone_number.sql

# Or connect to database and run the SQL directly
psql -h localhost -U <username> -d <database>
\i src/main/resources/db/rollback/rollback_v2_remove_phone_number.sql
```

### Available Rollback Scripts

- `rollback_v2_remove_phone_number.sql` - Rollback for V2\_\_add_phone_number_to_tmc_user.sql
  - Removes the `phone_number` column from `tmc_user` table
  - **WARNING**: This permanently deletes the column and all its data

## Workflow

1. **Forward Migration**: V2 runs automatically via Flyway when application starts
2. **If Rollback Needed**: Manually execute the corresponding rollback script from this directory
3. **Verify**: Check the database to confirm the rollback was successful

## Best Practices

- Always backup your database before executing rollback scripts
- Test rollback scripts in a development environment first
- Document any data that will be lost during rollback
- Consider data migration strategies if you need to preserve data
