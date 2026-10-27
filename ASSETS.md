# Routia / Ruti identity — 0.9.4

Ruti is the squirrel companion for small daily habits. Her acorn reserve is a metaphor for accumulating small actions, not currency or an extra achievement counter. Ruti welcomes, guides, focuses, rests and celebrates. This is product symbolism, not a scientific efficacy claim.

## Production assets

Created with built-in ImageGen, using the established original Ruti atlas as an identity reference. Cinnamon fur, ivory cheeks/belly, round ears, dark brown paws, a large curled tail, sage neckerchief and acorn clasp stay consistent. These are original app assets, not stock or another brand's character.

- drawable-nodpi/ruti_rig.webp — transparent 1536×1024 atlas containing neutral head, torso, tail, left/right arms, acorn, laughing head and left/right feet.
- drawable-nodpi/ruti_scenes.webp — transparent 1536×1024 atlas with six separate actions: welcome wave, reading a map/book, raised-arm cheer, curled-up rest, headphones/notebook focus, and putting an acorn in a basket.
- drawable-nodpi/ruti_icon_v2.webp — dedicated transparent 1254×1254 close-up foreground. Large face and acorn with tail framing; deliberately different composition from full-body scenes.
- drawable/ruti_launcher_background.xml — deep green gradient behind the close-up icon.
- drawable/ruti_launcher_foreground.xml — foreground inset for Android adaptive masks.
- drawable/ruti_monochrome.xml — close-up squirrel/acorn silhouette for Android themed icons.
- mipmap-anydpi and mipmap-anydpi-v33 — adaptive icon and monochrome override.

Old single-pose production atlas/icon files were removed. Assets are encoded as WebP with transparency preserved.

## Rendering and performance

Mascot.java selects six context illustrations and a separate compact avatar; it clips source neighbors near cell boundaries. RutiRig.java renders independent cutout parts and clips the neighboring hand out of the laughing-head region. Avatar source aspect ratio is preserved. An opaque fur neck underlap, sampled from the same atlas, sits behind the torso scarf and separately rotating head; it closes the jaw/scarf seam throughout head tilt and expression changes. The head artwork seats three units into the scarf, hiding the dark hollow opening in the source torso. Shoulder pivots are inside opaque torso fur. The previous single long arm cutouts are no longer rotated as whole limbs. Native Canvas draws short rounded upper-arm and forearm segments, textured from a solid fur region of the established atlas, plus separately cropped small paws. The shoulder/elbow/wrist positions are driven by two fixed 19-unit bones and wrist flex. Upper arms sit behind the torso; forearms and paws layer in front. Paws clap at the chest. After clapping, forearms turn upward while elbows return near the torso, then upper arms lift. In the raised pose, elbows are outside the shoulders, wrists above elbows, forearms nearly vertical and paws upright. The outward turn uses a depth component with fixed physical bone lengths; projected forearms can foreshorten. A single 2D bend-sign rule is not an anatomical validation. Clap light follows the transformed hand positions.

CelebrationMotion.java drives a 3.8-second articulated performance: notice/head tilt → two claps → arms raised → small vertical hop → grounded squash → alternating steps and tail follow-through → raised-arm finish. Head/torso/arms/feet/tail have independent transforms. Root position stays fixed, and the vertical flight returns to the foot anchor. This is not whole-image bobbing.

## Generation directions

1. Rig atlas: same Ruti identity; isolated body parts arranged with clear spacing; front-facing neutral and laughing heads; separate torso with scarf, tail, two arms, two feet and acorn; transparent background; no text/scenery.
2. Context atlas: same identity in six distinct full-body situations; varied expressions and body actions appropriate to welcome, explanation, celebration, rest, focus and accumulated progress; transparent background; no lettering.
3. Launcher portrait: same identity; premium close-up face/upper torso holding one acorn; large readable eyes and silhouette, tail framing left; transparent foreground; no text or baked-in frame/background. Android provides the green background and adaptive mask.

The layered source illustration is assembled and animated in native Canvas code. No runtime network or downloaded animation engine is used.
