# tiAuth [![CodeFactor](https://www.codefactor.io/repository/github/1050tit0p/tiauth/badge)](https://www.codefactor.io/repository/github/1050tit0p/tiauth)
Authorization plugin for BungeeCord and Velocity

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
  - Virtual server for auth server powered by [NanoLimbo](https://github.com/1050TIt0p/NanoLimbo)

---

### Commands:
#### For players:
- `/register <password> <password>` - Register an account
- `/login <password>` - Log in
- `/logout` - Destory session
- `/changepassword <old password> <new password>` - Change password
- `/premium` - Enable premium mode
- `/unregister` <password> - Delete account

#### For administrators:
- `/tiauth reload` - Reload config
  - Permission: `tiauth.admin.commands.reload`
- `/tiauth unregister <player>` - Delete player account
  - Permission: `tiauth.admin.commands.unregister`
- `/tiauth changepassword <player> <password>` - Change player password
  - Permission: `tiauth.admin.commands.changepassword`
- `/tiauth forcelogin <player>` - Force login player
  - Permission: `tiauth.admin.commands.forcelogin`
- `/tiauth migrate <sourceplugin> <sourcedatabase> [file] [user] [password] [host] [port] [name]` - Migrate database from other plugins/database type
  - Permission: `tiauth.admin.commands.migrate`
