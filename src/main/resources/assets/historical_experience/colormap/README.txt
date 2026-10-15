Historical Experience - extracted colour map values

These files hold the grass and foliage tint lookups of the original Minecraft clients. They were
EXTRACTED from those clients' own misc/grasscolor.png and misc/foliagecolor.png, not authored here,
and no original image is included in this repository.

Each .bin.gz is the client's own 65536-entry int array, written as RGB triples in the same order and
gzipped. The client builds that array with

    BufferedImage.getRGB(0, 0, 256, 256, pixels, 0, 256)

and indexes it with

    index = ((int) ((1.0 - rain * temp) * 255.0) << 8) | (int) ((1.0 - temp) * 255.0)

so entry i is row (i >> 8) = rainfall and column (i & 0xFF) = temperature, each 0..1.

Regenerate with:  java tools/GenerateHistoricalColormaps.java <libraries-dir> <output-dir>

grass_alpha_1_2_to_release_1_0.bin.gz
  contents: grass tint map, identical in every version from Alpha v1.2.0 to Release 1.0
  extracted from: misc/grasscolor.png in minecraft-1.0-client.jar (1.0)
  source jar sha256: 136e3dd54454e96175badf50bee2cdebdab9e7d66fee4fd6d135f39ead99eb58
  uncompressed: 196608 bytes, 65536 RGB triples
  uncompressed sha256: 69cbd59586464e5c6c39c6fccee6d42e4e1ec40aa34b7f096c2feffe68ba9f61
  gzip sha256: e250a1de5c4a52fcd12dcd63551adc934393c87e81c37a8a91239b51c3442503

foliage_alpha_1_2_to_beta_1_0.bin.gz
  contents: foliage tint map through Beta 1.0
  extracted from: misc/foliagecolor.png in minecraft-b1.0-client.jar (b1.0)
  source jar sha256: ad62df9cc678bfa15ff981f56aa8aa7ad0d60a1e584c39b981f9ec089ed450f0
  uncompressed: 196608 bytes, 65536 RGB triples
  uncompressed sha256: 79d59c199e36af634b7d79c1a3a4439ebcc8a9a74c3791a4b165847ae7d9a583
  gzip sha256: 1998941d0e471eabe2791998304d6e8932fbdf3667c92e9987ab63e9d7fb0a51

foliage_beta_1_2_to_release_1_0.bin.gz
  contents: foliage tint map from Beta 1.2
  extracted from: misc/foliagecolor.png in minecraft-1.0-client.jar (1.0)
  source jar sha256: 136e3dd54454e96175badf50bee2cdebdab9e7d66fee4fd6d135f39ead99eb58
  uncompressed: 196608 bytes, 65536 RGB triples
  uncompressed sha256: 88c1a0ea825d571d39253faf8dc335844b866bee859d9a49c8d59fe4e6f21bbd
  gzip sha256: 6557a009234896067e0d1275da57290ac1da241877a8453bbe2f17a834dfc18b

Verified identical across these clients (so only one copy of each is kept):
  grass   Alpha v1.2.6, Beta 1.0, 1.2_02, 1.3_01, 1.4_01, 1.5_01, 1.6.6, 1.7.3, 1.8-pre2, 1.8.1,
          1.9-pre6, Release 1.0
  foliage Alpha v1.2.6 and Beta 1.0 (the older map)
  foliage Beta 1.2_02, 1.3_01, 1.4_01, 1.5_01, 1.6.6, 1.7.3, 1.8-pre2, 1.8.1, 1.9-pre6, Release 1.0
