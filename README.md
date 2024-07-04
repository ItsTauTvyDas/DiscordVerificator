<h1><img width=80 src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/2a163eb9-515e-409a-b581-94a4fa513d91" /> <div>DiscordVerificator</div></h1>

<p>
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java-gray?color=C8273F" /></a>
  <a href="https://hub.spigotmc.org/javadocs/spigot/"><img src="https://img.shields.io/badge/Spigot_API-gray?color=F07427&logo=spigotmc&logoColor=FFFFFF" /></a>
  <a href="https://jda.wiki/"><img src="https://img.shields.io/badge/JDA-gray?color=5662F6&logo=discord&logoColor=FFFFFF" /></a>
  <a href="https://github.com/vshymanskyy/StandWithUkraine"><img src="https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/badges/StandWithUkraine.svg"></a>
</p>

 **DiscordVerificator** is a **Spigot** plugin that allows you to do player authentication using **Discord bot**.<br>

> [!WARNING]
> This plugin is **intended** to be used on **private servers** with the **manual player addition** because it involves you to manually link each player to their **Discord profile**.

 It was developed as an **alternative** for password-based authorization like `/login <password>` on servers with `online-mode` set to `false` (_in server.properties_).


 ## 🤔 How it works?

This plugin enables players to **link their usernames to their Discord profiles**. <br/>

The **linking process** is **controlled by the administrator** of the server. <br/>
In order **to link** the account, the **admin** should run `/link <Player> <Discord ID>`. ([how to get discord id?](https://youtu.be/RzTWH0g2xbo?si=oQT2rCSuf6B3Z5kY))
<img src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/50193702-ed0f-4b60-9884-58754a25328d">

Then, **when player joins a server**, the **verification code appears**.<br>
To join the server, the player should run the seen command to the **Discord bot** you've configured: <br>
<img height=250 src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/1ad48c69-198b-48dc-8f5a-837312f094fa"><br>
<img src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/235dfef9-f390-4e2b-9bbe-d8e525425fe8"><br>
<img src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/c2758242-a3cd-4ef3-b6ec-83eb68e9438f">

> [!CAUTION]
> The plugin **will prevent player from joining** if it isn't linked to **Discord** profile yet:
> <img height=200 src="https://github.com/MrQuackDuck/DiscordVerificator/assets/61251075/ef98c616-3c90-41cf-a111-ae49f416dc3c">

## 💻 Commands
- `/link <player> <discordId>` — links the player to its Discord profile. ([how to get discord id?](https://youtu.be/RzTWH0g2xbo?si=oQT2rCSuf6B3Z5kY))
- `/unlink <player>` — unlinks the player from its Discord profile.
- `/dvreload` — reloads the plugin (_including Discord bot_).
  
## 🔞 Permissions
- `discordVerificator.link` _(for **operators** by default)_ — Allows to use `/link <player> <discordId>`
- `discordVerificator.unlink` _(for **operators** by default)_ — Allows to use `/unlink <player>`
- `discordVerificator.reload` _(for **operators** by default)_ — Allows to use `/reload`

## 📄 Default config
> [!IMPORTANT]
> You should replace `DISCORD_BOT_TOKEN` with your **Discord bot token**.<br>
> **Otherwise, nothing will work!**

```yml
# 1. Create discord bot on discord developer portal: https://discord.com/developers/applications
# 2. In "Bot" tab
#    - Make sure to enable "Presence Intent"
#    - Make sure to enable "Server Members Intent"
#    - Make sure to enable "Message Content Intent"
# 3. Invite the bot on your Discord server to give an ability for players to send commands to the bot
token: "DISCORD_BOT_TOKEN"

messages:
  "not-enough-permissions": "&cNot enough permissions!"
  "invalid-link-format": "&cInvalid format! Please use: /link <Player> <Discord ID>"
  "invalid-unlink-format": "&cInvalid format! Please use: /unlink <Player>"
  "invalid-user-id-format": "&cInvalid Discord ID format!"
  "successfully-linked": "&aSuccessfully linked!"
  "successfully-unlinked": "&aSuccessfully unlinked!"
  "player-already-linked": "&cThis player was already linked!"
  "player-was-not-linked": "&cThis player was never linked!"
  "account-not-linked": "&cYour account is not linked to Discord profile yet."
  "bot-not-working": "&cDiscord bot is not currently working!\nAsk an administrator to resolve this issue."
  "confirm-with-command": "&6Confirm your IP via Discord bot\nUsage: &f&n/confirm %s"
  "wait-until-verification": "&cPlease wait until you can have new code!\n&f&n%s seconds left."
  "error-occurred": "Error occurred!"
  "its-not-your-account": "Account you're trying to confirm is not linked to your Discord profile"
  "allowed": "Allowed!"
  "allowed-to-join-from-ip": "Successfully allowed to join from `%s`!"
  "confirm-command": "Command to verify you on Minecraft server"
  "verification-code-you-got": "Verification code you've got from the server"
  "invalid-code": "Invalid Code!"
  "invalid-code-description": "This verification code is not valid!"
  "invalid-usage": "Invalid usage!"
  "provide-code-please": "Provide verification code, please!"
  "user-not-found": "User not found!"
  "user-not-found-description": "It seems like your account wasn't yet linked to any Minecraft username"
  "reloaded": "&#14C60D[DiscordVerificator] Reloaded!"
```
