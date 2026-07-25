# Krimson — display & animation engine migration

Replaces the broken `ItemDisplay` positioning, bone-hierarchy transforms and animation
engine with the verified logic, keeping the runtime resource-pack generator, the texture
pipeline and the project's architectural conventions intact.

**Files changed (5).** Everything else is untouched.

| File | Nature of change |
|---|---|
| `models/bbmodel/BBModelBaker.java` | Transform math + animation baking rewritten. Pack generation (`generateItemModelJson`, `elementToJson`, `toVanillaAxisAngle`, `buildDisplayItem`) preserved. |
| `models/bbmodel/BBAnimation.java` | Interpolation/loop-mode enums, cached sorted channels, moved-bone detection. |
| `models/bbmodel/BBModelParser.java` | Feeds the new enums; finalises channels; safer molang fallback. ~20 lines. |
| `models/bbmodel/BBBone.java` | Javadoc only — two comments described behaviour that is no longer true. |
| `models/BlockDisplayModel.java` | Spawn path, display properties, animation player. Legacy BDEngine/command paths untouched. |

**Not touched:** `resourcepack/creator/ResourcePack.kt`, `BBModelAssets.java`,
`BBElement.java`, `Models.java`, `DynamicPackResolver`, `ResourcePackHosting`, and every
public method signature the test plugin calls (`spawn`, `playAnimation`,
`playAnimationLoop`, `removeModelInstance`, `getAvailableAnimations`). `PluginModels` and
`ModelInteractionListener` need no edits.

---

## Root causes, measured

Each was isolated by re-implementing the current Java math and the corrected math against
a reference implementation, then measuring worst-case per-vertex world-space error over
all 23 animations in `the_world.bbmodel` at 9 sample times each.

| # | Root cause | Worst-case error | Visible symptom |
|---|---|---|---|
| 1 | Geometry is baked relative to the bone pivot, but `item_display` renders its model **centred on the entity** — model coord 0 lands at −0.5 blocks, not at the entity origin. Nothing compensated for it. | 0.50 blocks at rest, **0.87 rotating** | Whole model offset half a block; parts swing away from their pivot as they rotate |
| 2 | `Quaternionf.rotateXYZ` gives `Rx·Ry·Rz`; Blockbench and vanilla `ModelPart` use **`Rz·Ry·Rx`**. | **1.63 blocks** | Limbs twist wrongly whenever a bone rotates on more than one axis |
| 3 | FK carried a quaternion + a separate scale vector, so a parent's scale never reached its children's pivots. | **0.72 blocks** | `kick_barrage` scales 1.3× on Y and its four children detach |
| 4 | `catmullrom` keyframes sampled as `linear`. 1566 of this model's 1813 keyframes are catmullrom. | 0.11 blocks | Angular, snappy motion instead of smooth arcs |

After all four fixes the corrected math reproduces the reference to **0.000000000 blocks**
across every animation and sample time.

Bug 3 is the reason the fix uses a `Matrix4f` chain rather than a repaired quaternion
chain: a parent with **non-uniform** scale followed by a rotating child requires
`R_parent · S_parent · R_child`, and no quaternion-plus-scale representation can express
that. `kick_barrage` is exactly that case. A matrix chain gets it right for free.

Two further defects, found while reading rather than by measurement:

5. **Hidden bones were dropped entirely** (`if (!bone.visible) return;` in `bakeBindPose`),
   so no item model was generated and no entity spawned for them. `Barrage`,
   `BarrageCharge` and `KickBarrage` animate those bones, so those three animations had
   nothing to move.
6. **`Animator.channel(String)` rebuilt and re-sorted an `ArrayList` on every call** —
   once per bone, per channel, per baked tick. For this model that is ~87 000 throwaway
   lists and sorts at load.

---

## Step-by-step

### 1. `BBAnimation.java` — make the data model carry what the engine needs

* Added `LoopMode` (`LOOP`/`ONCE`/`HOLD`), `Interpolation` (`LINEAR`/`CATMULLROM`/`STEP`)
  and `Channel` (`POSITION`/`ROTATION`/`SCALE`, each with its **rest value** — 1 for
  scale, 0 otherwise). `Keyframe.channel` and `.interpolation` are now these enums instead
  of raw strings, so a typo can't silently become a linear keyframe.
