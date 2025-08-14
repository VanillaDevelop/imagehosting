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

In local development, MinIO is used for S3-compatible storage.
You can run MinIO in a container, for example using the following command:
```bash
podman run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" -e "MINIO_ROOT_PASSWORD=minioadmin" \
  -v /home/vanilla/Code/.imagehosting/minio:/data quay.io/minio/minio \
  server /data --console-address ":9001"
```

Ensure that the `application-dev.properties` variables match your MinIO configuration. access-key and secret-key 
need to match the username and password you set in the MinIO container. The mounted volume should be 
persistent to avoid losing data on container restarts. The endpoint port must also match the mapped port of the container.
Finally, the console port is used to access the MinIO web interface. Ensure your MinIO instance has a storage container 
configured with the name defined in `application-dev.properties` (default is `nya.gg`).


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