-- V14: Group Chat & Message Reactions Migration

ALTER TABLE conversations
    ADD COLUMN name VARCHAR(100) NULL,
    ADD COLUMN avatar_url VARCHAR(700) NULL,
    ADD COLUMN creator_id UUID NULL,
    ADD CONSTRAINT fk_conversations_creator
        FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE conversation_members
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID NOT NULL PRIMARY KEY,
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_message_reactions_message
        FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,

    CONSTRAINT fk_message_reactions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_message_reactions UNIQUE (message_id, user_id));
