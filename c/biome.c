#include <stdint.h>
#include <stdio.h>

typedef uint64_t OneBitBiomes;

// should be 5x5x5, someday
// we'll just use 3x3x3

typedef uint64_t
    SingleBiomeIn2x2Bitmask; // whether (x,y,z) -> (x+1,y+1,z+1) is the same

// const uint64_t xmask = 0b111;
// const uint64_t xzmask = (xmask | (xmask << 4)) | ((xmask | (xmask << 4)) <<
// 8); const uint64_t xyzmask = (xzmask | (xzmask << 16)) | ((xzmask | (xzmask
// << 16)) << 32);

const uint64_t xyzmask = 0x7777777777777777;

SingleBiomeIn2x2Bitmask calculateMaskFor0thBiome(OneBitBiomes biomes) {
  SingleBiomeIn2x2Bitmask biomeMasks[8] = {
      ((biomes >> 0) & xyzmask),  // x     y     z
      ((biomes >> 1) & xyzmask),  // x + 1 y     z
      ((biomes >> 4) & xyzmask),  // x     y     z + 1
      ((biomes >> 5) & xyzmask),  // x + 1 y     z + 1
      ((biomes >> 16) & xyzmask), // x     y + 1 z
      ((biomes >> 17) & xyzmask), // x + 1 y + 1 z
      ((biomes >> 20) & xyzmask), // x     y + 1 z + 1
      ((biomes >> 21) & xyzmask), // x + 1 y + 1 z + 1
  };

  printf("%064lb %064lb \n%064lb %064lb \n%064lb %064lb \n%064lb %064lb\n",
         biomeMasks[0], biomeMasks[1], biomeMasks[2], biomeMasks[3],
         biomeMasks[4], biomeMasks[5], biomeMasks[6], biomeMasks[7]);

  return biomeMasks[0] & biomeMasks[1] & biomeMasks[2] & biomeMasks[3] &
         biomeMasks[4] & biomeMasks[5] & biomeMasks[6] & biomeMasks[7];
}

OneBitBiomes setBiome(OneBitBiomes biome, int x, int y, int z) {
  return biome | (0x1 << (((y & 0x3) << 4) | ((z & 0x3) << 2) | (x & 0x3)));
}

void printBiomeRange(int ix, int iy, int iz) {
  int px = (ix - 2) >> 2;
  int py = (iy - 2) >> 2;
  int pz = (iz - 2) >> 2;
  printf("(%02d %02d %02d) -> (%02d %02d %02d)\n", px, py, pz, px + 1, py + 1,
         pz + 1);
}

int main() {

  for (int x = 0; x < 16; x++) {
    printBiomeRange(x, 0, 0);
  }

  OneBitBiomes biomes = 0x0;

  for (int x = 0; x < 2; x++) {
    for (int z = 0; z < 2; z++) {
      for (int y = 0; y < 2; y++) {
        biomes = setBiome(biomes, x, y, z);
      }
    }
  }

  printf("%064lb \n", biomes);
  printf("%064lb \n", calculateMaskFor0thBiome(biomes));

  return 0;
}