package `in`.dragonbra.vapulla.anim

import android.support.transition.ChangeBounds
import android.support.transition.Fade
import android.support.transition.TransitionSet

class AutoParallelTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(Fade(Fade.OUT))
                .addTransition(ChangeBounds())
                .addTransition(Fade(Fade.IN))
    }
}