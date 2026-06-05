-- liquibase formatted sql
-- changeset lutece-global-pom:create_db_platform.sql
-- preconditions onFail:MARK_RAN onError:WARN

-- =============================================================================
-- LuteceAI platform — full schema (regenerated from a clean DB dump)
-- All tables created in one pass. Foreign key checks disabled during creation
-- so the alphabetical ordering doesn't matter.
-- =============================================================================


CREATE TABLE platform_decision_node (
  id_decision_node int NOT NULL AUTO_INCREMENT,
  tree_id int NOT NULL,
  node_title varchar(255) DEFAULT NULL,
  show_back_button SMALLINT DEFAULT 0 NOT NULL,
  back_target_node_id int DEFAULT NULL,
  content LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_decision_node)
);

CREATE TABLE platform_decision_transition (
  id_decision_transition int NOT NULL AUTO_INCREMENT,
  source_node_id int NOT NULL,
  target_node_id int NOT NULL,
  label varchar(500) DEFAULT NULL,
  sort_order int DEFAULT 0 NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_decision_transition)
);

CREATE TABLE platform_bot_pipeline (
  id int NOT NULL AUTO_INCREMENT,
  id_bot int NOT NULL,
  id_pipeline int NOT NULL,
  tool_description LONG VARCHAR DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_bot_pipeline UNIQUE (id_bot,id_pipeline)
);
CREATE INDEX idx_bot_pipeline_bot ON platform_bot_pipeline (id_bot);
CREATE INDEX idx_bot_pipeline_pipeline ON platform_bot_pipeline (id_pipeline);

CREATE TABLE platform_decision_tree_conversation_step (
  id int NOT NULL AUTO_INCREMENT,
  conversation_id varchar(36) NOT NULL,
  node_id int NOT NULL,
  node_title varchar(255) DEFAULT NULL,
  chosen_transition_id int DEFAULT NULL,
  step_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_dt_step_conversation ON platform_decision_tree_conversation_step (conversation_id);

CREATE TABLE platform_bot_conversation (
  id int NOT NULL AUTO_INCREMENT,
  bot_id int NOT NULL,
  conversation_uuid varchar(36) NOT NULL,
  user_id varchar(255) NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_conversation_uuid UNIQUE (conversation_uuid)
);
CREATE INDEX idx_conversation_bot ON platform_bot_conversation (bot_id);
CREATE INDEX idx_conversation_user ON platform_bot_conversation (user_id);
CREATE INDEX idx_conversation_bot_user ON platform_bot_conversation (bot_id,user_id);
CREATE INDEX idx_conversation_created ON platform_bot_conversation (created_at);

CREATE TABLE platform_pipeline (
  id_pipeline int NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  description LONG VARCHAR DEFAULT NULL,
  max_concurrent_workers int DEFAULT 1 NOT NULL,
  rate_limit_by_user_by_day int DEFAULT 0,
  id_client int NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_pipeline)
);
CREATE INDEX idx_pipeline_client ON platform_pipeline (id_client);
CREATE INDEX idx_pipeline_name_client ON platform_pipeline (name,id_client);

CREATE TABLE platform_dataset (
  dataset_id int NOT NULL AUTO_INCREMENT,
  dataset_name varchar(255) NOT NULL,
  dataset_description LONG VARCHAR DEFAULT NULL,
  embed_provider_id int NOT NULL,
  llm_provider_id int NOT NULL,
  client_id int NOT NULL,
  dataset_routing_rules LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (dataset_id)
);
CREATE INDEX idx_dataset_client ON platform_dataset (client_id);
CREATE INDEX idx_dataset_embed_provider ON platform_dataset (embed_provider_id);
CREATE INDEX idx_dataset_llm_provider ON platform_dataset (llm_provider_id);
CREATE INDEX idx_dataset_name_client ON platform_dataset (dataset_name,client_id);

