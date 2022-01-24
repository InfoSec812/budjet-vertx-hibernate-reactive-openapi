
-- Bills Query
WITH days_in_period AS (
    -- Create a series of dates
    SELECT CAST(generate_series(CAST(? AS date), CAST(? AS date), CAST('1 day' AS interval)) AS date) as date
),
dates AS (
    -- Using those dates, extract the day/week/month/year parts for later join logic
    SELECT
        EXTRACT(ISODOW FROM d.date) AS dow,
        EXTRACT(DAY FROM d.date) AS dom,
        EXTRACT(WEEK FROM d.date) AS week,
        EXTRACT(MONTH FROM d.date) AS month,
        EXTRACT(YEAR FROM d.date) AS year,
        d.date
    FROM days_in_period d
),
bills_with_months AS (
    -- Select all bills which have due dates which correspond with a date from the series above
    -- and join against any existing payment information from the months table
    SELECT
        b.*,
        array_agg(
            json_build_object(
                'id', m.id,
                'bill_id', COALESCE(m.bill_id, b.id),
                'day', COALESCE(m.day, d.dom),
                'month', coalesce(m.month, d.month),
                'year', coalesce(m.year, d.year),
                'paid', coalesce(m.paid, false)
            )
        ) AS months
    FROM dates d
    INNER JOIN bills b ON
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 12 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 7 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 6 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 6 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 3 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 5 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (EXTRACT(DAY FROM b.startdate) = d.dom AND b.period = 4 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (((EXTRACT(WEEK FROM b.startdate) % 2) = (d.week % 2)) AND (EXTRACT(ISODOW FROM b.startdate) = d.dow) AND b.period = 2 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (EXTRACT(ISODOW FROM b.startdate) = d.dow AND b.period = 1 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (b.startdate = d.date AND b.period = 0)
    LEFT JOIN
        months m
    ON
        m.bill_id = b.id AND
        m.day = d.dom AND
        m.month = d.month AND
        m.year = d.year
    GROUP BY b.id, m.bill_id
)
SELECT to_json(bwm) as bill FROM bills_with_months bwm;


-- Income Query

WITH days_in_period AS (
    -- Create a series of dates
    SELECT CAST(generate_series(CAST(? AS date), CAST(? AS date), CAST('1 day' AS interval)) AS date) as date
),
dates AS (
     -- Using those dates, extract the day/week/month/year parts for later join logic
     SELECT
         EXTRACT(ISODOW FROM d.date) AS dow,
         EXTRACT(DAY FROM d.date) AS dom,
         EXTRACT(WEEK FROM d.date) AS week,
         EXTRACT(MONTH FROM d.date) AS month,
         d.date
     FROM days_in_period d
)
-- Select all income sources which have due dates which correspond with a date from the
-- series above
SELECT
    i.*
FROM dates d
INNER JOIN
    income i
ON
    ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 12 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 7 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 6 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 6 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 3 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 5 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    (EXTRACT(DAY FROM i.startdate) = d.dom AND i.period = 4 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    (((EXTRACT(WEEK FROM i.startdate) % 2) = (d.week % 2)) AND (EXTRACT(ISODOW FROM i.startdate) = d.dow) AND i.period = 2 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    (EXTRACT(ISODOW FROM i.startdate) = d.dow AND i.period = 1 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
    (i.startdate = d.date AND i.period = 0)
ORDER BY d.date ASC;


-- Cash Flow Query WIP

WITH days_in_period AS (
    -- Create a series of dates
    SELECT CAST(generate_series(CAST(? AS date), CAST(? AS date), CAST('1 day' AS interval)) AS date) as date
),
dates AS (
    -- Using those dates, extract the day/week/month/year parts for later join logic
    SELECT
        EXTRACT(ISODOW FROM d.date) AS dow,
        EXTRACT(DAY FROM d.date) AS dom,
        EXTRACT(WEEK FROM d.date) AS week,
        EXTRACT(MONTH FROM d.date) AS month,
        EXTRACT(YEAR FROM d.date) AS year,
        d.date
FROM days_in_period d
),
bills_with_months AS (
    -- Select all bills which have due dates which correspond with a date from the series above
    -- and join against any existing payment information from the months table
    SELECT
         d.date,
         b.*
    FROM dates d
    INNER JOIN bills b ON
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 12 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 7 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 6 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 6 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, b.startdate)) % 3 = 0) AND (EXTRACT(DAY FROM b.startdate) = d.dom) AND b.period = 5 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (EXTRACT(DAY FROM b.startdate) = d.dom AND b.period = 4 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (((EXTRACT(WEEK FROM b.startdate) % 2) = (d.week % 2)) AND (EXTRACT(ISODOW FROM b.startdate) = d.dow) AND b.period = 2 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (EXTRACT(ISODOW FROM b.startdate) = d.dow AND b.period = 1 AND (b.enddate IS NULL OR b.enddate >= d.date)) OR
        (b.startdate = d.date AND b.period = 0)
    GROUP BY b.id, d.date
),
incomes AS (
    -- Select all income sources which have due dates which correspond with a date from the
    -- series above
    SELECT
        d.date,
        i.*
    FROM dates d
    INNER JOIN
          income i
    ON
        ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 12 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 7 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 6 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 6 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        ((EXTRACT(MONTH FROM age(d.date, i.startdate)) % 3 = 0) AND (EXTRACT(DAY FROM i.startdate) = d.dom) AND i.period = 5 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        (EXTRACT(DAY FROM i.startdate) = d.dom AND i.period = 4 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        (((EXTRACT(WEEK FROM i.startdate) % 2) = (d.week % 2)) AND (EXTRACT(ISODOW FROM i.startdate) = d.dow) AND i.period = 2 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        (EXTRACT(ISODOW FROM i.startdate) = d.dow AND i.period = 1 AND (i.enddate IS NULL OR i.enddate >= d.date)) OR
        (i.startdate = d.date AND i.period = 0)
    ORDER BY d.date ASC
)
SELECT
    -- Join together the bills and income using aggregate and JSON functions to build out a cashflow
    -- series
    json_build_object(
        'date', d.date,
        'balance', (SUM(COALESCE(i.amount, 0)) + ? - SUM(COALESCE(b.amount, 0))),
        'currency', 'USD',
        'expenditures', to_json(array_agg(b)),
        'income', to_json(array_agg(i))
    ) as daily_balance
FROM
    dates d
LEFT JOIN
    bills_with_months b
ON
    b.date = d.date
LEFT JOIN
    incomes i
ON
    i.date = d.date
GROUP BY d.date, b.id, i.id
ORDER BY d.date ASC;