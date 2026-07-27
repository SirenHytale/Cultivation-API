# Getting started

## 1. Get the jar into your local Maven repository

Cultivation is not published to a public Maven repository. Install the released
jar once:

```bash
mvn install:install-file \
  -Dfile=Cultivation-0.6.0.jar \
  -DgroupId=plugin.siren \
  -DartifactId=Cultivation \
  -Dversion=0.6.0 \
  -Dpackaging=jar
```

Repeat whenever you move to a new Cultivation version.

## 2. Declare the dependency

```xml
<dependencies>
    <dependency>
        <groupId>com.hypixel.hytale</groupId>
        <artifactId>Server</artifactId>
        <version>0.5.7</version>
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
        <version>0.6.0</version>
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
  "ServerVersion": "0.5.x",
  "Dependencies": {
    "Siren:Cultivation": ">=0.6.0"
  },
  "Main": "com.example.myaddon.MyAddon"
}
```

Without this entry, your mod loads and then dies with `NoClassDefFoundError` the
first time it touches a Cultivation class.

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
  "Siren:Cultivation": ">=0.6.0"
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
- [Pitfalls](pitfalls.md) — read before you ship
