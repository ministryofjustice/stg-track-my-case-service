# Rollback Scripts

This directory contains rollback scripts for Flyway migrations. These scripts are stored separately from the main migration directory and are moved to the migration directory only when a rollback is needed.

## Strategy Overview

### Forward Migration Process

1. **Create forward migration** in `db/migration/` directory (e.g., `V2__add_phone_number_to_tmc_user.sql`)
2. **Create corresponding rollback script** in `db/rollback/` directory (e.g., `V2.1__add_phone_number_to_tmc_user_rollback.sql`)
3. When the application starts, Flyway automatically runs the forward migration from `migration/` directory

### Rollback Process

When a rollback is needed:

1. **Create a new branch** for the rollback
2. **Move the rollback script** from `db/rollback/` to `db/migration/` directory
3. **Run the release build** - Flyway will automatically detect and execute the rollback migration

## Directory Structure

```
src/main/resources/db/
├── migration/                          (Tracked by Flyway - runs automatically)
│   ├── V1__create_tmc_user_table.sql
│   └── V2__add_phone_number_to_tmc_user.sql
│
└── rollback/                           (Storage for rollback scripts - NOT tracked by Flyway)
    ├── README.md
    └── V2.1__add_phone_number_to_tmc_user_rollback.sql
```

## Step-by-Step Rollback Workflow

### Step 1: Create Rollback Branch from the release braanch(when V2 was) released

### Step 2: Move Rollback Script to Migration Directory

```bash
# Move the rollback script from rollback/ to migration/
mv src/main/resources/db/rollback/V2.1__add_phone_number_to_tmc_user_rollback.sql \
   src/main/resources/db/migration/V2.1__add_phone_number_to_tmc_user_rollback.sql
```

### Step 3: builkd and run the Application

### Step 4: Verify Rollback

## Available Rollback Scripts

- `V2.1__add_phone_number_to_tmc_user_rollback.sql` - Rollback for `V2__add_phone_number_to_tmc_user.sql`
  - Removes the `phone_number` column from `tmc_user` table
  - **WARNING**: This permanently deletes the column and all its data

## Naming Convention

Rollback scripts follow this naming pattern:

- Forward migration: `V2__add_phone_number_to_tmc_user.sql`
- Rollback script: `V2.1__add_phone_number_to_tmc_user_rollback.sql`

The version number (V2.1) should be:

- Higher than the forward migration version (V2)
- Lower than the next forward migration version (V3)
- Include `.1` to indicate it's a rollback/patch for the previous version

## Important Notes

- **Rollback scripts in `rollback/` directory are NOT tracked by Flyway** - they are stored separately
- **Only when moved to `migration/` directory** will Flyway execute them automatically
- **Always create a separate branch** for rollback to maintain clear version control history
- **Test rollback in development/staging** before applying to production
- **Backup database** before executing rollback in production

## Best Practices

1. **Always backup your database** before executing rollback migrations
2. **Test rollback in development environment** first
3. **Document any data that will be lost** during rollback

## Example: Complete Rollback Scenario

### Initial State

- `db/migration/V2__add_phone_number_to_tmc_user.sql` (already applied)
- `db/rollback/V2.1__add_phone_number_to_tmc_user_rollback.sql` (stored, not applied)

### Rollback script not executing

- Ensure the script is in `db/migration/` directory (not `db/rollback/`)
- Check Flyway migration history: `SELECT * FROM flyway_schema_history;`
- Verify script naming follows Flyway convention: `V{version}__{description}.sql`

### Version conflict

- Ensure rollback version (e.g., V2.1) is between forward migration versions
- If V3 already exists, use V2.1, V2.2, etc. for rollbacks
