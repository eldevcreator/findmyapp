-- Полная схема базы (зеркало ensureDatabase() в main.js).
-- Применяется на чистую БД. Рабочий воркер сам докатывает
-- недостающие колонки/индексы через CREATE TABLE / INDEX IF NOT EXISTS.
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS connections;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS user_history;
DROP TABLE IF EXISTS auto_replies;
DROP TABLE IF EXISTS edit_history;
DROP TABLE IF EXISTS authorized_users;
DROP TABLE IF EXISTS banned_users;
DROP TABLE IF EXISTS beta_applications;
DROP TABLE IF EXISTS user_states;
DROP TABLE IF EXISTS photo_cache;
DROP TABLE IF EXISTS inline_sessions;
DROP TABLE IF EXISTS warns;
DROP TABLE IF EXISTS lists;
DROP TABLE IF EXISTS settings;
DROP TABLE IF EXISTS emoji_cache;
DROP TABLE IF EXISTS spam_settings;
DROP TABLE IF EXISTS xp;
DROP TABLE IF EXISTS notes;
DROP TABLE IF EXISTS filters;
DROP TABLE IF EXISTS reminders;
DROP TABLE IF EXISTS badwords;
DROP TABLE IF EXISTS allowed_links;
DROP TABLE IF EXISTS group_extras;
DROP TABLE IF EXISTS temp_mutes;
DROP TABLE IF EXISTS captcha_pending;
DROP TABLE IF EXISTS afk;
DROP TABLE IF EXISTS karma;
DROP TABLE IF EXISTS scheduled_msgs;
DROP TABLE IF EXISTS weather_subs;
DROP TABLE IF EXISTS price_alerts;
DROP TABLE IF EXISTS economy;
DROP TABLE IF EXISTS marriages;
DROP TABLE IF EXISTS streaks;
DROP TABLE IF EXISTS lotto_entries;
DROP TABLE IF EXISTS giveaways;
DROP TABLE IF EXISTS giveaway_entries;
DROP TABLE IF EXISTS daily_claims;
DROP TABLE IF EXISTS shields;
DROP TABLE IF EXISTS spam_hits;
DROP TABLE IF EXISTS spam_exempt;
DROP TABLE IF EXISTS onboarding;
DROP TABLE IF EXISTS birthdays;
DROP TABLE IF EXISTS tickets;

CREATE TABLE IF NOT EXISTS connections (
  connection_id TEXT PRIMARY KEY,
  user_id INTEGER,
  username TEXT,
  full_name TEXT,
  is_active INTEGER DEFAULT 1,
  connected_at INTEGER
);

CREATE TABLE IF NOT EXISTS authorized_users (
  user_id INTEGER PRIMARY KEY,
  username TEXT,
  full_name TEXT,
  added_by INTEGER,
  added_at INTEGER
);

CREATE TABLE IF NOT EXISTS banned_users (
  user_id INTEGER PRIMARY KEY,
  username TEXT,
  full_name TEXT,
  reason TEXT,
  banned_by INTEGER,
  banned_at INTEGER
);

CREATE TABLE IF NOT EXISTS messages (
  chat_id INTEGER,
  message_id INTEGER,
  owner_id INTEGER,
  user_id INTEGER,
  is_outgoing INTEGER DEFAULT 0,
  username TEXT,
  full_name TEXT,
  text TEXT,
  media_type TEXT,
  vault_msg_id INTEGER,
  file_id TEXT,
  has_spoiler INTEGER DEFAULT 0,
  sent_at INTEGER,
  is_deleted INTEGER DEFAULT 0,
  chat_type TEXT DEFAULT 'private',
  PRIMARY KEY (chat_id, message_id)
);

CREATE TABLE IF NOT EXISTS edit_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  message_id INTEGER,
  user_id INTEGER,
  username TEXT,
  full_name TEXT,
  old_text TEXT,
  new_text TEXT,
  edited_at INTEGER
);

CREATE TABLE IF NOT EXISTS users (
  user_id INTEGER PRIMARY KEY,
  username TEXT,
  full_name TEXT,
  avatar_id TEXT,
  first_seen INTEGER,
  last_seen INTEGER
);

CREATE TABLE IF NOT EXISTS user_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER,
  change_type TEXT,
  old_val TEXT,
  new_val TEXT,
  changed_at INTEGER
);

CREATE TABLE IF NOT EXISTS auto_replies (
  chat_id INTEGER,
  owner_id INTEGER,
  last_reply INTEGER,
  PRIMARY KEY (chat_id, owner_id)
);

CREATE TABLE IF NOT EXISTS lists (
  owner_id INTEGER,
  target_id INTEGER,
  target_name TEXT,
  list_type TEXT,
  added_at INTEGER,
  PRIMARY KEY (owner_id, target_id, list_type)
);

