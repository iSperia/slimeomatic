package com.h4games.slime.field

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.GameContext

class AnimatedActor(
    val context: GameContext,
    val id: String,
    val fps: Float
) : Actor() {

    val animation = Animation<TextureRegion>(fps, context.textures(id))

    var timePassed: Float = 0f

    var texture: Drawable? = null

    var inverted = false

    override fun act(delta: Float) {
        super.act(delta)
        timePassed += delta
        if (timePassed > animation.animationDuration) {
            remove()
        } else {
            texture = TextureRegionDrawable(animation.getKeyFrame(if (inverted) (animation.animationDuration - timePassed) else timePassed)).tint(color)
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        texture?.draw(batch, x, y, width, height)
    }
}