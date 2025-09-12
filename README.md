# tiAuth [![CodeFactor](https://www.codefactor.io/repository/github/1050tit0p/tiauth/badge)](https://www.codefactor.io/repository/github/1050tit0p/tiauth)
Authorization plugin for BungeeCord

---

### Features:
- Dialog window support `(1.21.6+)`
  - Interactive window with a password input field
- Premium mode
  - Allows licensed players to skip password entry by activating local `online-mode true`
- Session support
  - Allows players to skip password entry for a certain period after successful authentication
- Multiple database types support
  - Supports `SQLite`, `H2`, `MySQL`, `PostgreSQL`
- Virtual server
  - Virtual server for auth server provided by [NanoLimbo](https://github.com/1050TIt0p/NanoLimbo)

---

### Commands:
#### For players:
- /register <password> <password> - Register an account
- /login <password> - Log in
- /logout - Reset session
- /changepassword <old password> <new password> - Change password
- /premium - Enable premium mode
- /unregister <password> - Delete account

#### For administrators:
- coming soon...

---

### TODO:
Sorted roughly in the order of planned implementation
- Add support for multiple authentication servers and backends + online load balancer
- Display authentication countdown in ActionBar, Title, and BossBar
- Administrator commands
- 2FA via Discord, Telegram, and TOTP