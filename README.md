# Re:Zero Death Sound Trigger (NeoForge 1.21.11, client-only)

Plays the resource pack's `rezero:death` sound when you die:
* **Real death** - detected when your health hits 0.
* **Fake death (Hypixel etc.)** - detected from the death message in server chat.

## Build it (pick one)

**A. No install - GitHub Actions**
1. Create a free GitHub repo, upload everything in this folder (keep the `.github` folder).
2. Open the repo's **Actions** tab -> the latest "Build" run -> download the **rezerodeath-jar** artifact.
3. Unzip it and put the `.jar` in your profile's `mods` folder.

**B. Local** (needs JDK 21)
```
./gradlew build        # Windows: gradlew.bat build
```
The jar appears in `build/libs/`.

## Use it
* Resource pack: enable `ReZeroDeathSound.zip` (Modrinth App -> profile -> Resource Packs -> Add).
* `/rezerodeath test` plays the sound once. It plays via Java audio (assets/rezero/death.wav) so Respawn does not cut it off.
* Fake-death detection on Hypixel: spectator-mode switch (text-independent) + death messages in chat.
* On death it also prints "I LOVE YOU" x15 in your own chat and tiles it in big dark-purple letters across the screen for 3s (local only; all configurable in the toml).
* `/rezerodeath debug` toggles logging every server chat line to `logs/latest.log` (search `[RezeroDeath]`).
* Config: `config/rezerodeath-client.toml` (or Mods screen -> Config). Edit `chatTriggers` to add the
  exact death messages your server sends. `{player}` = your username.
