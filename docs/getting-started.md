# Getting started

## 1. Get the jar into your local Maven repository

Cultivation is not published to a public Maven repository. Install the released
jar once:

```bash
mvn install:install-file \
  -Dfile=Cultivation-0.11.1.jar \
  -DgroupId=plugin.siren \
  -DartifactId=Cultivation \
  -Dversion=0.11.1 \
  -Dpackaging=jar
```

Repeat whenever you move to a new Cultivation version.

## 2. Declare the dependency

```xml
<repositories>
    <repository>
        <id>hytale-release</id>
        <url>https://maven.hytale.com/release</url>
    </repository>
</repositories>

<dependencies>
    <!-- The server API Cultivation 0.11.1 is built against (Update 7). -->
    <dependency>
        <groupId>com.hypixel.hytale</groupId>
        <artifactId>Server</artifactId>
        <version>0.7.2</version>
        <scope>provided</scope>
    </dependency>

    <!--
      provided, never shaded: the server loads the real Cultivation jar, and two
      copies of plugin.siren.Cultivation on the classpath would each have their
      own static plugin instance.
    -->
    <dependency>
        <groupId>plugin.siren</groupId>
        <artifactId>Cultivation</artifactId>
        <version>0.11.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Cultivation targets Java 25:

```xml
<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## 3. Declare it in your manifest

The plugin loader resolves another plugin's classes for you only if you say you
need them. In `src/main/resources/manifest.json`:

```json
{
  "Group": "MyGroup",
  "Name": "MyAddon",
  "Version": "1.0.0",
  "ServerVersion": ">=0.7.2 <0.8.0",
  "Dependencies": {
    "Siren:Cultivation": ">=0.11.1"
  },
  "Main": "com.example.myaddon.MyAddon"
}
```

Without this entry, your mod loads and then dies with `NoClassDefFoundError` the
first time it touches a Cultivation class.

`ServerVersion` needs a real range, not a bare wildcard-minor string like
`"0.7.x"` — a wildcard-minor pattern can never match a pre-release server, no
matter how high the pre-release number is. Match the range shape Cultivation's
own `manifest.json` uses: since 0.11.1 that is `>=0.7.2 <0.8.0`.

A range that does not match the running server does not stop your mod loading on
0.7.2 — the engine logs a warning naming it, a SEVERE "targeting a different
server version" line at boot, and a notice to admins as they join, then loads it
anyway. The engine source marks this to become a hard error, so treat the warning
as one.

`>=0.11.1` is what an addon using anything from 0.11.1 — `DaoDomainEvents`,
`SectArrayEvents`, `DaoElement.fromDamageCause` — must declare. An addon that uses
only older API can declare a lower floor, but 0.11.0 is the oldest Cultivation
that runs on Update 7 at all.

## 4. Register everything from `setup()`

```java
package com.example.myaddon;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import plugin.siren.API.CultivationAPI;
import plugin.siren.API.CultivationEvents;

import javax.annotation.Nonnull;

public class MyAddon extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static MyAddon plugin;

    public MyAddon(@Nonnull JavaPluginInit init) {
        super(init);
        plugin = this;
    }

    @Override
    protected void setup() {
        CultivationEvents.onBreakthrough(event ->
                LOGGER.atInfo().log("A cultivator reached %s.", event.newRealm().name()));

        CultivationAPI.registerQiAbsorptionItemModifier("MyAddon_JadeCharm", 1.5f);
    }

    public static MyAddon get() {
        return plugin;
    }
}
```

### Load order does not matter

Every registry in this API — listeners, races, techniques, menu pages, codex
entries, admin sections, the progression provider, the theme — is a plain static
collection that **nothing reads until a player actually does something**: opens a
menu, meditates, performs a technique, trips a realm gate. All of that is long
after every plugin on the server has finished loading.

So there is no need for `LoadBefore`, retry loops, deferred initialization, or
checking whether Cultivation has loaded yet. Register in `setup()` and move on.

### Clean up in `shutdown()`

Only needed if you installed something exclusive or something the mod keeps
rendering:

```java
@Override
protected void shutdown() {
    CultivationAPI.setProgressionProvider(null);   // back to the built-in ladder
    CultivationAPI.setTheme(null);                 // back to Cultivation's wording
    CultivationAPI.unregisterMenuPage("myAddon:alchemy");
    CultivationAPI.unregisterAdminConfigSection("MyAddon:balance");
}
```

Event listeners deliberately have **no** unregister — listener lifetime is server
lifetime, matching how plugins load once and stay.

## Optional dependencies

If your mod should also work on a server without Cultivation, move the entry:

```json
"OptionalDependencies": {
  "Siren:Cultivation": ">=0.11.1"
}
```

…and gate every entry point that touches a Cultivation class, so the JVM never
has to resolve one:

```java
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.PluginIdentifier;

private boolean cultivationInstalled;

@Override
protected void setup() {
    this.cultivationInstalled = HytaleServer.get().getPluginManager()
            .getPlugin(PluginIdentifier.fromString("Siren:Cultivation")) != null;

    if (this.cultivationInstalled) {
        LOGGER.atInfo().log("Cultivation found - enabling compatibility.");
        CultivationCompat.register();   // <-- the ONLY class that imports plugin.siren.API
    }
}
```

The important part is the **class boundary**: keep every Cultivation import
inside a separate class (`CultivationCompat` above) that is only ever reached
behind the check. Class loading is lazy per class, so a class nothing calls is
never resolved, and a server without Cultivation never notices.

Do not scatter `if (cultivationInstalled)` through a class that imports
Cultivation types at its top — the class fails to load as a whole, guard or no
guard.

## Where to go next

- [Reading player state](reading-state.md) — what you can ask about a player
- [Events](events.md) — reacting to and changing what the mod does
- [Driving progression](driving-progression.md) — granting Qi, ranking up, spirit veins
- [Config access](config-access.md) — reading and writing Cultivation's own settings
- [Compatibility](compatibility.md) — Endless Leveling, PlaceholderAPI, Marriage
- [Pitfalls](pitfalls.md) — read before you ship
