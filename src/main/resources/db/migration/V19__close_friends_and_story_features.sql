-- Migration V19: Close Friends and Story Extensions (Highlights, Polls, Mentions, Likes)

-- 1. Extend stories table with caption and close-friends-only visibility flag
ALTER TABLE stories
    ADD COLUMN IF NOT EXISTS caption VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS is_close_friends_only BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Close Friends table
CREATE TABLE IF NOT EXISTS close_friends (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    friend_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_close_friends_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_close_friends_friend
        FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_close_friends UNIQUE (user_id, friend_id)
);

-- 3. Story Highlights table
CREATE TABLE IF NOT EXISTS story_highlights (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(50) NOT NULL,
    cover_image_url VARCHAR(700) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_highlights_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Story Highlight Items table
CREATE TABLE IF NOT EXISTS story_highlight_items (
    id UUID NOT NULL PRIMARY KEY,
    highlight_id UUID NOT NULL,
    story_id UUID NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_highlight_items_highlight
        FOREIGN KEY (highlight_id) REFERENCES story_highlights(id) ON DELETE CASCADE,

    CONSTRAINT fk_highlight_items_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- 5. Story Likes table
CREATE TABLE IF NOT EXISTS story_likes (
    id UUID NOT NULL PRIMARY KEY,
    story_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_likes_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,

    CONSTRAINT fk_story_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_story_likes UNIQUE (story_id, user_id)
);

-- 6. Story Mentions table
CREATE TABLE IF NOT EXISTS story_mentions (
    id UUID NOT NULL PRIMARY KEY,
    story_id UUID NOT NULL,
    mentioned_user_id UUID NOT NULL,
    x_percent DOUBLE PRECISION NULL,
    y_percent DOUBLE PRECISION NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_mentions_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,

    CONSTRAINT fk_story_mentions_user
        FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. Story Polls table
CREATE TABLE IF NOT EXISTS story_polls (
    id UUID NOT NULL PRIMARY KEY,
    story_id UUID NOT NULL UNIQUE,
    question VARCHAR(200) NOT NULL,
    option_a VARCHAR(50) NOT NULL DEFAULT 'Yes',
    option_b VARCHAR(50) NOT NULL DEFAULT 'No',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_polls_story
        FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- 8. Story Poll Votes table
CREATE TABLE IF NOT EXISTS story_poll_votes (
    id UUID NOT NULL PRIMARY KEY,
    poll_id UUID NOT NULL,
    user_id UUID NOT NULL,
    selected_option INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_story_poll_votes_poll
        FOREIGN KEY (poll_id) REFERENCES story_polls(id) ON DELETE CASCADE,

    CONSTRAINT fk_story_poll_votes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT uq_story_poll_votes UNIQUE (poll_id, user_id)
);
