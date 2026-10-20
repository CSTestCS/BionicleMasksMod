# Bionicle Masks: Kanoka & Kanohi

A NeoForge mod for **Minecraft 26.3** that adds every Kanoka disk, every Kanohi mask (56 of them, in
every form) and the forges to make them - using vanilla materials instead of protodermis.

## Getting the jar

Every push is built by GitHub Actions (`.github/workflows/build.yml`). Open the latest run under the
repository's **Actions** tab and download the `bioniclemasks-jar` artifact. To build it yourself you
need Java 25: `./gradlew build` (the jar lands in `build/libs/`).

## Kanoka disks

Press disks in the **Kanoka Disk Forge** (iron ingots, stone bricks, an anvil):

| Slot | What goes in |
| --- | --- |
| Material | Planks (level 1), Gold (2), Cobblestone (3), Copper (4), Iron (5), Diamond (7), Netherite (8) |
| Power | Fermented Spider Eye = Reconstitutes at Random, Packed Ice = Freeze, Rotten Flesh = Weaken, Honey Bottle = Remove Poison, Bone Meal = Enlarge, Pufferfish = Shrink, Glistering Melon = Regenerate, Ender Pearl = Teleport |
| Additives (optional, x2) | Glowstone = Empowered (+1 level), Redstone = Lasting, Amethyst = Resonant (area), Quartz = Swift, Slime Ball = Bouncing, Obsidian = Heavy, Breeze Rod = Gale, Honeycomb = Preserved (unbreakable), Emerald = Fortunate (may strike twice), Echo Shard = Silent |
| Metru (optional additive) | Blaze Powder = Ta, Prismarine Shard = Ga, Clay Ball = Po, Snowball = Ko, Feather = Le, Glow Ink Sac = Onu. Otherwise the biome decides. |

Every disk has a real three-digit Kanoka code (Metru, power, level). Each Metru's disks fly the way
the lore describes: Ta smashes through weak obstacles, Ga steers toward where you look, Po deflects
projectiles, Ko dodges the first obstacle, Le flies farther, Onu returns to the thrower.
Right-click a disk to throw it.

A **netherite** disk pressed with a **Nether Star** becomes one of the six level-9 **Great Disks**
(codes 159, 279, 339, 429, 589, 619).

## Kanohi masks

Melt disks down in the **Kanohi Mask Forge** (bricks, iron, blast furnace, lava bucket). It needs fuel
(coal, charcoal, blaze rods/powder, fire charges or lava). Put disks in the six slots and press **Forge**;
hover the button to preview the result.

* Every mask has a recipe: a unique combination of three disk powers (see `KanohiPower.java`).
* The disks' average power level sets the form: below 2.5 Powerless (Matoran), below 6.5 Noble (Turaga),
  otherwise Great (Toa).
* **Mismatched disk materials**: only a 25% chance the mask holds any power. If it does, it is made of one
  of the disks' materials at random; if not, it comes out as a plain powerless mask.
* Legendary masks: six Great Disks make the **Disk of Time**, which carved alone makes the **Vahi**.
  The **Ignika** takes six level-8+ Regenerate disks and a Totem of Undying; the **Mask of Creation**
  takes six level-8+ disks (Reconstitute, Freeze, Remove Poison, Enlarge, Regenerate, Teleport) and a Nether Star.
* A finished mask plus a catalyst:
  * an **element** (Fire Charge, Water Bucket, Wind Charge, Rooted Dirt, Smooth Stone, Blue Ice, Lightning
    Rod, Glowstone, Sculk, Magma Cream, Note Block, Heavy Core, Lodestone, Iron Block, Moss Block, Eye of
    Ender) or a **crystal** (Amethyst, Quartz, Prismarine Crystals, Echo Shard, Diamond, Emerald, Lapis,
    End Crystal) infuses it - the upper 30% of the mask turns into that element or crystal and it gains a boost;
  * any **dye** dyes it (vanilla color mixing);
  * **Dragon's Breath** turns a Great Toa Mata mask into a **Kanohi Nuva**, which shares its power with allies.
* All six Great Toa Mata masks plus a **Block of Gold** make a **Golden Kanohi** that holds all six powers.

Wear a mask and press **V** to use its power (sneak for the alternate use where a mask has one).
Press **G** to switch powers on a Golden Kanohi.

## Notes

* `tools/gen_assets.py` regenerates every texture and JSON file from `KanohiPower.java`.
* `tools/check_lore.sh` checks the lore rules (unique recipes, the 25% mismatch rule, Great Disk codes)
  with plain `javac`.
