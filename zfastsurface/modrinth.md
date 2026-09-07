Fast Surface is a vanilla worldgen optimization mod, that optimizes block collections in surface builder.

**Does the mod change world generation?** The mod maintains **vanilla parity**, including with any datapacks. Do keep in mind, [Vanilla non-determinism](https://bugs.mojang.com/browse/MC-55596).

**Can I use custom worldgen mods with this?** Yes, the mod will fallback to normal execution where it encounters modded content. Though it may not work with mods that add custom code to surface builder using mixins.

Fast Surface is part of [Reverie Projects](https://modrinth.com/organization/reverie), check out our other mods as well.

## How does this mod work?  

Fast Surface works by packing,rewriting and optimizing chunk data reads and writes during surface builder by replacing the buildSurface function with an optimized version.

## How fast is it?

About 40% faster surface builder function in overworld, about 170% faster in nether and just instantaneous in the end.

## Socials

[![Chat with us on discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/RFSmd7debX) [![Chat with me on mastodon](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/mastodon-singular_vector.svg)](https://mastodon.social/@ZenXArch) [![Chat with us on Gitter](https://raw.githubusercontent.com/offbeat-stuff/devins-badges/refs/heads/v3/assets/cozy/social/gitter_vector.svg)](https://matrix.to/#/#zenxarchs-modding-server:gitter.im)

## Credits
- No-one specific right now
- Thanks to everyone who reported bugs, and tested the mod

## Reverie Projects
Consider joining the [Reverie Projects](https://discord.gg/6pRkrYxbGW) for sneak peeks and updates on development! By joining, you can:
- Get installation help and technical support for alot of awesome projects
- Get the latest updates about development
- Talk with and collaborate with our team
- And just hang out with the rest of our community!