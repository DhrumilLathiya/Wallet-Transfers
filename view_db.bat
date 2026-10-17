@echo off
echo ========================================================
echo                   ACCOUNTS TABLE
echo ========================================================
sqlite3 wallet.db -header -column "SELECT id, owner_name, balance, (balance / 100.0) as rupees, created_at FROM accounts;"

echo.
echo ========================================================
echo                   TRANSFERS TABLE
echo ========================================================
sqlite3 wallet.db -header -column "SELECT id, from_id, to_id, amount, (amount / 100.0) as rupees, reference, status, reason, created_at FROM transfers ORDER BY id DESC LIMIT 20;"
echo ========================================================
