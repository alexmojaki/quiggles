package com.alexmojaki.quiggles

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SaveFileV1::class, name = "SaveFileV1")
)
abstract class SaveFile {
    abstract fun restore(drawing: Drawing)
}

data class SaveFileV1(val quiggles: List<Quiggle>) : SaveFile() {
    constructor(drawing: Drawing) : this(drawing.quiggles)

    override fun restore(drawing: Drawing) {
        for (quiggle in quiggles) {
            quiggle.restore(drawing.scenter)
        }
        drawing.quiggles.addAll(quiggles)
    }

}