CREATE TABLE platform_query_job (
  job_id int NOT NULL AUTO_INCREMENT,
  bot_id int NOT NULL,
  query LONG VARCHAR NOT NULL,
  conversation_uuid varchar(36) DEFAULT NULL,
  user_id varchar(255) DEFAULT NULL,
  status varchar(50) DEFAULT 'pending' NOT NULL,
  stream_id varchar(255) DEFAULT NULL,
  error_message LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (job_id)
);
CREATE INDEX idx_query_job_bot ON platform_query_job (bot_id);
CREATE INDEX idx_query_job_stream ON platform_query_job (stream_id);
CREATE INDEX idx_query_job_conversation ON platform_query_job (conversation_uuid);
CREATE INDEX idx_query_job_user ON platform_query_job (user_id);
CREATE INDEX idx_query_job_status ON platform_query_job (status);
CREATE INDEX idx_query_job_created ON platform_query_job (created_at);

CREATE TABLE platform_vision_extractor (
  extractor_id int NOT NULL AUTO_INCREMENT,
  vision_id int NOT NULL,
  extractor_name varchar(255) NOT NULL,
  extractor_description LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (extractor_id)
);
CREATE INDEX idx_extractor_vision ON platform_vision_extractor (vision_id);
CREATE INDEX idx_extractor_name_vision ON platform_vision_extractor (extractor_name,vision_id);

CREATE TABLE platform_bot (
  bot_id int NOT NULL AUTO_INCREMENT,
  bot_name varchar(255) NOT NULL,
  bot_description LONG VARCHAR DEFAULT NULL,
  bot_system_prompt LONG VARCHAR DEFAULT NULL,
  logo_base64 LONG VARCHAR DEFAULT NULL,
  welcome_message LONG VARCHAR DEFAULT NULL,
  client_id int NOT NULL,
  llm_provider_id int NOT NULL,
  embed_provider_id int NOT NULL,
  max_tokens int DEFAULT 4000 NOT NULL,
  temperature decimal(3,2) DEFAULT 0.70,
  rate_limit_by_user_by_day int DEFAULT 0,
  enable_content_aggregation SMALLINT DEFAULT 0,
  aggregation_max_results int DEFAULT 5,
  aggregation_strategy varchar(50) DEFAULT 'DEFAULT',
  query_enhancement_mode varchar(50) DEFAULT 'NONE',
  query_expansion_variants int DEFAULT 3,
  hyde_max_tokens int DEFAULT 500,
  hyde_prompt_template LONG VARCHAR DEFAULT NULL,
  semantic_search_max_results int DEFAULT 15,
  semantic_search_min_score decimal(3,2) DEFAULT 0.70,
  enable_builtin_tools SMALLINT DEFAULT 0,
  is_active SMALLINT DEFAULT 1,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (bot_id)
);
CREATE INDEX idx_bot_client ON platform_bot (client_id);
CREATE INDEX idx_bot_llm_provider ON platform_bot (llm_provider_id);
CREATE INDEX idx_bot_embed_provider ON platform_bot (embed_provider_id);
CREATE INDEX idx_bot_name_client ON platform_bot (bot_name,client_id);
CREATE INDEX idx_bot_active ON platform_bot (is_active);

CREATE TABLE platform_dataset_document (
  document_id int NOT NULL AUTO_INCREMENT,
  document_name varchar(255) NOT NULL,
  document_description LONG VARCHAR DEFAULT NULL,
  document_file_key varchar(255) NOT NULL,
  dataset_id int NOT NULL,
  folder_id int DEFAULT NULL,
  full_content LONG VARCHAR DEFAULT NULL,
  use_document_intelligence SMALLINT DEFAULT 0,
  document_intelligence_provider_id int DEFAULT NULL,
  chunk_size int DEFAULT 1000,
  chunk_overlap int DEFAULT 200,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (document_id),
  CONSTRAINT uk_document_file_key UNIQUE (document_file_key)
);
CREATE INDEX idx_document_dataset ON platform_dataset_document (dataset_id);
CREATE INDEX idx_document_name_dataset ON platform_dataset_document (document_name,dataset_id);
CREATE INDEX idx_document_folder_id ON platform_dataset_document (dataset_id,folder_id);
CREATE INDEX idx_doc_intelligence ON platform_dataset_document (use_document_intelligence,document_intelligence_provider_id);

