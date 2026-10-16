<center> 
  
  [Keep Android Open | Contact Regulators to stop Google's lockdown of Android](https://keepandroidopen.org/)
  
</center>

Material Rule Compiler is an experimental worldgen optimization mod, that works to improve surface builder times by compiling material rules into static functions.

**Does the mod change world generation?** The mod maintains **vanilla parity**, including with any datapacks. Do keep in mind, [Vanilla non-determinism](https://bugs.mojang.com/browse/MC-55596).

**Can I use custom worldgen mods with this?** Yes, the mod will fallback to normal execution where it encounters modded content. Though, it is possible to have mod incompatibilites. I do test against major worldgen mods like terratonic etc.

> **Note** Fast Noise is needed for optimal speed

Material Rule Compiler is part of [Reverie Projects](https://modrinth.com/organization/reverie), check out our other mods as well.

<center>
  
  [![Available on codeberg](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/codeberg_vector.svg)](https://codeberg.org/ZenXArch/FastNoise) [![Available on curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/zmaterial-rule-compiler) [![Available on modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/zmaterial-rule-compiler) [![Support me on Ko-Fi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/I2I31KW58I)

</center>

## How does this mod work?  

Material rule compiler compiles material rules into classfiles using asm library, optimized for every unique set of biomes. It also reduces allocations for random splitters in material rules and predicts biomes for material rules.

## How fast is it?

About 30-40% decrease in surface builder execution times.

## Socials

[![Chat with us on discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/RFSmd7debX) [![Chat with me on mastodon](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/mastodon-singular_vector.svg)](https://mastodon.social/@ZenXArch) [![Chat with us on Gitter](https://raw.githubusercontent.com/offbeat-stuff/devins-badges/refs/heads/v3/assets/cozy/social/gitter_vector.svg)](https://matrix.to/#/#zenxarchs-modding-server:gitter.im)

## Credits
- [Ishland](https://modrinth.com/user/ishland) for suggesting method handle api
- [Taumc](https://modrinth.com/organization/taumc) members for discussing compat and surface rules
- [DJ](https://modrinth.com/user/ThatDeeJay) for testing and benchmarking

## Reverie Projects
Consider joining the [Reverie Projects](https://discord.gg/6pRkrYxbGW) for sneak peeks and updates on development! By joining, you can:
- Get installation help and technical support for alot of awesome projects
- Get the latest updates about development
- Talk with and collaborate with our team
- And just hang out with the rest of our community!