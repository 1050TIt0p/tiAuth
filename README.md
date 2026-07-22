# tiAuth [![CodeFactor](https://www.codefactor.io/repository/github/1050tit0p/tiauth/badge)](https://www.codefactor.io/repository/github/1050tit0p/tiauth)
Authorization plugin for BungeeCord and Velocity

---

### Features:
- Dialog window support `(1.21.6+)`
  - Interactive window with a password input field
- Premium mode
  - Allows accounts marked premium to skip password entry after online-mode verification
- Session support
  - Allows players to skip password entry for a certain period after successful authentication
- Two-factor authentication
  - Supports account linking with Google Authenticator, Discord, and Telegram via [tiAuth-SocialAddon](https://github.com/1050TIt0p/tiAuth-SocialAddon)
- Multiple database types support
  - Supports `SQLite`, `H2`, `MySQL`, `PostgreSQL`
- Virtual server
  - Virtual server for auth server powered by [PicoLimbo](https://github.com/Quozul/PicoLimbo/)

---

### Commands:
#### For players:
- `/register <password> <password>` - Register an account
- `/login <password>` - Log in
- `/logout` - Destroy session
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

## Upgrading a 1.3.5 configuration to 1.4.3

Back up `plugins/tiAuth`, replace the jar, and start the proxy once. The serializer keeps existing values and writes missing 1.4.x options with defaults. Existing MySQL accounts and password hashes remain compatible.

The important additions for a 1.3.5 configuration are:

```yaml
servers:
  use-virtual-server: true
  virtual-server-port: 65535
  virtual-server-auto-update: true
  auth: "auth"
  backend: "lifesteal_backup"
  forced-hosts: {}

auth:
  # Existing BCRYPT and SHA256 hashes continue to work. ARGON2 is also available.
  argon2-iterations: 2
  argon2-memory: 65536
  argon2-parallelism: 1
  repeat-password-when-register: true
  totp:
    enabled: true
    issuer: "Koro Network"
    qr-generator-url: "https://api.qrserver.com/v1/create-qr-code/?data={data}&size=200x200&ecc=M&margin=30"
    need-password: true
    recovery-codes-amount: 16
    max-attempts: 3
    ban-player: true
    ban-time: 60
    timeout-seconds: 60

premium:
  enabled: true
  bypass-authentication: true
  force-online-mode: true

check-updates: true
```

PicoLimbo now uses `plugins/tiAuth/picolimbo/config.toml`; the older NanoLimbo settings path is not used by 1.4.x. Keep `virtual-server-port` unused by other local services.

Do not disable `premium.force-online-mode` on an offline-mode proxy. Without Mojang/Microsoft online-mode verification, another client could claim a premium username. Mark your authenticated account premium with `/premium`, or use `/tiauth forcepremium <player>` as an administrator.

## KoroEdge integration (Velocity)

Install KoroEdge 1.2.0 and tiAuth 1.4.3 on every proxy. KoroEdge uses its existing Redis connection to carry a one-time, username-and-IP-bound authentication handoff during a remote `backendRoutes` transfer. Destination tiAuth consumes it before selecting the auth server, so a player who authenticated on the PK proxy can transfer directly to Europe and join `lifesteal_backup` without authenticating again.

No database or Redis password is duplicated in tiAuth. If either plugin is missing or outdated on a node, tiAuth fails closed and performs its normal authentication flow.