CREATE TABLE platform_pipeline_user_rate_limit (
  id int NOT NULL AUTO_INCREMENT,
  user_id varchar(255) NOT NULL,
  pipeline_id int NOT NULL,
  execution_count int DEFAULT 0 NOT NULL,
  date_first_execution timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  date_last_execution timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_pipeline_date UNIQUE (user_id,pipeline_id,date_first_execution)
);
CREATE INDEX idx_pipeline_rate_limit_pipeline ON platform_pipeline_user_rate_limit (pipeline_id);
CREATE INDEX idx_pipeline_rate_limit_date ON platform_pipeline_user_rate_limit (date_first_execution);

CREATE TABLE platform_provider (
  provider_id int NOT NULL AUTO_INCREMENT,
  provider_name varchar(255) NOT NULL,
  provider_description LONG VARCHAR DEFAULT NULL,
  provider_type varchar(50) NOT NULL,
  provider_vendor varchar(50) DEFAULT 'azure_openai',
  deployment_name varchar(255) DEFAULT NULL,
  deployment_model_name varchar(255) NOT NULL,
  deployment_endpoint varchar(255) DEFAULT NULL,
  deployment_api_version varchar(50) DEFAULT NULL,
  deployment_api_key varchar(255) NOT NULL,
  token_input_price_1m decimal(10,6) DEFAULT NULL,
  token_output_price_1m decimal(10,6) DEFAULT NULL,
  document_analysis_price_1000_pages decimal(10,4) DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (provider_id)
);
CREATE INDEX idx_provider_type ON platform_provider (provider_type);
CREATE INDEX idx_provider_vendor ON platform_provider (provider_vendor);
CREATE INDEX idx_provider_type_doc_intel ON platform_provider (provider_type,document_analysis_price_1000_pages);

CREATE TABLE platform_document_job (
  job_id int NOT NULL AUTO_INCREMENT,
  document_id int NOT NULL,
  dataset_id int NOT NULL,
  status varchar(50) DEFAULT 'pending' NOT NULL,
  error_message LONG VARCHAR DEFAULT NULL,
  progress_percentage SMALLINT DEFAULT 0,
  attempts int DEFAULT 0 NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (job_id)
);
CREATE INDEX idx_job_document ON platform_document_job (document_id);
CREATE INDEX idx_job_dataset ON platform_document_job (dataset_id);
CREATE INDEX idx_job_status ON platform_document_job (status);
CREATE INDEX idx_job_created ON platform_document_job (created_at);

CREATE TABLE platform_pipeline_execution (
  id_execution int NOT NULL AUTO_INCREMENT,
  execution_id varchar(255) NOT NULL,
  id_pipeline int NOT NULL,
  id_client int NOT NULL,
  user_id varchar(255) DEFAULT NULL,
  status varchar(50) DEFAULT 'PENDING' NOT NULL,
  date_creation timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  date_completion timestamp DEFAULT NULL,
  inputs LONG VARCHAR DEFAULT NULL,
  outputs LONG VARCHAR DEFAULT NULL,
  error LONG VARCHAR DEFAULT NULL,
  PRIMARY KEY (id_execution),
  CONSTRAINT uk_execution_id UNIQUE (execution_id)
);
CREATE INDEX idx_pipe_exec_pipeline ON platform_pipeline_execution (id_pipeline);
CREATE INDEX idx_pipe_exec_client ON platform_pipeline_execution (id_client);
CREATE INDEX idx_pipe_exec_status ON platform_pipeline_execution (status);
CREATE INDEX idx_pipe_exec_status_pipeline ON platform_pipeline_execution (status,id_pipeline);
CREATE INDEX idx_pipe_exec_creation ON platform_pipeline_execution (date_creation);

