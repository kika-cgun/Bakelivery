CREATE TABLE messages (
  id UUID PRIMARY KEY,
  bakery_id UUID NOT NULL,
  thread_id UUID NOT NULL REFERENCES threads(id),
  sender_id UUID NOT NULL,
  sender_role VARCHAR(30) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_thread ON messages(thread_id, created_at);
