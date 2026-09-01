# Krimson

A library for creating custom blocks in a Spigot/Paper-based Minecraft server environment.

Future library to add UI, custom blocks, custom models + animations + sounds with BDEngine, sounds and more!

The goal of this API is to be fully extendable and customizable to your usage, and always free!
Any contribution is welcome!

## Roadmap

- [x] Custom blocks
  - [x] Item display blocks
  - [x] Noteblock blocks
  - [ ] Mushroom blocks (don't think that's a good idea actually lol)
- [x] Custom models
  - [x] BDEngine
    - [x] "Custom" textures
    - [x] Custom animations
    - [x] "Custom" sounds
  - [ ] Blockbench (some calculations errors)
    - [x] Custom textures
    - [x] Custom animations
- [x] Custom sounds
- [ ] Custom UI (more support for positioning and something else?)
- [x] Custom items
- [ ] Custom enchantments?
- [ ] Split into modules
- [ ] Ore generation
- [ ] Localized items/blocks name
- [ ] Ensure two and more plugins using Krimson don't conflict

## Known issues
- Entities lose their behaviour and model on restart.

## Showcases
![Screenshot of the custom block, BDEngine and custom UI](assets/block-bdengine-ui.png)
![Screenshot of blockbench loader, almost finished](assets/blockbench-almost-finished.png)

---

## License

GPL-3.0 license, see [LICENSE](./LICENSE).