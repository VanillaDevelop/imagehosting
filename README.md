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

In order to test certain parts of the apps functionality, for example rich embeds served by Discord via OpenGraph, 
it is recommended to tunnel the local development server to the public internet. Note that due to the way subdomains 
interact with this application (identifying the user space), the tunneled domain should be a fully qualified domain name, 
NOT a subdomain. For this reason, while the use of a service like `cloudflared` is recommended, you may need to register 
a unique domain name rather than relying on the free subdomains provided by such services. Please adjust `imagehosting.url` 
in `application-dev.properties` to match the domain you are using for tunneling, to avoid subdomain conflicts.

Sample cloudflared config.yml for testing: 
```yaml
tunnel: [tunnel-id]
credentials-file: /home/vanilla/.cloudflared/[tunnel-id].json
ingress:
- hostname: vanilla.tgirl.app # explicitly proxies the subdomain
  service: http://localhost:8080
  originRequest:
  httpHostHeader: vanilla.localhost # sets vanilla.localhost as the host header
- service: http://localhost:8080 # fallback to the main domain
```
 
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