package org.codeberg.zenxarch.zconfig.entry;

import java.util.List;

public interface ConfigEntry<T> {
  String key();

  String comment();

  List<String> incompats();

  T defaultValue();
}
