"""Diagnose map issues: duplicated placements, missing animals, misplaced buildings."""

# 1. Check for double-placed items between farm_map.txt and buildObjects()
# buildObjects() hardcodes these:
hardcoded_sprites = {
    "wood_pile@82,66",
    "plank_thin@84,66",
    "boat@8,33",           # addAnimated(getSceneryFrames("boat"), 8*TILE, 33*TILE, ...)
    "coracle_land@7,31",   # addObject(getCoracleLand(), 7*TILE, 31*TILE, ...)
}

# farm_map.txt also places:
map_sprites = []
with open(r'c:\Users\John Lawrence\OneDrive\Desktop\Anihan\res\maps\farm_map.txt', 'r') as f:
    for line in f:
        line = line.split('#')[0].strip()
        if not line:
            continue
        parts = line.split()
        if parts[0] in ('SPRITE', 'ANIM'):
            asset = parts[1]
            tx, ty = parts[2], parts[3]
            map_sprites.append((parts[0], asset, tx, ty))

print("=== farm_map.txt entries ===")
for kind, asset, tx, ty in map_sprites:
    print(f"  {kind} {asset} @ ({tx},{ty})")

print("\n=== Potential duplicates (hardcoded in buildObjects AND in farm_map.txt) ===")
for kind, asset, tx, ty in map_sprites:
    key = None
    if 'wood_pile' in asset and tx == '82' and ty == '66':
        key = "wood_pile@82,66"
    elif 'plank_thin' in asset and tx == '84' and ty == '66':
        key = "plank_thin@84,66"
    elif asset == 'boat' and tx == '8' and ty == '33':
        key = "boat@8,33"
    elif 'coracle_land' in asset and tx == '7' and ty == '31':
        key = "coracle_land@7,31"
    if key:
        print(f"  DUPLICATE: {kind} {asset} @ ({tx},{ty})")

# 2. Animals loaded but never placed
print("\n=== Animals loaded in AssetManager but NEVER placed in TileManager ===")
animals = ['chicken', 'cow', 'sheep', 'pig']
scenery_animals = ['duck', 'bird', 'blinking']

with open(r'c:\Users\John Lawrence\OneDrive\Desktop\Anihan\src\managers\TileManager.java', 'r') as f:
    tm = f.read()

for a in animals:
    if f'get{a.capitalize()}Frames' not in tm and f'getChicken' not in tm.replace(a,''):
        pass
    # Actually check if any of these getters are called
    getter = f'get{a[0].upper()}{a[1:]}Frames'
    if getter in tm:
        print(f"  {a}: getter {getter} IS called in TileManager (OK)")
    else:
        print(f"  {a}: getter {getter} NOT called in TileManager (MISSING!)")

for a in scenery_animals:
    if f'"{a}"' in tm or f"'{a}'" in tm:
        print(f"  {a}: scenery key '{a}' IS referenced in TileManager (OK)")
    else:
        print(f"  {a}: scenery key '{a}' NOT referenced in TileManager (MISSING!)")

# 3. Check the pasture area
print("\n=== Pasture pen boundaries ===")
print("  Fenced area: x=28..42, y=29..38")
print("  Egg spots:", [(31,35),(35,31),(40,36)])
print("  Milk spots:", [(33,33),(38,34),(30,37)])
print("  NOTE: (35,31) is at y=31 which is INSIDE the pasture")
print("  NOTE: Animals should be rendered inside pasture but currently aren't!")
