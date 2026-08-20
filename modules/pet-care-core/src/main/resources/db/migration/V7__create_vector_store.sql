CREATE TABLE petcare.vector_store (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content text,
    metadata json,
    embedding petcare.vector(1024)
);

CREATE INDEX idx_vector_store_embedding_hnsw
    ON petcare.vector_store USING hnsw (embedding petcare.vector_cosine_ops);

GRANT SELECT, INSERT, UPDATE, DELETE ON petcare.vector_store TO petcare_app;