* `Animator.finishLoading()` sorts each channel **once**; `channel(Channel)` returns the
  cached immutable list. Fixes defect 6.
* `movedBoneUuids()` returns the bones whose keyframes actually leave the rest pose. This
  drives hidden-bone reveal (step 3).

### 2. `BBModelParser.java` — feed the enums

* Keyframes resolve their `Channel` via `Channel.fromJson`; unknown channels
  (`effect`, `sound`, `timeline`) are skipped instead of being coerced to `rotation`.
* `animator.finishLoading()` is called once the animator is fully parsed.
* `parseMaybeMath` now takes a fallback and uses the channel's **rest value**. Previously
  an unparseable value returned `0`, which on the scale channel would have collapsed the
  bone to nothing.

### 3. `BBModelBaker.java` — the transform rewrite

* **`solveWorldMatrices` / `solveBone`** replace `bakeBoneAtTime`. Per bone:
  `world = parent · T(pivot + position) · Rz · Ry · Rx · S · T(−pivot)`, all in Blockbench
  units. This fixes bugs 2 and 3 at once, and matches vanilla `ModelPart` ordering.
* **`toDisplayTransform`** is the single place where units convert and where bug 1 is
  fixed. It keeps the world matrix's linear part, sets the translation to
  `world · pivot / 16`, then applies `translate(+0.5, +0.5, +0.5)` to undo the item-display
  centring. The compensation is the named constant `ITEM_DISPLAY_CENTER_OFFSET` — if your
  build ever anchors item displays differently, that constant is the only thing to change,
  and the pack stays as-is.
