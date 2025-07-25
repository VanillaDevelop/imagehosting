CREATE TABLE IF NOT EXISTS video_upload_users
(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    mode VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS video_upload_user_files
(
    id SERIAL PRIMARY KEY,
    video_upload_user_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (video_upload_user_id) REFERENCES video_upload_users (id)

);

INSERT INTO roles (role)
VALUES ('VIDEO_UPLOAD');