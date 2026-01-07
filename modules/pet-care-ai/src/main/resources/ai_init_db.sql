-- AI服务数据库
CREATE DATABASE IF NOT EXISTS `pet_care_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pet_care_ai`;

-- 文档表
CREATE TABLE `tb_knowledge_document`
(
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    `name`        VARCHAR(200) NOT NULL COMMENT '文档名称',
    `file_url`    VARCHAR(500) NOT NULL COMMENT '文件存储URL',
    `file_type`   VARCHAR(20)  NOT NULL COMMENT '文件类型：pdf, doc, docx, md, txt等',
    `file_size`   BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `version`     INT      DEFAULT 1 COMMENT '文档版本号',
    `status`      TINYINT  DEFAULT 1 COMMENT '状态：1-有效，0-已删除',
    `chunk_count` INT      DEFAULT 0 COMMENT '文档分块数量',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='RAG知识库文档表';

