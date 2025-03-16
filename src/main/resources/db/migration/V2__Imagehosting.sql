CREATE TABLE IF NOT EXISTS image_hosting_users
(
    id      SERIAL PRIMARY KEY,
    user_id INT          NOT NULL,
    mode    VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS image_hosting_user_files
(
    id                    SERIAL PRIMARY KEY,
    image_hosting_user_id INT          NOT NULL,
    file_name             VARCHAR(255) NOT NULL,
    file_size             BIGINT       NOT NULL,
    created_at            TIMESTAMP    NOT NULL,
    FOREIGN KEY (image_hosting_user_id) REFERENCES image_hosting_users (id)
)