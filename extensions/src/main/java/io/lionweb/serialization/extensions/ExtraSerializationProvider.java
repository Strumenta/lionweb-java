package io.lionweb.serialization.extensions;

import io.lionweb.LionWebVersion;
import io.lionweb.serialization.SerializationProvider;
import javax.annotation.Nonnull;

public class ExtraSerializationProvider extends SerializationProvider {
  public static ExtraFlatBuffersSerialization getExtraStandardFlatBuffersSerialization() {
    ExtraFlatBuffersSerialization serialization = new ExtraFlatBuffersSerialization();
    standardInitialization(serialization);
    return serialization;
  }

  public static ExtraFlatBuffersSerialization getExtraStandardFlatBuffersSerialization(
      @Nonnull LionWebVersion lionWebVersion) {
    ExtraFlatBuffersSerialization serialization = new ExtraFlatBuffersSerialization(lionWebVersion);
    standardInitialization(serialization);
    return serialization;
  }

  public static ExtraProtoBufSerialization getExtraStandardProtoBufSerialization() {
    ExtraProtoBufSerialization serialization = new ExtraProtoBufSerialization();
    standardInitialization(serialization);
    return serialization;
  }

  public static ExtraProtoBufSerialization getExtraStandardProtoBufSerialization(
      @Nonnull LionWebVersion lionWebVersion) {
    ExtraProtoBufSerialization serialization = new ExtraProtoBufSerialization(lionWebVersion);
    standardInitialization(serialization);
    return serialization;
  }
}
