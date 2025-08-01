# Local Development
Local development requires a PostgreSQL database on the default port (5432).  
Configure a user according to `application-dev.properties`, and ensure it owns the database.
```sql
ALTER DATABASE imagehosting OWNER TO imagehosting_user;
```

Migrations are applied automatically on startup, but you can also run them manually using flyway as part of gradle:
```bash
./gradlew flywayMigrate
```

Both in development and production, AWS access is configured automatically via the credentials file. On linux,
place this file at `~/.aws/credentials` with the following content:
```
[default]
aws_access_key_id = [your_access_key_id]
aws_secret_access_key = [your_secret_access_key]
```

The user should have `AmazonS3FullAccess` permissions to allow the application to upload files to S3. This type of 
user can be configured in the IAM console of AWS, not to confuse with "IAM Identity Center", which for some reason 
is something else entirely.

# User Management
A user can be created easily via the login page.  
A user with the admin role can view and control the roles of other users.  
To create an initial admin user, you can run the following SQL command in your PostgreSQL database:

```sql
INSERT INTO public.user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM public.users WHERE username = '[username]'),
    1
);
```

# Modules
## Video Upload Module
A user with the `VIDEO_UPLOAD` role can upload videos for public access. 