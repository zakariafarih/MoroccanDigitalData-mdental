ALTER TABLE realms
    ADD COLUMN admin_tmp_password     VARCHAR(255),
  ADD COLUMN service_client_id      VARCHAR(100),
  ADD COLUMN service_client_secret  VARCHAR(255);