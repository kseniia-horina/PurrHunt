package app.purrhunt.game.engine

/**
 * Detects the owner's exit gestures, which a cat is very unlikely to perform:
 * a finger held in the top right corner for [CORNER_HOLD] seconds, or three fingers held for [THREE_FINGER_HOLD].
 * The corner progress ring appears only after [RING_DELAY] seconds so a passing paw shows nothing.
 */
class OwnerExitDetector(
    private var width: Float,
    private var cornerSize: Float,
) {
    private val ids = IntArray(MAX_POINTERS) { -1 }
    private val xs = FloatArray(MAX_POINTERS)
    private val ys = FloatArray(MAX_POINTERS)
    private var cornerPointer = -1
    private var cornerSince = 0.0
    private var threeSince = -1.0

    var triggered = false
        private set

    /** 0..1 progress for the corner ring; 0 while hidden. */
    var cornerProgress = 0f
        private set
    var threeFingerProgress = 0f
        private set

    fun resize(width: Float, cornerSize: Float) {
        this.width = width
        this.cornerSize = cornerSize
    }

    fun reset() {
        ids.fill(-1)
        cornerPointer = -1
        threeSince = -1.0
        triggered = false
        cornerProgress = 0f
        threeFingerProgress = 0f
    }

    private fun inCorner(x: Float, y: Float) = x >= width - cornerSize && y <= cornerSize

    fun down(id: Int, x: Float, y: Float, time: Double) {
        val slot = ids.indexOf(-1)
        if (slot < 0) return
        ids[slot] = id
        xs[slot] = x
        ys[slot] = y
        if (cornerPointer == -1 && inCorner(x, y)) {
            cornerPointer = id
            cornerSince = time
        }
        if (count() >= 3 && threeSince < 0) threeSince = time
    }

    fun move(id: Int, x: Float, y: Float) {
        val slot = ids.indexOf(id)
        if (slot < 0) return
        xs[slot] = x
        ys[slot] = y
        if (id == cornerPointer && !inCorner(x, y)) cornerPointer = -1
    }

    fun up(id: Int) {
        val slot = ids.indexOf(id)
        if (slot >= 0) ids[slot] = -1
        if (id == cornerPointer) cornerPointer = -1
        if (count() < 3) threeSince = -1.0
    }

    fun update(time: Double) {
        if (cornerPointer != -1) {
            val held = (time - cornerSince).toFloat()
            cornerProgress = ((held - RING_DELAY) / (CORNER_HOLD - RING_DELAY)).coerceIn(0f, 1f)
            if (held >= CORNER_HOLD) triggered = true
        } else {
            cornerProgress = 0f
        }
        if (threeSince >= 0) {
            val held = (time - threeSince).toFloat()
            threeFingerProgress = (held / THREE_FINGER_HOLD).coerceIn(0f, 1f)
            if (held >= THREE_FINGER_HOLD) triggered = true
        } else {
            threeFingerProgress = 0f
        }
    }

    private fun count(): Int = ids.count { it != -1 }

    companion object {
        const val CORNER_HOLD = 3f
        const val RING_DELAY = 1f
        const val THREE_FINGER_HOLD = 2f
        private const val MAX_POINTERS = 10
    }
}
