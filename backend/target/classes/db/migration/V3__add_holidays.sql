-- 节假日表（预留扩展）
CREATE TABLE IF NOT EXISTS holidays (
  id BIGSERIAL PRIMARY KEY,
  holiday_date DATE NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) NOT NULL DEFAULT 'NATIONAL',
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holidays_date ON holidays(holiday_date);

-- 插入示例节假日数据（2026年部分节假日）
INSERT INTO holidays (holiday_date, name, type) VALUES
('2026-01-01', '元旦', 'NATIONAL'),
('2026-01-02', '元旦假期', 'NATIONAL'),
('2026-02-17', '春节', 'NATIONAL'),
('2026-02-18', '春节假期', 'NATIONAL'),
('2026-02-19', '春节假期', 'NATIONAL'),
('2026-02-20', '春节假期', 'NATIONAL'),
('2026-02-21', '春节假期', 'NATIONAL'),
('2026-02-22', '春节假期', 'NATIONAL'),
('2026-02-23', '春节假期', 'NATIONAL'),
('2026-04-05', '清明节', 'NATIONAL'),
('2026-04-06', '清明节假期', 'NATIONAL'),
('2026-04-07', '清明节假期', 'NATIONAL'),
('2026-05-01', '劳动节', 'NATIONAL'),
('2026-05-02', '劳动节假期', 'NATIONAL'),
('2026-05-03', '劳动节假期', 'NATIONAL'),
('2026-06-22', '端午节', 'NATIONAL'),
('2026-06-23', '端午节假期', 'NATIONAL'),
('2026-06-24', '端午节假期', 'NATIONAL'),
('2026-10-01', '国庆节', 'NATIONAL'),
('2026-10-02', '国庆节假期', 'NATIONAL'),
('2026-10-03', '国庆节假期', 'NATIONAL'),
('2026-10-04', '国庆节假期', 'NATIONAL'),
('2026-10-05', '国庆节假期', 'NATIONAL'),
('2026-10-06', '国庆节假期', 'NATIONAL'),
('2026-10-07', '国庆节假期', 'NATIONAL');