CREATE TABLE platform_decision_tree (
  id_decision_tree int NOT NULL AUTO_INCREMENT,
  tree_name varchar(255) NOT NULL,
  tree_description LONG VARCHAR DEFAULT NULL,
  client_id int NOT NULL,
  welcome_message LONG VARCHAR DEFAULT NULL,
  end_message LONG VARCHAR DEFAULT NULL,
  logo_base64 LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_decision_tree)
);

CREATE TABLE platform_bot_mcp_server (
  bot_id int NOT NULL,
  mcp_server_id int NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (bot_id,mcp_server_id)
);
CREATE INDEX idx_bot_mcp_server_server ON platform_bot_mcp_server (mcp_server_id);

CREATE TABLE platform_dataset_folder (
  folder_id int NOT NULL AUTO_INCREMENT,
  dataset_id int NOT NULL,
  parent_folder_id int DEFAULT NULL,
  folder_name varchar(255) NOT NULL,
  folder_description LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (folder_id),
  CONSTRAINT uk_folder_dataset_parent_name UNIQUE (dataset_id,parent_folder_id,folder_name)
);
CREATE INDEX idx_folder_dataset ON platform_dataset_folder (dataset_id);
CREATE INDEX idx_folder_parent ON platform_dataset_folder (parent_folder_id);

CREATE TABLE platform_subscription (
  id int NOT NULL AUTO_INCREMENT,
  id_client int NOT NULL,
  resource_type varchar(100) NOT NULL,
  resource_id varchar(100) NOT NULL,
  subscription_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  status varchar(50) DEFAULT 'active',
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_client_resource UNIQUE (id_client,resource_type,resource_id)
);
CREATE INDEX idx_subscription_client ON platform_subscription (id_client);
CREATE INDEX idx_subscription_status ON platform_subscription (status);
CREATE INDEX idx_subscription_type ON platform_subscription (resource_type);

CREATE TABLE platform_pipeline_trigger (
  id_trigger int NOT NULL AUTO_INCREMENT,
  id_pipeline int NOT NULL,
  id_client int NOT NULL,
  name varchar(255) NOT NULL,
  trigger_type varchar(50) DEFAULT 'CRON' NOT NULL,
  configuration LONG VARCHAR DEFAULT NULL,
  input_data LONG VARCHAR DEFAULT NULL,
  is_active SMALLINT DEFAULT 0 NOT NULL,
  last_triggered_at timestamp DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_trigger)
);
CREATE INDEX idx_trigger_pipeline ON platform_pipeline_trigger (id_pipeline);
CREATE INDEX idx_trigger_active ON platform_pipeline_trigger (is_active);

CREATE TABLE platform_mcp_server (
  mcp_server_id int NOT NULL AUTO_INCREMENT,
  mcp_server_name varchar(255) NOT NULL,
  mcp_server_description LONG VARCHAR DEFAULT NULL,
  transport_type varchar(50) NOT NULL,
  url varchar(512) NOT NULL,
  headers LONG VARCHAR DEFAULT NULL,
  timeout_ms int DEFAULT 60000,
  enabled SMALLINT DEFAULT 1,
  tool_filter varchar(1024) DEFAULT NULL,
  log_requests SMALLINT DEFAULT 0,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (mcp_server_id),
  CONSTRAINT uk_mcp_server_name UNIQUE (mcp_server_name)
);
CREATE INDEX idx_mcp_server_enabled ON platform_mcp_server (enabled);

CREATE TABLE platform_pipeline_version (
  id_version int NOT NULL AUTO_INCREMENT,
  id_pipeline int NOT NULL,
  version_name varchar(255) NOT NULL,
  description LONG VARCHAR DEFAULT NULL,
  flow LONG VARCHAR NOT NULL,
  is_current SMALLINT DEFAULT 0,
  input_schema LONG VARCHAR DEFAULT NULL,
  date_creation timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id_version),
  CONSTRAINT uk_pipeline_version_name UNIQUE (id_pipeline,version_name)
);
CREATE INDEX idx_version_pipeline ON platform_pipeline_version (id_pipeline);
CREATE INDEX idx_version_current ON platform_pipeline_version (id_pipeline,is_current);