* **`sample`** implements linear, catmullrom (uniform Catmull-Rom, Blockbench's "smooth")
  and step, using the interpolation of the keyframe that *starts* the segment. Fixes bug 4.
* **`POSITION_ANIM_SCALE` is gone.** Its javadoc claimed position keyframes were in blocks
  while the code divided by 16. They are in Blockbench units, the same as pivots, so they
  now add straight onto the pivot and the single `/16` happens in `toDisplayTransform`.
* **Bind pose and animation frames now share one code path** — `bakeBindPose` calls
  `solveWorldMatrices(model, null, 0)`. The rest pose can no longer disagree with tick 0
  of an animation.
* **Hidden bones are baked and flagged**, not skipped: `BakedPart.visibleByDefault`
  carries Blockbench's group visibility. Fixes defect 5. For `the_world` the pack goes
  from 16 to 26 item models (the 10 extra are the six "BAM" arms and four barrage legs).
  *This is the one change that touches pack output, and it is the carve-out your rules
  allow for model display IDs — without those models the three barrage animations cannot
  render.*
* **`bakeAnimations` returns `BakedAnimation`** (frames + loop mode + revealed part tags)
  instead of a bare tick map.
* **Loop-aware frame counts:** a looping animation is baked over `[0, totalTicks)` and
  sampled on a normalised grid so the cycle's period stays exactly as authored and the
  wrap is seamless. Baking `[0, totalTicks]` inclusive replays the same pose twice at the
  seam, which reads as a one-tick stall every cycle. Non-looping animations keep the 20 Hz
  grid and clamp on the final frame.

### 4. `BlockDisplayModel.java` — spawn and playback

* **`spawn`** now applies `applyDisplayDefaults` to every part: `setPersistent(false)`,
  `setBillboard(FIXED)`, `setInterpolationDelay(0)`,
  `setInterpolationDuration(FRAME_INTERPOLATION_TICKS)`, `setViewRange(1.0f)`.
  `Billboard.FIXED` is set explicitly because any other value rotates parts toward each
  viewer, which destroys a rig whose orientation lives in its matrix.
* **Hidden parts are spawned but parked** at `HIDDEN_TRANSFORM` (zero scale). Masking by
  scale avoids a spawn/despawn cycle — and the packets that come with it — every time an
  animation reveals or re-hides a bone.
* **One animation engine instead of two.** `playAnimation` and `playAnimationLoop` were
  ~95% duplicated; both now delegate to `startAnimation`, with `playAnimationLoop` forcing
  `LOOP` and `playAnimation` honouring the model's declared mode. Public signatures are
  unchanged.
* **Loop modes are honoured:** `LOOP` wraps, `HOLD` freezes on the last frame, `ONCE`
  restores the bind pose and stops. Legacy BDEngine models default to `HOLD`, which is
  exactly what the old `playAnimation` did, so their behaviour is unchanged.
* **Reveal / re-park per animation:** on start, active parts are
  `visibleByDefault ∪ revealedPartTags`; everything else is parked immediately with
  interpolation 0 so it snaps out of sight, and frames for inactive parts are skipped.
* **`AnimationRunner`** is a named inner class rather than a lambda, because the runnable
  has to cancel its own task. It also self-cancels if every display of the instance has
  become invalid, so deleting a model can't leave a task pushing packets at nothing.
* **`applyTransform` writes interpolation settings *before* the matrix.** In the other
  order the client interpolates toward the new pose using the previous frame's settings.
* **Scheduling now uses `KrimsonPlugin.getScheduler()`** (UniversalScheduler), matching
  `CustomBlockTracker`, `CustomBlock` and the listeners, instead of `BukkitRunnable`. This
  also makes the engine Folia-safe, consistent with why the dependency is shaded in the
  first place.

---

## One deliberate deviation, easy to revert

`elementToJson` now applies `BBElement.inflate` to the baked `from`/`to`. The previous
code parsed `inflate` and never used it, so **39 of this model's 75 cubes** rendered at the
wrong size, and coincident faces z-fought — flicker that reads as "glitchy". This is the
only change to geometry output, it's 4 lines, and reverting it is deleting them:

```java
if (el.inflate != 0f) {
    from.sub(el.inflate, el.inflate, el.inflate);
    to.add(el.inflate, el.inflate, el.inflate);
}
```

---

## Known remaining gap (not fixed — it would require reshaping the pack)

Element-level rotation is still snapped to one axis and one of `{-45, -22.5, 0, 22.5, 45}`
by `toVanillaAxisAngle`, because a vanilla JSON element cannot express free rotation.
**36 of 75 cubes** in `the_world` have arbitrary element rotations, so they are visibly
mis-angled regardless of how correct the bone math is.

The only faithful fix is to stop baking element rotation into the geometry: split each
bone's cubes into groups sharing the same rotation/pivot, give each group its own item
model and display entity, and carry the rotation in that entity's matrix instead. That
changes part identity and model keys — i.e. it reshapes the pack — so it's out of scope
here. For this model it would raise the entity count from 26 to about 50 per instance.
Say the word and I'll do it as a follow-up.

Also unchanged and worth knowing: `collectInstanceDisplays` still scans `world.getEntities()`
once per `startAnimation` call, as before. Fine at this scale; if you end up with many
concurrent rigs, cache `instanceId -> List<Display>` at spawn.

---

## How this was verified

- The reference implementation was validated independently: a from-scratch bbmodel loader,
  animation sampler and software rasteriser, rendered to images to confirm the hierarchy,
  transform order, Catmull-Rom sampling and box-UV convention. Its geometry was then
  reproduced through the display-transform formula and compared quad-by-quad — identical.
- The current Krimson math and each candidate fix were run against that reference; the
  error table above is that measurement. All four fixes together give exactly zero error.
- All five files parse cleanly under a Java 21 grammar (records, switch expressions,
  pattern `instanceof`), and every cross-file symbol reference was checked to resolve.
- **Not compiled or run.** There is no JDK or Paper artifact in the environment I worked
  in, so `./gradlew build` has not been executed and nothing has been seen in-game. The
  math is what I could verify rigorously; the API surface I could only check by reading.

### Worth testing first, in this order

1. `/summon` a `the_world` instance — the rig should stand at the location with **no half-block
   offset** and correct proportions. This is bug 1.
2. Play `Idle` — smooth, no part drifting from its pivot.
3. Play `ChargePunch` — this was the worst multi-axis case (1.63 blocks). Limbs should
   track the Blockbench preview.
4. Play `Barrage`, then `KickBarrage`, then `Idle`. The ghost arms should appear only in
   `Barrage`, the four legs only in `KickBarrage`, and both should be gone again in `Idle`.
   That exercises the reveal/re-park path end to end.
5. Play a `hold` animation and confirm it freezes rather than snapping back; a `once`
   animation should return to the bind pose.
6. Regression: `SPEAKER` (legacy `/summon` model) and `ANIMATED_MODEL`/`STONE_GATE`/`READING`
   (BDEngine JSON) must behave exactly as before — they take the untouched code paths, but
   they do share `spawn` and the player, so they're the ones to check for accidental damage.
