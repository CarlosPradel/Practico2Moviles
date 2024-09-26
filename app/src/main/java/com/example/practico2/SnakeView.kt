package com.example.practico2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

class SnakeView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private var canvas: Canvas? = null
    private var paint: Paint = Paint()
    private var surfaceHolder: SurfaceHolder = holder

    // coordenadas de la víbora
    private var snakeBody = mutableListOf<Pair<Int, Int>>()
    private var snakeDirection = Direction.RIGHT // Dirección inicial
    private var foodPosition = Pair(0, 0) // Posición inicial de la comida, se actualizará

    // Tamaño de los bloques que conforman la víbora y la comida
    private val blockSize = 50
    private var numBlocksWide = 0
    private var numBlocksHigh = 0

    // Velocidad de la víbora
    private var speed: Long = 200

    // Direcciones  para mover
    enum class Direction { UP, DOWN, LEFT, RIGHT }
    private var lastTouchTime: Long = 0
    private val minTouchInterval: Long = 200 // Intervalo mínimo entre toques (en milisegundos)


    init {
        // Asegura que el cálculo de bloques se haga solo cuando la vista tiene dimensiones
        viewTreeObserver.addOnGlobalLayoutListener {
            if (numBlocksWide == 0 || numBlocksHigh == 0) {
                numBlocksWide = width / blockSize
                numBlocksHigh = height / blockSize
                startGame() // Inicia el juego una vez que las dimensiones estén listas
            }
        }
    }

    private fun startGame() {
        snakeBody.clear()
        snakeBody.add(Pair(numBlocksWide / 2, numBlocksHigh / 2))
        snakeDirection = Direction.RIGHT
        foodPosition = generateFoodPosition()
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }


    private fun generateFoodPosition(): Pair<Int, Int> {
        return Pair(Random.nextInt(numBlocksWide), Random.nextInt(numBlocksHigh))
    }


    override fun run() {
        while (isPlaying) {
            updateGame()
            drawGame()
            Thread.sleep(speed)
        }
    }


    private fun updateGame() {
        synchronized(snakeBody) {
            if (snakeBody.isEmpty()) {
                return
            }

            val head = snakeBody.first() // cabeza de la víbora
            val newHead = when (snakeDirection) {
                Direction.UP -> Pair(head.first, (head.second - 1 + numBlocksHigh) % numBlocksHigh)
                Direction.DOWN -> Pair(head.first, (head.second + 1) % numBlocksHigh)
                Direction.LEFT -> Pair((head.first - 1 + numBlocksWide) % numBlocksWide, head.second)
                Direction.RIGHT -> Pair((head.first + 1) % numBlocksWide, head.second)
            }

            // la víbora choca consigo misma
            if (snakeBody.contains(newHead)) {
                isPlaying = false
                return
            }

            //la víbora ha comido la comida
            if (newHead == foodPosition) {
                snakeBody.add(0, newHead)
                foodPosition = generateFoodPosition()
            } else {
                snakeBody.add(0, newHead)
                snakeBody.removeAt(snakeBody.size - 1)
            }
        }
    }

    private fun drawGame() {
        if (surfaceHolder.surface.isValid) {
            canvas = surfaceHolder.lockCanvas()
            canvas?.drawColor(Color.BLACK) // Fondo negro


            synchronized(snakeBody) {

                paint.color = Color.GREEN
                for (segment in snakeBody) {
                    canvas?.drawRect(
                        (segment.first * blockSize).toFloat(),
                        (segment.second * blockSize).toFloat(),
                        ((segment.first + 1) * blockSize).toFloat(),
                        ((segment.second + 1) * blockSize).toFloat(),
                        paint
                    )
                }
            }


            paint.color = Color.RED
            canvas?.drawRect(
                (foodPosition.first * blockSize).toFloat(),
                (foodPosition.second * blockSize).toFloat(),
                ((foodPosition.first + 1) * blockSize).toFloat(),
                ((foodPosition.second + 1) * blockSize).toFloat(),
                paint
            )

            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastTouchTime > minTouchInterval) {
                    changeDirection(it.x, it.y)
                    lastTouchTime = currentTime
                }
            }
        }
        return true
    }

    private fun changeDirection(x: Float, y: Float) {
        val screenWidth = width
        val screenHeight = height
        val topBoundary = screenHeight / 4 // arriba
        val bottomBoundary = 3 * screenHeight / 4 // abajo
        val leftBoundary = screenWidth / 2 //izquierda y derecha

        when {
            // Arriba
            y < topBoundary -> {
                snakeDirection = Direction.UP
            }
            // Abajo
            y > bottomBoundary -> {
                snakeDirection = Direction.DOWN
            }
            // Izquierda
            x < leftBoundary -> {
                snakeDirection = Direction.LEFT
            }
            //  Derecha
            x >= leftBoundary -> {
                snakeDirection = Direction.RIGHT
            }
        }
    }


}