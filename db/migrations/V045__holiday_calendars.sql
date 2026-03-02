-- Holiday Calendars: reusable named calendars per market/region
CREATE TABLE IF NOT EXISTS holiday_calendars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES spaces(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    market VARCHAR(10) NOT NULL DEFAULT 'GLOBAL',
    region VARCHAR(50),
    weekend_days JSONB NOT NULL DEFAULT '["SATURDAY","SUNDAY"]',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Holiday Entries: individual dates in a calendar
CREATE TABLE IF NOT EXISTS holiday_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    calendar_id UUID NOT NULL REFERENCES holiday_calendars(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    name VARCHAR(200) NOT NULL,
    entry_type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC_HOLIDAY',
    recurring BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(calendar_id, date)
);

-- entry_type values: PUBLIC_HOLIDAY, AD_HOC, DAY_IN_LIEU, COMPANY_HOLIDAY

-- Link schedules to holiday calendars
ALTER TABLE schedules ADD COLUMN IF NOT EXISTS holiday_calendar_id UUID REFERENCES holiday_calendars(id) ON DELETE SET NULL;
ALTER TABLE schedules ADD COLUMN IF NOT EXISTS skip_on_holiday VARCHAR(20) NOT NULL DEFAULT 'SKIP';
-- skip_on_holiday values: SKIP (don't run), DEFER_NEXT (run next business day), RUN_ANYWAY

-- Link workflows to holiday calendars
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS holiday_calendar_id UUID REFERENCES holiday_calendars(id) ON DELETE SET NULL;
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS skip_on_holiday VARCHAR(20) NOT NULL DEFAULT 'SKIP';

-- Seed sample calendars
INSERT INTO holiday_calendars (id, space_id, name, description, market, region, weekend_days, enabled, created_by) VALUES
('ca100000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'US Federal Holidays', 'US market federal holidays and weekends', 'US', 'North America', '["SATURDAY","SUNDAY"]', true, 'a0000000-0000-0000-0000-000000000001'),
('ca100000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'UK Bank Holidays', 'UK market bank holidays', 'UK', 'Europe', '["SATURDAY","SUNDAY"]', true, 'a0000000-0000-0000-0000-000000000001'),
('ca100000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 'Singapore Public Holidays', 'Singapore market holidays', 'SG', 'Asia Pacific', '["SATURDAY","SUNDAY"]', true, 'a0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Seed US holidays for 2026
INSERT INTO holiday_entries (calendar_id, date, name, entry_type, recurring) VALUES
('ca100000-0000-0000-0000-000000000001', '2026-01-01', 'New Year''s Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-01-19', 'Martin Luther King Jr. Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-02-16', 'Presidents'' Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-05-25', 'Memorial Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-07-04', 'Independence Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-09-07', 'Labor Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-11-26', 'Thanksgiving Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000001', '2026-12-25', 'Christmas Day', 'PUBLIC_HOLIDAY', true),
-- UK holidays
('ca100000-0000-0000-0000-000000000002', '2026-01-01', 'New Year''s Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-04-03', 'Good Friday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-04-06', 'Easter Monday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-05-04', 'Early May Bank Holiday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-05-25', 'Spring Bank Holiday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-08-31', 'Summer Bank Holiday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-12-25', 'Christmas Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000002', '2026-12-28', 'Boxing Day (substitute)', 'PUBLIC_HOLIDAY', true),
-- Singapore holidays
('ca100000-0000-0000-0000-000000000003', '2026-01-01', 'New Year''s Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-01-29', 'Chinese New Year', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-01-30', 'Chinese New Year Day 2', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-04-03', 'Good Friday', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-05-01', 'Labour Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-05-26', 'Vesak Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-06-15', 'Hari Raya Haji', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-08-09', 'National Day', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-10-20', 'Deepavali', 'PUBLIC_HOLIDAY', true),
('ca100000-0000-0000-0000-000000000003', '2026-12-25', 'Christmas Day', 'PUBLIC_HOLIDAY', true)
ON CONFLICT DO NOTHING;

-- Link first schedule to US calendar
UPDATE schedules SET holiday_calendar_id = 'ca100000-0000-0000-0000-000000000001', skip_on_holiday = 'SKIP'
WHERE name = 'Nightly Loan Eligibility Check';