CREATE TABLE IF NOT EXISTS settings (
  user_id INTEGER PRIMARY KEY,
  auto_reply INTEGER DEFAULT 0,
  cooldown_sec INTEGER DEFAULT 1800,
  notify_deleted INTEGER DEFAULT 1,
  notify_edited INTEGER DEFAULT 1,
  notify_new_user INTEGER DEFAULT 0,
  reply_mode TEXT DEFAULT 'blacklist',
  custom_reply TEXT,
  ai_model TEXT,
  ai_roast INTEGER DEFAULT 1);

CREATE TABLE IF NOT EXISTS beta_applications (
  user_id INTEGER PRIMARY KEY,
  username TEXT,
  full_name TEXT,
  status TEXT DEFAULT 'pending',
  reason TEXT,
  applied_at INTEGER
);

CREATE TABLE IF NOT EXISTS user_states (
  user_id INTEGER PRIMARY KEY,
  state TEXT,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS photo_cache (
  key TEXT PRIMARY KEY,
  file_id TEXT
);

CREATE TABLE IF NOT EXISTS inline_sessions (
  session_id TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL,
  kind TEXT NOT NULL,
  query TEXT NOT NULL,
  payload TEXT NOT NULL,
  page INTEGER DEFAULT 1,
  expires_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS warns (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  count INTEGER DEFAULT 0,
  last_reason TEXT,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS emoji_cache (
  emoji TEXT PRIMARY KEY,
  custom_emoji_id TEXT
);

CREATE TABLE IF NOT EXISTS spam_settings (
  chat_id INTEGER PRIMARY KEY,
  enabled INTEGER DEFAULT 0,
  max_per_window INTEGER DEFAULT 5,
  window_sec INTEGER DEFAULT 10,
  action TEXT DEFAULT 'del',
  antilink INTEGER DEFAULT 0,
  antiforward INTEGER DEFAULT 0,
  antisticker INTEGER DEFAULT 0,
  anticaps INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS xp (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  xp INTEGER DEFAULT 0,
  msg_count INTEGER DEFAULT 0,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS notes (
  chat_id INTEGER,
  name TEXT,
  text TEXT,
  by_id INTEGER,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, name)
);

CREATE TABLE IF NOT EXISTS filters (
  chat_id INTEGER,
  trigger TEXT,
  reply TEXT,
  by_id INTEGER,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, trigger)
);

CREATE TABLE IF NOT EXISTS reminders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  text TEXT,
  due_at INTEGER,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS badwords (
  chat_id INTEGER,
  word TEXT,
  by_id INTEGER,
  added_at INTEGER,
  PRIMARY KEY (chat_id, word)
);

CREATE TABLE IF NOT EXISTS allowed_links (
  chat_id INTEGER,
  domain TEXT,
  by_id INTEGER,
  added_at INTEGER,
  PRIMARY KEY (chat_id, domain)
);

CREATE TABLE IF NOT EXISTS group_extras (
  chat_id INTEGER PRIMARY KEY,
  welcome TEXT,
  goodbye TEXT,
  rules TEXT,
  captcha INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS temp_mutes (
  chat_id INTEGER,
  user_id INTEGER,
  until_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS captcha_pending (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  answer INTEGER,
  attempts INTEGER DEFAULT 3,
  expires INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS afk (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  reason TEXT,
  since INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS karma (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  karma INTEGER DEFAULT 0,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS scheduled_msgs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  user_id INTEGER,
  text TEXT,
  send_at INTEGER,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS weather_subs (
  chat_id INTEGER,
  user_id INTEGER,
  city TEXT,
  created_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS price_alerts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  coin TEXT,
  above REAL,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS economy (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  coins INTEGER DEFAULT 0,
  updated_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS marriages (
  chat_id INTEGER,
  user_a INTEGER,
  user_b INTEGER,
  since INTEGER,
  PRIMARY KEY (chat_id, user_a)
);

CREATE TABLE IF NOT EXISTS streaks (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  streak INTEGER DEFAULT 0,
  last_day TEXT,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS lotto_entries (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  created_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS giveaways (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  prize TEXT,
  ends_at INTEGER,
  created_by INTEGER,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS giveaway_entries (
  giveaway_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  PRIMARY KEY (giveaway_id, user_id)
);

CREATE TABLE IF NOT EXISTS daily_claims (
  chat_id INTEGER,
  user_id INTEGER,
  last_claim INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS shields (
  chat_id INTEGER,
  user_id INTEGER,
  until_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS spam_hits (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  reason TEXT,
  action TEXT,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS spam_exempt (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  by_id INTEGER,
  added_at INTEGER,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS onboarding (
  user_id INTEGER PRIMARY KEY,
  step TEXT DEFAULT 'start',
  completed INTEGER DEFAULT 0,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS premium_subs (
  user_id INTEGER PRIMARY KEY,
  tier TEXT,
  until_at INTEGER,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS birthdays (
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  day TEXT,
  month TEXT,
  year TEXT,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS tickets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER,
  user_id INTEGER,
  user_name TEXT,
  text TEXT,
  status TEXT DEFAULT 'open',
  created_at INTEGER,
  closed_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_messages_owner_chat ON messages (owner_id, chat_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_messages_owner_user ON messages (owner_id, user_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_messages_user ON messages (user_id);
CREATE INDEX IF NOT EXISTS idx_messages_username ON messages (username);
CREATE INDEX IF NOT EXISTS idx_edit_history_chat_msg ON edit_history (chat_id, message_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_user_history_user ON user_history (user_id);
CREATE INDEX IF NOT EXISTS idx_lists_owner_type ON lists (owner_id, list_type);
CREATE INDEX IF NOT EXISTS idx_connections_user ON connections (user_id);
CREATE INDEX IF NOT EXISTS idx_beta_status ON beta_applications (status);
CREATE INDEX IF NOT EXISTS idx_xp_chat ON xp (chat_id, xp DESC);
CREATE INDEX IF NOT EXISTS idx_notes_chat ON notes (chat_id);
CREATE INDEX IF NOT EXISTS idx_filters_chat ON filters (chat_id);
CREATE INDEX IF NOT EXISTS idx_reminders_due ON reminders (due_at);
CREATE INDEX IF NOT EXISTS idx_badwords_chat ON badwords (chat_id);
CREATE INDEX IF NOT EXISTS idx_temp_mutes_due ON temp_mutes (until_at);
CREATE INDEX IF NOT EXISTS idx_captcha_exp ON captcha_pending (expires);
CREATE INDEX IF NOT EXISTS idx_inline_sessions_user ON inline_sessions (user_id, kind, expires_at);
CREATE INDEX IF NOT EXISTS idx_inline_sessions_expiry ON inline_sessions (expires_at);
CREATE INDEX IF NOT EXISTS idx_karma_chat ON karma (chat_id, karma DESC);
CREATE INDEX IF NOT EXISTS idx_sched_due ON scheduled_msgs (send_at);
CREATE INDEX IF NOT EXISTS idx_economy_chat ON economy (chat_id, coins DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_coin ON price_alerts (coin);
CREATE INDEX IF NOT EXISTS idx_give_entries ON giveaway_entries (giveaway_id);
CREATE INDEX IF NOT EXISTS idx_give_ends ON giveaways (ends_at);
CREATE INDEX IF NOT EXISTS idx_spam_hits_chat ON spam_hits (chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_spam_exempt_chat ON spam_exempt (chat_id);
CREATE INDEX IF NOT EXISTS idx_birthdays_chat ON birthdays (chat_id, month, day);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets (chat_id, status, created_at DESC);
CREATE TABLE IF NOT EXISTS phones (phone TEXT, user_id INTEGER, full_name TEXT, username TEXT, first_seen INTEGER, source INTEGER, PRIMARY KEY (phone, user_id));
CREATE INDEX IF NOT EXISTS idx_phones_phone ON phones (phone);
CREATE INDEX IF NOT EXISTS idx_phones_user ON phones (user_id);

CREATE TABLE IF NOT EXISTS sec_notes (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, title TEXT, content TEXT, created_at INTEGER);
CREATE TABLE IF NOT EXISTS sec_habits (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, icon TEXT, created_at INTEGER);
CREATE TABLE IF NOT EXISTS sec_habit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, habit_id INTEGER, user_id INTEGER, date TEXT, done INTEGER);
CREATE TABLE IF NOT EXISTS sec_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, title TEXT, deadline TEXT, status TEXT, created_at INTEGER);
CREATE TABLE IF NOT EXISTS sec_journal (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, date TEXT, content TEXT, mood INTEGER, created_at INTEGER, UNIQUE(user_id, date));
CREATE INDEX IF NOT EXISTS idx_sec_notes_user ON sec_notes(user_id);
CREATE INDEX IF NOT EXISTS idx_sec_journal_user ON sec_journal(user_id);
CREATE INDEX IF NOT EXISTS idx_sec_habits_user ON sec_habits(user_id);
CREATE INDEX IF NOT EXISTS idx_sec_goals_user ON sec_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_sec_habit_logs_user ON sec_habit_logs(user_id);

CREATE TABLE IF NOT EXISTS fin_ops (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, type TEXT, amount REAL, cat TEXT, note TEXT, currency TEXT, date TEXT, created_at INTEGER);
CREATE TABLE IF NOT EXISTS fin_budgets (user_id INTEGER, cat TEXT, amount_limit REAL, created_at INTEGER, PRIMARY KEY (user_id, cat));
CREATE INDEX IF NOT EXISTS idx_fin_ops_user ON fin_ops(user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_fin_budgets_user ON fin_budgets(user_id);
CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, title TEXT, note TEXT, project TEXT, prio TEXT DEFAULT 'mid', status TEXT DEFAULT 'todo', due_at INTEGER, done_at INTEGER, created_at INTEGER);
CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id, status, due_at);
CREATE INDEX IF NOT EXISTS idx_tasks_due ON tasks(due_at);
CREATE TABLE IF NOT EXISTS biz_chats (owner_id INTEGER PRIMARY KEY, chat_id INTEGER, updated_at INTEGER);