CREATE TABLE platform_bot_dataset (
  bot_id int NOT NULL,
  dataset_id int NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (bot_id,dataset_id)
);
CREATE INDEX idx_bot_dataset_dataset ON platform_bot_dataset (dataset_id);

CREATE TABLE platform_observability_resource_node_execution (
  id_node_execution int NOT NULL AUTO_INCREMENT,
  execution_id varchar(255) NOT NULL,
  node_id varchar(255) NOT NULL,
  node_name varchar(255) DEFAULT NULL,
  status varchar(50) NOT NULL,
  start_time timestamp DEFAULT NULL,
  end_time timestamp DEFAULT NULL,
  duration_ms int DEFAULT NULL,
  input_data LONG VARCHAR DEFAULT NULL,
  output_data LONG VARCHAR DEFAULT NULL,
  error_message LONG VARCHAR DEFAULT NULL,
  execution_order int DEFAULT NULL,
  total_cost decimal(10,8) DEFAULT NULL,
  PRIMARY KEY (id_node_execution)
);
CREATE INDEX idx_node_execution_id ON platform_observability_resource_node_execution (execution_id);
CREATE INDEX idx_node_execution_node ON platform_observability_resource_node_execution (node_id);
CREATE INDEX idx_node_execution_order ON platform_observability_resource_node_execution (execution_id,execution_order);
CREATE INDEX idx_node_execution_start_time ON platform_observability_resource_node_execution (start_time);

CREATE TABLE platform_client (
  id int NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  code varchar(255) NOT NULL,
  description LONG VARCHAR DEFAULT NULL,
  active SMALLINT DEFAULT 1,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_client_code UNIQUE (code)
);
CREATE INDEX idx_client_active ON platform_client (active);

CREATE TABLE platform_conversation_message_feedback (
  id int NOT NULL AUTO_INCREMENT,
  message_id int NOT NULL,
  user_id varchar(255) NOT NULL,
  bot_id int NOT NULL,
  client_id int DEFAULT 0 NOT NULL,
  is_positive SMALLINT NOT NULL,
  comment LONG VARCHAR DEFAULT NULL,
  status varchar(50) DEFAULT 'pending' NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_feedback_message UNIQUE (message_id)
);
CREATE INDEX idx_feedback_message ON platform_conversation_message_feedback (message_id);
CREATE INDEX idx_feedback_user ON platform_conversation_message_feedback (user_id);
CREATE INDEX idx_feedback_bot ON platform_conversation_message_feedback (bot_id);
CREATE INDEX idx_feedback_client ON platform_conversation_message_feedback (client_id);
CREATE INDEX idx_feedback_status ON platform_conversation_message_feedback (status);
CREATE INDEX idx_feedback_positive ON platform_conversation_message_feedback (is_positive);
CREATE INDEX idx_feedback_created ON platform_conversation_message_feedback (created_at);
CREATE INDEX idx_feedback_bot_status ON platform_conversation_message_feedback (bot_id,status);

CREATE TABLE platform_bot_user_rate_limit (
  id int NOT NULL AUTO_INCREMENT,
  user_id varchar(255) NOT NULL,
  bot_id int NOT NULL,
  message_count int DEFAULT 0 NOT NULL,
  date_first_message timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  date_last_message timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_bot_date UNIQUE (user_id,bot_id,date_first_message)
);
CREATE INDEX idx_rate_limit_bot ON platform_bot_user_rate_limit (bot_id);
CREATE INDEX idx_rate_limit_date ON platform_bot_user_rate_limit (date_first_message);

CREATE TABLE platform_vision (
  vision_id int NOT NULL AUTO_INCREMENT,
  vision_title varchar(255) NOT NULL,
  vision_description LONG VARCHAR DEFAULT NULL,
  provider_id int NOT NULL,
  client_id int NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (vision_id)
);
CREATE INDEX idx_vision_provider ON platform_vision (provider_id);
CREATE INDEX idx_vision_client ON platform_vision (client_id);
CREATE INDEX idx_vision_title_client ON platform_vision (vision_title,client_id);

