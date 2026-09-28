// XtoXray Test Lab
// Этот скрипт устанавливается только в тестовую инстанцию XtoXray.

function run(server, command) {
  server.runCommandSilent(command);
}

function place(server, x, y, z, blockId) {
  run(server, `setblock ${x} ${y} ${z} ${blockId} replace`);
}

function buildStaticOreWall(server) {
  // Толстая каменная стена. Руды находятся внутри, а не на лицевой стороне.
  run(server, 'fill -32 18 -8 31 55 -6 minecraft:stone');

  const ores = [
    ['minecraft:coal_ore', -28, 50],
    ['minecraft:iron_ore', -24, 50],
    ['minecraft:copper_ore', -20, 50],
    ['minecraft:gold_ore', -16, 50],
    ['minecraft:redstone_ore', -12, 50],
    ['minecraft:lapis_ore', -8, 50],
    ['minecraft:diamond_ore', -4, 50],
    ['minecraft:emerald_ore', 0, 50],
    ['minecraft:deepslate_coal_ore', 4, 50],
    ['minecraft:deepslate_iron_ore', 8, 50],
    ['minecraft:deepslate_copper_ore', 12, 50],
    ['minecraft:deepslate_gold_ore', 16, 50],
    ['minecraft:deepslate_redstone_ore', 20, 50],
    ['minecraft:deepslate_lapis_ore', 24, 50],
    ['minecraft:deepslate_diamond_ore', 28, 50],
    ['minecraft:ancient_debris', -28, 44]
  ];

  for (const [id, x, y] of ores) {
    place(server, x, y, -7, id);
  }

  // Небольшие жилы для проверки VeinMiner и визуального выделения нескольких блоков.
  const vein = [
    ['minecraft:diamond_ore', -18, 36],
    ['minecraft:diamond_ore', -17, 36],
    ['minecraft:diamond_ore', -16, 36],
    ['minecraft:diamond_ore', -18, 35],
    ['minecraft:diamond_ore', -17, 35],
    ['minecraft:diamond_ore', -16, 35],
    ['minecraft:gold_ore', 6, 36],
    ['minecraft:gold_ore', 7, 36],
    ['minecraft:gold_ore', 8, 36],
    ['minecraft:gold_ore', 7, 35]
  ];

  for (const [id, x, y] of vein) {
    place(server, x, y, -7, id);
  }
}

function buildMaterialTests(server) {
  run(server, 'fill -32 20 8 31 35 10 minecraft:stone');
  run(server, 'fill -32 20 11 31 35 13 minecraft:glass');
  run(server, 'fill -32 20 14 31 35 16 minecraft:obsidian');
  run(server, 'fill -32 20 20 31 35 22 minecraft:deepslate');
  run(server, 'fill -32 20 23 31 35 25 minecraft:stone');
}

function buildModOreGallery(server) {
  const ids = Utils.getRegistryIds('minecraft:block');
  const ores = [];
  const seen = new Set();

  for (const value of ids) {
    const id = String(value);
    if (!id.includes(':')) continue;

    const path = id.substring(id.indexOf(':') + 1).toLowerCase();

    const looksLikeOre =
      path.includes('ore') ||
      path.includes('hematite') ||
      path.includes('limonite') ||
      path.includes('magnetite') ||
      path.includes('bismuthinite') ||
      path.includes('garnierite') ||
      path.includes('cassiterite') ||
      path.includes('malachite') ||
      path.includes('tetrahedrite') ||
      path.includes('sphalerite') ||
      path.includes('chromite') ||
      path.includes('uraninite') ||
      path.includes('native_copper') ||
      path.includes('native_gold') ||
      path.includes('native_silver');

    if (looksLikeOre && !seen.has(id)) {
      seen.add(id);
      ores.push(id);
    }
  }

  ores.sort((a, b) => a.localeCompare(b));

  const columns = 16;
  const colStep = 4;
  const startX = -30;
  const startY = 24;
  const z = 27;
  const maxEntries = 176;

  run(server, 'fill -32 20 26 31 61 28 minecraft:stone');

  let placed = 0;

  for (let i = 0; i < ores.length && placed < maxEntries; i++) {
    const x = startX + (placed % columns) * colStep;
    const y = startY + Math.floor(placed / columns) * 3;

    if (y > 58) continue;

    // Два блока камня перед рудой, чтобы тестировать просмотр сквозь стену.
    place(server, x, y, z - 1, ores[i]);
    placed++;
  }

  console.log(`[XtoXray Test] Найдено рудоподобных блоков: ${ores.length}. Размещено: ${placed}.`);
}

ServerEvents.loaded(event => {
  const server = event.server;

  run(server, 'gamerule doDaylightCycle false');
  run(server, 'gamerule doWeatherCycle false');
  run(server, 'gamerule doMobSpawning false');
  run(server, 'gamerule keepInventory true');
  run(server, 'gamerule doFireTick false');

  run(server, 'difficulty peaceful');
  run(server, 'time set noon');
  run(server, 'weather clear');

  run(server, 'worldborder center 0 0');
  run(server, 'worldborder set 128');

  // Пересобираем только тестовую арену. Остальные миры не затрагиваются.
  run(server, 'fill -40 10 -40 39 62 39 minecraft:air');
  run(server, 'fill -40 10 -40 39 10 39 minecraft:stone');

  run(server, 'fill -40 11 -40 -39 62 39 minecraft:stone');
  run(server, 'fill 38 11 -40 39 62 39 minecraft:stone');
  run(server, 'fill -40 11 -40 39 62 -39 minecraft:stone');
  run(server, 'fill -40 11 38 39 62 39 minecraft:stone');

  // Потолок намеренно отсутствует. Здесь проверяются солнце и луна.
  buildStaticOreWall(server);
  buildMaterialTests(server);
  buildModOreGallery(server);

  run(server, 'gamemode creative @a');
  run(server, 'tp @a 0 35 30 180 0');

  run(server, 'title @a title {"text":"XtoXray TEST LAB","color":"aqua"}');
  run(server, 'title @a subtitle {"text":"Свежая сборка XtoXray","color":"white"}');
});
