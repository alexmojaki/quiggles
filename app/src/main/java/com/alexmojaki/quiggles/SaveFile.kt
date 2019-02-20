package com.alexmojaki.quiggles

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "version"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SaveFileV1::class, name = "1")
)
abstract class SaveFile {
    abstract fun restore(drawing: Drawing)
}

data class SaveFileV1(val quiggles: List<Quiggle>, val stars: Boolean) : SaveFile() {
    constructor(drawing: Drawing) : this(drawing.quiggles, drawing.starField != null)

    override fun restore(drawing: Drawing) {
        for (quiggle in quiggles) {
            quiggle.restore(drawing.scenter)
        }
        drawing.quiggles.addAll(quiggles)

        if (stars) {
            drawing.starField = StarField(drawing.scenter)
        }
    }

}