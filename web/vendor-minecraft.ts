// fill vendor/ with a Minecraft version, its libraries and its assets from a local launcher install
import {
    copyFileSync,
    existsSync,
    mkdirSync,
    readdirSync,
    readFileSync,
    rmSync,
    writeFileSync,
} from "node:fs";
import { homedir, platform } from "node:os";
import { dirname, join, resolve } from "node:path";

interface Library {
    name: string;
    natives?: unknown;
    rules?: unknown;
    downloads?: { artifact?: { path: string } };
}

// Flagon brings its own LWJGL and input; Realms needs TLS sockets, and Mojang's netty patch JNDI
const SKIPPED = [
    "org.lwjgl.",
    "net.java.jinput:",
    "net.java.jutils:",
    "com.mojang:realms:",
    "com.mojang:netty:",
    "org.apache.httpcomponents:",
    "commons-logging:",
];
// the packages log4j scans for its plugins
const PRESERVED = "org.apache.logging.log4j.core.pattern.*\n";
// paulscode calls static methods on its sound library by name
const REFLECTED = "paulscode.sound.libraries.*\n";
// the languages besides en_US and the world preset images load after the game starts
const APART = "minecraft/lang/*.lang\nassets/minecraft/textures/gui/presets/*\n";

const version = process.argv[2] ?? "1.8.9";
const launcher = resolve(process.argv[3] ?? defaultLauncher());
const vendor = resolve(import.meta.dir, "..", "vendor");

const manifestPath = join(launcher, "versions", version, `${version}.json`);
if (!existsSync(manifestPath)) {
    console.error(
        `no ${manifestPath}; start ${version} once in the launcher, or pass its folder`,
    );
    process.exit(1);
}
const manifest = JSON.parse(readFileSync(manifestPath, "utf8"));

// vendor/ holds one program at a time
mkdirSync(vendor, { recursive: true });
for (const entry of readdirSync(vendor)) {
    if (entry.endsWith(".jar") || entry === "assets") {
        rmSync(join(vendor, entry), { recursive: true });
    }
}
writeFileSync(join(vendor, "preserve.txt"), PRESERVED);
writeFileSync(join(vendor, "reflect.txt"), REFLECTED);
writeFileSync(join(vendor, "apart.txt"), APART);
copyFileSync(
    join(launcher, "versions", version, `${version}.jar`),
    join(vendor, `${version}.jar`),
);

let libraries = 0;
for (const library of manifest.libraries as Library[]) {
    if (
        library.natives ||
        library.rules ||
        SKIPPED.some((name) => library.name.startsWith(name))
    ) {
        continue;
    }
    const path = library.downloads?.artifact?.path ?? mavenPath(library.name);
    copyFileSync(
        join(launcher, "libraries", path),
        join(vendor, path.split("/").pop()!),
    );
    libraries++;
}

// laid out as the launcher lays them out; the game finds them through the index, as files
const assets = join(launcher, "assets");
const indexName = join("indexes", `${manifest.assetIndex.id}.json`);
mkdirSync(join(vendor, "assets", "indexes"), { recursive: true });
copyFileSync(join(assets, indexName), join(vendor, "assets", indexName));
const index = JSON.parse(readFileSync(join(assets, indexName), "utf8"));
let objects = 0;
for (const { hash } of Object.values(
    index.objects as Record<string, { hash: string }>,
)) {
    const object = join("objects", hash.slice(0, 2), hash);
    mkdirSync(dirname(join(vendor, "assets", object)), { recursive: true });
    copyFileSync(join(assets, object), join(vendor, "assets", object));
    objects++;
}

console.log(
    `vendor/ holds ${version}, ${libraries} libraries and ${objects} assets`,
);

function mavenPath(coordinate: string): string {
    const [group, artifact, release] = coordinate.split(":");
    return `${group.replaceAll(".", "/")}/${artifact}/${release}/${artifact}-${release}.jar`;
}

function defaultLauncher(): string {
    if (platform() === "darwin") {
        return join(homedir(), "Library", "Application Support", "minecraft");
    }
    if (platform() === "win32") {
        return join(process.env.APPDATA ?? homedir(), ".minecraft");
    }
    return join(homedir(), ".minecraft");
}
