scoreboard players set #setup xtoxray_test 1
gamerule doDaylightCycle false
time set noon
weather clear
worldborder center 0 0
worldborder set 128
fill -24 0 -24 23 7 23 minecraft:stone
fill -24 28 -24 23 35 23 minecraft:stone
fill -24 8 -24 23 27 -17 minecraft:stone
fill -24 8 16 23 27 23 minecraft:stone
fill -24 8 -16 -17 27 15 minecraft:stone
fill 16 8 -16 23 27 15 minecraft:stone
fill -16 8 -16 15 27 15 minecraft:air
fill -4 8 16 3 15 23 minecraft:air
fill -24 8 -16 23 27 -15 minecraft:stone

fill -20 20 -18 -17 23 -18 minecraft:coal_ore
fill -12 20 -18 -9 23 -18 minecraft:iron_ore
fill -4 20 -18 -1 23 -18 minecraft:copper_ore
fill 4 20 -18 7 23 -18 minecraft:gold_ore
fill 12 20 -18 15 23 -18 minecraft:redstone_ore

fill -20 13 -18 -17 16 -18 minecraft:lapis_ore
fill -12 13 -18 -9 16 -18 minecraft:diamond_ore
fill -4 13 -18 -1 16 -18 minecraft:ancient_debris

fill -20 20 -20 -17 23 -20 minecraft:deepslate
fill -16 20 -20 -13 23 -20 minecraft:deepslate_diamond_ore
fill -12 20 -20 -9 23 -20 minecraft:deepslate_redstone_ore
fill -8 20 -20 -5 23 -20 minecraft:deepslate_gold_ore
fill -4 20 -20 -1 23 -20 minecraft:deepslate_iron_ore
fill 0 20 -20 3 23 -20 minecraft:deepslate_copper_ore
fill 4 20 -20 7 23 -20 minecraft:deepslate_lapis_ore
fill 8 20 -20 11 23 -20 minecraft:deepslate_coal_ore

fill -23 9 -14 -19 12 -14 minecraft:stone
fill -17 9 -14 -13 12 -14 minecraft:glass
fill -11 9 -14 -7 12 -14 minecraft:obsidian

setblock -22 11 -14 minecraft:oak_sign[rotation=0]{front_text:{messages:['{"text":"ОРДИНАРНЫЕ БЛОКИ"}','','','']}}
setblock -16 11 -14 minecraft:oak_sign[rotation=0]{front_text:{messages:['{"text":"СТЕКЛО"}','','','']}}
setblock -10 11 -14 minecraft:oak_sign[rotation=0]{front_text:{messages:['{"text":"ОБСИДИАН"}','','','']}}

tp @a 0 12 10 180 0
