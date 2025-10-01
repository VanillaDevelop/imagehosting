# Local Development
While reading this section, please refer to the `application-dev.properties` file for configuration options.

### 1. Database Setup
Local development requires a PostgreSQL database.   
Configure a user and ensure it owns the database.
```sql
ALTER DATABASE imagehosting OWNER TO imagehosting_user;
```
Then set up the database configuration according to `application-dev.properties`

Migrations are applied automatically on startup, but you can also run them manually using flyway as part of gradle:
```bash
./gradlew flywayMigrate
```

### 2. S3-Compatible Storage
In local development, MinIO is used for S3-compatible storage, but you can substitute it with any S3-compatible service.
You can run MinIO in a container, for example using the following command:
```bash
podman run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" -e "MINIO_ROOT_PASSWORD=minioadmin" \
  -v /home/vanilla/Code/.imagehosting/minio:/data quay.io/minio/minio \
  server /data --console-address ":9001"
```
In MinIO, access-key and secret-key need to match the username and password you define when running the container. 
The mounted volume should be persistent to avoid losing data on container restarts. The endpoint port must also match 
the mapped port of the container. Finally, the console port is used to access the MinIO web interface. 
Ensure your MinIO instance has a storage container configured with the name defined in `application-dev.properties` (default is `nya.gg`). 
You can do this via the web interface at `http://localhost:9001` using the credentials defined above. Then ensure that 
the remaining configuration options match.

### 3. FFmpeg Setup
This application uses FFmpeg for video processing. The easiest way to satisfy this requirement is to simply place the binary `ffmpeg` and 
`ffprobe` files in a directory that is accessible to the application, or install them via your package manager. 
You can find the binary files [here](https://www.ffmpeg.org/download.html).

Then, set the `media.ffmpeg.path` and `media.ffprobe.path` variables in `application-dev.properties` to point to the respective binaries.

### 4. Local Directories
The application stores some data directly on the local filesystem. You need to create these directories and ensure the 
application has read and write access to them, then set the variables in `application-dev.properties` accordingly.

`media.temp.directory` is used to store temporary files during video processing. These files should be deleted automatically 
after processing, so it is not important that data in this directory is persistent. However, while the application is running, 
the contents or access to this directory should not be modified.

`thumbnails.storage.directory` is used to store generated thumbnails. The thumbnails are generally relatively small, but 
must be persisted as they are not stored on the cloud.


### 5. Testing Rich Embeds With Tunneling
**This section is only relevant if you want to serve the content via the internet from your local machine (e.g. to test rich embeds). 
Full functionality is available locally without tunneling otherwise.**

In order to test certain parts of the apps functionality, you might have to tunnel the local development server to the public internet. 
For example, to test rich embeds on Discord or Twitter, the service needs to be able to access the content. In this case, you 
want to use a tunneling service to expose your local server to the internet.

Note that this application relies on subdomains for user identification, which may not be compatible with free tunneling services 
that provide shared subdomains. For example, if you use a free subdomain like `example.tunnelprovider.com`, you are able to define 
`example.tunnelprovider.com` in `application-dev.properties` under `imagehosting.url` and access the website as expected. However, a user URL would be 
`username.example.tunnelprovider.com`, and free tunneling service likely will not resolve this subdomain to your local server correctly.

For this reason, while the use of a service like `cloudflared` is recommended, you may need to register 
a unique domain name rather than relying on the free subdomains provided by such services. Please adjust `imagehosting.url` 
in `application-dev.properties` to match the domain you are using for tunneling.

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

More information can be found [here](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/local-management/create-local-tunnel/). 
The relevant command to launch the tunnel after setup is: 
```bash
cloudflared tunnel run [UUID/Name]
```

### 6. Running the Application
In development, you can simply directly run a spring boot debugger from your IDE of choice.
Initially, you will have to use gradle to download dependencies.
```bash
./gradlew build
```
Then, you can run the application via your IDE.  

The application will be available at `http://localhost:8080` by default.
 
# Modules
## User Management (TODO - This Section Requires Cleanup)
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
## Video Upload Module
A user with the `VIDEO_UPLOAD` role can upload videos for public access. This role can be granted via the user management page, 
and grants access to the `videoupload.xhtml` page. On first access to this page, a `VideoUploadUser` entity is created for the user, 
which stores his preferred upload settings. 

From there, the user can use the UI to upload videos, optionally setting the beginning and end timestamp of the video. All of this 
is done via the UI via a JavaScript-based trimmer. A title must be provided, but this title does not have to be unique. 

When the form in the background is submitted through the user pressing the "Trim And Upload" button, the video is uploaded to the server, 
locally stored in a temporary directory, processed with ffmpeg (conversion to mp4, trimming, thumbnail generation) 
and then uploaded to the S3-compatible storage. At this point, local files are cleaned up and a database entry is created for the video.

Should the upload fail, a job will later try to remove the temporary files again, and update the database entry to reflect the failed upload. 
The user will be shown a message under the provided upload URL that the upload failed.

The user can view all of his uploaded videos on the `videolibrary.xhtml` page, which can be accessed via a button on the upload page. The 
library is paginated and shows the thumbnails generated during the upload. 

The video can be accessed in two ways: The generated URL (e.g. `https://vanilla.nya.gg/v/abcd1234`) will show a simple page with the video embedded, 
and will also provide OpenGraph data for rich embeds. The direct URL (e.g. `https://vanilla.nya.gg/v/abcd1234.mp4`) will serve the video file directly.

The following features are currently not implemented, and **may** be added in the future:
- Video deletion
- Changing video title
- Video privacy settings (public/unlisted/private)
- Mass import of existing videos