CREATE TABLE platform_decision_tree_image (
  content_hash varchar(64) NOT NULL,
  tree_id int NOT NULL,
  file_store_key varchar(255) NOT NULL,
  mime_type varchar(100) NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (content_hash,tree_id)
);
CREATE INDEX idx_decision_tree_image_tree ON platform_decision_tree_image (tree_id);

CREATE TABLE platform_observability_resource_node_trace (
  id_trace int NOT NULL AUTO_INCREMENT,
  id_node_execution int NOT NULL,
  message LONG VARCHAR DEFAULT NULL,
  data LONG VARCHAR DEFAULT NULL,
  timestamp timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  status varchar(50) DEFAULT NULL,
  cost decimal(10,8) DEFAULT NULL,
  PRIMARY KEY (id_trace)
);
CREATE INDEX idx_trace_node_execution ON platform_observability_resource_node_trace (id_node_execution);
CREATE INDEX idx_trace_timestamp ON platform_observability_resource_node_trace (timestamp);
CREATE INDEX idx_trace_status ON platform_observability_resource_node_trace (status);

CREATE TABLE platform_conversation_message (
  id int NOT NULL AUTO_INCREMENT,
  conversation_id int NOT NULL,
  message LONG VARCHAR DEFAULT NULL,
  role varchar(50) NOT NULL,
  metadata LONG VARCHAR DEFAULT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_message_conversation ON platform_conversation_message (conversation_id);
CREATE INDEX idx_message_role ON platform_conversation_message (role);
CREATE INDEX idx_message_created ON platform_conversation_message (created_at);

CREATE TABLE platform_model (
  model_id int NOT NULL AUTO_INCREMENT,
  client_id int NOT NULL,
  provider_id int NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (model_id),
  CONSTRAINT uk_client_provider UNIQUE (client_id,provider_id)
);
CREATE INDEX idx_model_client ON platform_model (client_id);
CREATE INDEX idx_model_provider ON platform_model (provider_id);

CREATE TABLE platform_decision_tree_conversation (
  conversation_id varchar(36) NOT NULL,
  tree_id int NOT NULL,
  client_id int NOT NULL,
  start_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  last_step_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (conversation_id)
);
CREATE INDEX idx_dt_conv_tree_client ON platform_decision_tree_conversation (tree_id,client_id);

CREATE TABLE platform_vision_extractor_field (
  field_id int NOT NULL AUTO_INCREMENT,
  extractor_id int NOT NULL,
  field_name varchar(255) NOT NULL,
  field_description LONG VARCHAR DEFAULT NULL,
  field_type varchar(50) NOT NULL,
  field_order SMALLINT DEFAULT 0,
  is_required SMALLINT DEFAULT 0,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (field_id)
);
CREATE INDEX idx_field_extractor ON platform_vision_extractor_field (extractor_id);
CREATE INDEX idx_field_name_extractor ON platform_vision_extractor_field (field_name,extractor_id);
CREATE INDEX idx_field_order ON platform_vision_extractor_field (extractor_id,field_order);

CREATE TABLE platform_observability_resource_execution (
  execution_id varchar(255) NOT NULL,
  resource_type varchar(255) NOT NULL,
  resource_id varchar(255) NOT NULL,
  client_id int DEFAULT NULL,
  status varchar(50) NOT NULL,
  start_time timestamp DEFAULT NULL,
  end_time timestamp DEFAULT NULL,
  duration_ms int DEFAULT NULL,
  error_message LONG VARCHAR DEFAULT NULL,
  input_data LONG VARCHAR DEFAULT NULL,
  output_data LONG VARCHAR DEFAULT NULL,
  total_cost decimal(10,8) DEFAULT NULL,
  PRIMARY KEY (execution_id)
);
CREATE INDEX idx_execution_resource ON platform_observability_resource_execution (resource_type,resource_id);
CREATE INDEX idx_execution_client ON platform_observability_resource_execution (client_id);
CREATE INDEX idx_execution_status ON platform_observability_resource_execution (status);
CREATE INDEX idx_execution_start_time ON platform_observability_resource_execution (start_time);
CREATE INDEX idx_execution_client_status ON platform_observability_resource_execution (client_id,status);

-- Foreign keys (re-added as portable ALTER statements, after all tables/indexes).
-- Restores referential integrity + ON DELETE cascades that the DB guarantees (HSQL & MariaDB).
ALTER TABLE platform_decision_node ADD CONSTRAINT fk_decision_node_tree FOREIGN KEY (tree_id) REFERENCES platform_decision_tree (id_decision_tree) ON DELETE CASCADE;
ALTER TABLE platform_decision_transition ADD CONSTRAINT fk_transition_source FOREIGN KEY (source_node_id) REFERENCES platform_decision_node (id_decision_node) ON DELETE CASCADE;
ALTER TABLE platform_decision_transition ADD CONSTRAINT fk_transition_target FOREIGN KEY (target_node_id) REFERENCES platform_decision_node (id_decision_node) ON DELETE CASCADE;
ALTER TABLE platform_bot_pipeline ADD CONSTRAINT fk_bot_pipeline_bot FOREIGN KEY (id_bot) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_bot_pipeline ADD CONSTRAINT fk_bot_pipeline_pipeline FOREIGN KEY (id_pipeline) REFERENCES platform_pipeline (id_pipeline) ON DELETE CASCADE;
ALTER TABLE platform_bot_conversation ADD CONSTRAINT fk_conversation_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_pipeline ADD CONSTRAINT fk_pipeline_client FOREIGN KEY (id_client) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_dataset ADD CONSTRAINT fk_dataset_client FOREIGN KEY (client_id) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_dataset ADD CONSTRAINT fk_dataset_embed_provider FOREIGN KEY (embed_provider_id) REFERENCES platform_provider (provider_id);
ALTER TABLE platform_dataset ADD CONSTRAINT fk_dataset_llm_provider FOREIGN KEY (llm_provider_id) REFERENCES platform_provider (provider_id);
ALTER TABLE platform_query_job ADD CONSTRAINT fk_query_job_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_vision_extractor ADD CONSTRAINT fk_extractor_vision FOREIGN KEY (vision_id) REFERENCES platform_vision (vision_id) ON DELETE CASCADE;
ALTER TABLE platform_bot ADD CONSTRAINT fk_bot_client FOREIGN KEY (client_id) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_bot ADD CONSTRAINT fk_bot_embed_provider FOREIGN KEY (embed_provider_id) REFERENCES platform_provider (provider_id);
ALTER TABLE platform_bot ADD CONSTRAINT fk_bot_llm_provider FOREIGN KEY (llm_provider_id) REFERENCES platform_provider (provider_id);
ALTER TABLE platform_dataset_document ADD CONSTRAINT fk_dataset_document_di_provider FOREIGN KEY (document_intelligence_provider_id) REFERENCES platform_provider (provider_id) ON DELETE SET NULL;
ALTER TABLE platform_dataset_document ADD CONSTRAINT fk_document_dataset FOREIGN KEY (dataset_id) REFERENCES platform_dataset (dataset_id) ON DELETE CASCADE;
ALTER TABLE platform_pipeline_user_rate_limit ADD CONSTRAINT fk_pipeline_rate_limit_pipeline FOREIGN KEY (pipeline_id) REFERENCES platform_pipeline (id_pipeline) ON DELETE CASCADE;
ALTER TABLE platform_document_job ADD CONSTRAINT fk_job_dataset FOREIGN KEY (dataset_id) REFERENCES platform_dataset (dataset_id) ON DELETE CASCADE;
ALTER TABLE platform_document_job ADD CONSTRAINT fk_job_document FOREIGN KEY (document_id) REFERENCES platform_dataset_document (document_id) ON DELETE CASCADE;
ALTER TABLE platform_pipeline_execution ADD CONSTRAINT fk_pipeline_execution_client FOREIGN KEY (id_client) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_pipeline_execution ADD CONSTRAINT fk_pipeline_execution_pipeline FOREIGN KEY (id_pipeline) REFERENCES platform_pipeline (id_pipeline) ON DELETE CASCADE;
ALTER TABLE platform_bot_mcp_server ADD CONSTRAINT fk_bot_mcp_server_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_bot_mcp_server ADD CONSTRAINT fk_bot_mcp_server_server FOREIGN KEY (mcp_server_id) REFERENCES platform_mcp_server (mcp_server_id) ON DELETE CASCADE;
ALTER TABLE platform_dataset_folder ADD CONSTRAINT fk_folder_dataset FOREIGN KEY (dataset_id) REFERENCES platform_dataset (dataset_id) ON DELETE CASCADE;
ALTER TABLE platform_dataset_folder ADD CONSTRAINT fk_folder_parent FOREIGN KEY (parent_folder_id) REFERENCES platform_dataset_folder (folder_id) ON DELETE CASCADE;
ALTER TABLE platform_subscription ADD CONSTRAINT fk_subscription_client FOREIGN KEY (id_client) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_pipeline_version ADD CONSTRAINT fk_version_pipeline FOREIGN KEY (id_pipeline) REFERENCES platform_pipeline (id_pipeline) ON DELETE CASCADE;
ALTER TABLE platform_bot_dataset ADD CONSTRAINT fk_bot_dataset_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_bot_dataset ADD CONSTRAINT fk_bot_dataset_dataset FOREIGN KEY (dataset_id) REFERENCES platform_dataset (dataset_id) ON DELETE CASCADE;
ALTER TABLE platform_observability_resource_node_execution ADD CONSTRAINT fk_node_execution FOREIGN KEY (execution_id) REFERENCES platform_observability_resource_execution (execution_id) ON DELETE CASCADE;
ALTER TABLE platform_conversation_message_feedback ADD CONSTRAINT fk_feedback_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_conversation_message_feedback ADD CONSTRAINT fk_feedback_message FOREIGN KEY (message_id) REFERENCES platform_conversation_message (id) ON DELETE CASCADE;
ALTER TABLE platform_bot_user_rate_limit ADD CONSTRAINT fk_rate_limit_bot FOREIGN KEY (bot_id) REFERENCES platform_bot (bot_id) ON DELETE CASCADE;
ALTER TABLE platform_vision ADD CONSTRAINT fk_vision_client FOREIGN KEY (client_id) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_vision ADD CONSTRAINT fk_vision_provider FOREIGN KEY (provider_id) REFERENCES platform_provider (provider_id);
ALTER TABLE platform_decision_tree_image ADD CONSTRAINT fk_decision_tree_image_tree FOREIGN KEY (tree_id) REFERENCES platform_decision_tree (id_decision_tree) ON DELETE CASCADE;
ALTER TABLE platform_observability_resource_node_trace ADD CONSTRAINT fk_trace_node_execution FOREIGN KEY (id_node_execution) REFERENCES platform_observability_resource_node_execution (id_node_execution) ON DELETE CASCADE;
ALTER TABLE platform_conversation_message ADD CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES platform_bot_conversation (id) ON DELETE CASCADE;
ALTER TABLE platform_model ADD CONSTRAINT fk_model_client FOREIGN KEY (client_id) REFERENCES platform_client (id) ON DELETE CASCADE;
ALTER TABLE platform_model ADD CONSTRAINT fk_model_provider FOREIGN KEY (provider_id) REFERENCES platform_provider (provider_id) ON DELETE CASCADE;
ALTER TABLE platform_vision_extractor_field ADD CONSTRAINT fk_field_extractor FOREIGN KEY (extractor_id) REFERENCES platform_vision_extractor (extractor_id) ON DELETE CASCADE;
ALTER TABLE platform_observability_resource_execution ADD CONSTRAINT fk_execution_client FOREIGN KEY (client_id) REFERENCES platform_client (id) ON DELETE SET NULL;
