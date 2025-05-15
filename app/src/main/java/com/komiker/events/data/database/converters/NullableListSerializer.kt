package com.komiker.events.data.database.converters

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object NullableListSerializer : KSerializer<List<String>?> {

    private val listSerializer: KSerializer<List<String>> = serializer()

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NullableList") {
        element("value", listSerializer.descriptor, isOptional = true)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: List<String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(listSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): List<String>? {
        return decoder.decodeSerializableValue(serializer())
    }
}